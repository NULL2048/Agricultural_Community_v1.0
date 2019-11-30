package team.se.acommunity.quartz;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundSetOperations;
import org.springframework.data.redis.core.RedisTemplate;
import team.se.acommunity.entity.DiscussPost;
import team.se.acommunity.service.DiscussPostService;
import team.se.acommunity.service.ElasticsearchService;
import team.se.acommunity.service.LikeService;
import team.se.acommunity.util.CommunityConstant;
import team.se.acommunity.util.RedisKeyUtil;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class PostScoreRefreshJob implements Job, CommunityConstant {
    private static final Logger logger = LoggerFactory.getLogger(PostScoreRefreshJob.class);

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private DiscussPostService discussPostService;

    // 要将更新后的评分结果同步到搜索引擎中
    @Autowired
    private ElasticsearchService elasticsearchService;

    @Autowired
    private LikeService likeService;

    // 项目创建时间，用来计算分数
    private static final Date epoch;

    // 因为这个时间只需要在加载这个类的时候初始化一次，并且只需要执行一次，所以将他的初始化过程写在静态代码块中，因为里面用到的方法需要处理异常，写在静态代码块中比较方便
    static {
        try {
            // SimpleDateFormat这个类既可以将字符串按照指定的格式解析成Date对象，也可以将Date对象解析成字符串
            epoch = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse("2017-09-01 00:00:00");
        } catch (ParseException e) {
            throw new RuntimeException("初始化惠农网纪元失败", e);
        }
    }

    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        String redisKey = RedisKeyUtil.getPostScoreKey();
        // 因为这个key中有多个数据，我们需要对他里面的值进行多次操作，直接将key绑定在bound上进行操作，比较方便
        BoundSetOperations operations = redisTemplate.boundSetOps(redisKey);

        // 如果没有帖子有点赞，评论就不执行任务了
        if (operations.size() == 0) {
            logger.info("[任务取消] 没有需要刷新的帖子");
            return;
        }

        logger.info("[任务开始] 正在刷新帖子分数：" + operations.size());
        while (operations.size() > 0) {
            // 每次弹出一个帖子id
            this.refresh((Integer) operations.pop());
        }
        logger.info("[任务结束] 帖子分数刷新完毕！：");

    }

    public void refresh(int postId) {
        DiscussPost post = discussPostService.getDiscussPostById(postId);

        if (post == null) {
            logger.error("该帖子不存在：id=" + postId);
            return;
        }

        if (post.getStatus() == 2) {
            logger.error("该帖子已删除：id=" + postId);
            return;
        }

        // 是否精华  要熟练这种很简便的代码书写方法
        boolean wonderful = post.getStatus() == 1;
        // 评论数量
        int commentCount = post.getCommentCount();
        // 点赞数量
        long likeCount = likeService.countEntityLike(ENTITY_TYPE_POST, postId);

        // 计算权重  习惯性将点赞，评论，加精分数叫做权重   习惯用三目运算写法
        double w = (wonderful ? 75 : 0) + commentCount * 10 + likeCount * 2;
        // 分数 = 帖子权重 + 距离天数
        // 以10为底的Log 因为log函数的横坐标小于1就会使纵坐标为负数，分数最好不要出现符输，所以这里使用一个max函数来计算如果小于1就将其记为1来计算
        double score = Math.log10(Math.max(w, 1))
                + (post.getCreateTime().getTime() - epoch.getTime()) / (1000 * 3600 * 24);
        // getTime是将Date日期格式转换成毫秒类型，这样方便进行日期的计算，再将毫秒转换成天数，通过除以(1000 * 3600 * 24)

        // 更新帖子分数
        discussPostService.updateScore(postId, score);

        // 同步搜索数据
        post.setScore(score); // 更新一下post对象的score
        elasticsearchService.saveDiscussPost(post);
    }
}
