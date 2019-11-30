package team.se.acommunity.service;

import com.github.benmanes.caffeine.cache.CacheLoader;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;
import team.se.acommunity.dao.DiscussPostMapper;
import team.se.acommunity.entity.DiscussPost;
import team.se.acommunity.util.SensitiveFilter;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class DiscussPostService {
    private static final Logger logger = LoggerFactory.getLogger(DiscussPostService.class);

    @Autowired
    private DiscussPostMapper discussPostMapper;

    @Autowired
    private SensitiveFilter sensitiveFilter;

    @Autowired
    private RedisTemplate redisTemplate;

    @Value("${caffeine.posts.max-size}")
    private int maxSize;

    @Value("${caffeine.posts.expire-seconds}")
    private int expireSeconds;

    // caffeine核心接口：Cache  一个Cache就代表一个缓存
    // Cache有两个子接口  一个是LoadingCache 一个是AsyncLoadingCache
    // 前者更常用，前者是同步缓存，就是说如果如果用这个缓存，发现缓存中没有数据，他会先让要请求数据的进程等待，等取到了数据再给他
    // 后者是异步缓存，它支持并发执行

    // 帖子列表的缓存
    private LoadingCache<String, List<DiscussPost>> postListCache;
    // 帖子总数缓存   因为帖子主页面下面会一直显示帖子的页数，他只显示前几页和最后几页，也属于一个经常需要访问的资源，所以最好也存入缓存中
    private LoadingCache<Integer, Integer> postRowsCache;

    // 上面的两个缓存需要在项目启动的时候初始化一次，所以写一个初始化方法
    @PostConstruct
    public void init() {
        // 初始化帖子列表缓存   Caffeine是通过newBuilder方法来进行实例化的
        postListCache = Caffeine.newBuilder()
                .maximumSize(maxSize) // 设置这个缓存所能存放的最大的数据个数
                .expireAfterWrite(expireSeconds, TimeUnit.SECONDS) // 设置这个缓存的数据多长时间后失效，并且指定单位
                .build(new CacheLoader<String, List<DiscussPost>>() {
                    @Nullable
                    @Override
                    public List<DiscussPost> load(@NonNull String key) throws Exception { // 如果从本地缓存中没有找到数据，就通过load方法用来实现先看一下redis有没有数据，如果还没有就再去mysql中查找数据
                        if (key == null || key.length() == 0) {
                            throw new IllegalArgumentException("参数不允许为空");
                        }

                        String[] params =  key.split(":");
                        if (params == null || params.length != 2) {
                            throw new IllegalArgumentException("参数错误");
                        }

                        int offset = Integer.valueOf(params[0]);
                        int limit = Integer.valueOf(params[1]);

                        // 二级缓存：Redis -> mysql   先看一下redis中有没有数据，如果有就直接返回数据，如果没有就再去查询mysql
                        // 如果redis中没有，则从mysql中取数据
                        if (redisTemplate.opsForList().range(key, 0, -1) == null || redisTemplate.opsForList().size(key) == 0) {
                            logger.debug("load post list from DB.");
                            List<DiscussPost> postList = discussPostMapper.listDiscussPosts(0, offset, limit, 1);

                            // 将数据插到redis中，然后再返回
                            redisTemplate.opsForList().rightPushAll(key, postList);
                            return postList;
                        } else {
                            logger.debug("load post list from Redis.");
                            // 如果能在二级缓存中找到，则直接将redis中的值返回
                            return redisTemplate.opsForList().range(key, 0, -1);
                        }
                        // 不用自己手动去讲数据查到本地缓存中，只要是调用了load这个方法，就是本地缓存中没有数据，caffine会自动将return的数据根据key查到本地缓存中的
                        // 这里需要注意以下，当项目停止运行，本地缓存就消失了，以后部署到服务器之后他会一直运行，所以就不会存在每一次重启项目本地缓存丢失的情况
                        // redis中的数据插进去就不会丢失了
                    }
                }); // 最后执行build方法去实例化缓存   这里需要传一个参数

        // 初始化帖子总数缓存
        postRowsCache = Caffeine.newBuilder()
                .maximumSize(maxSize)
                .expireAfterWrite(expireSeconds, TimeUnit.SECONDS)
                .build(new CacheLoader<Integer, Integer>() {
                    @Nullable
                    @Override
                    public Integer load(@NonNull Integer k) throws Exception {
                        if (k == null) {
                            throw new IllegalArgumentException("参数不允许为空");
                        }

                        // 这里一定要注意，redis中的key必须是String，所以要先把这个int型的转成String型的才可以
                        String key = String.valueOf(k);

                        // 先查一下二级缓存
                        if (redisTemplate.opsForValue().get(key) == null) {
                            logger.debug("load post rows from DB.");
                            int rows = discussPostMapper.getDiscussPostRows(k);

                            // 将数据插到redis中，然后再返回  key必须是String,但是value可以是各种类型
                            redisTemplate.opsForValue().set(key, rows);
                            return rows;
                        } else {
                            logger.debug("load post rows from Redis.");
                            // 如果能在二级缓存中找到，则直接将redis中的值返回   这里返回的数据是一个int数据，但是这个方法返回的时候没有将他向下转型，而是统一返回的Object类型，所以这里把他向下转型一下，变成Integer，int和Integer是可以相互转型的
                            return (Integer) redisTemplate.opsForValue().get(key);
                        }
                    }
                });
    }

    public List<DiscussPost> listDiscussPosts(int userId, int offset, int limit, int orderMode) {
        // 只有当传过来的userId是0的时候表示这时主页的请求，返回多条帖子的数据  orderMode为1的时候说明是最热页面的数据
        // 我们只对主页的最热页面做缓存
        if (userId == 0 && orderMode == 1) {
            return postListCache.get(offset + ":" + limit);
        }

        logger.debug("load post list from DB.");
        return discussPostMapper.listDiscussPosts(userId, offset, limit, orderMode);
    }

    public int getDiscussPostRows(int userId) {
        // 当传过来的userId为0的时候表示要求所有帖子的总数
        if (userId == 0) {
            return postRowsCache.get(userId);
        }

        logger.debug("load post rows from DB.");
        return discussPostMapper.getDiscussPostRows(userId);
    }

    public int insertDiscussPost(DiscussPost post) {
        if (post == null) {
            throw new IllegalArgumentException("参数不能为空");
        }
        // 转译HTML标记，将内容中可能是HTML标签内容的给转译一下，比如<input> ，就会自动转换成转义字符,让这些标签字符串原样输出，而不会被浏览器解析成HTML样式来影响原本的页面。这个方法是spring MVC自带的
        post.setTitle(HtmlUtils.htmlEscape(post.getTitle()));
        post.setContent(HtmlUtils.htmlEscape(post.getContent()));

        // 过滤敏感词
        post.setTitle(sensitiveFilter.filter(post.getTitle()));
        post.setContent(sensitiveFilter.filter(post.getContent()));

        return discussPostMapper.insertDiscussPost(post);
    }

    public DiscussPost getDiscussPostById(int id) {
        return discussPostMapper.getDiscussPostById(id);
    }

    public int updateCommentCount(int id, int commentCount) {
        return discussPostMapper.updateCommentCount(id, commentCount);
    }

    public int updateType(int id, int type) {
        return discussPostMapper.updateType(id, type);
    }

    public int updateStatus(int id, int type) {
        return discussPostMapper.updateStatus(id, type);
    }

    public int updateScore(int id, double score) {
        return discussPostMapper.updateScore(id, score);
    }
}
