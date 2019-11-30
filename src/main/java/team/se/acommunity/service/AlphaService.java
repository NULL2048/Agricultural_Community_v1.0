package team.se.acommunity.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import team.se.acommunity.dao.AlphaDao;
import team.se.acommunity.dao.DiscussPostMapper;
import team.se.acommunity.dao.UserMapper;
import team.se.acommunity.entity.DiscussPost;
import team.se.acommunity.entity.User;
import team.se.acommunity.util.CommunityUtil;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.Date;

// 下面是业务层注解，业务层的类都要用这个注解
@Service
// 下面这个注解表示这个bean作用范围，标注整个spring容器中只能用一个这个bean,还是可以有多个这个bean
// 默认参数是singleton,单例
@Scope("prototype") // 这个属性表示这个bean可以在spring容器中有多个，每次访问这个bean，都会生成一个bean对象，也不会调用完自己就销毁了
public class AlphaService {
    private static final Logger logger = LoggerFactory.getLogger(AlphaService.class);
    // 将dao层的bean依赖注入给service
    @Autowired
    private AlphaDao alphaDao;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private DiscussPostMapper discussPostMapper;
    @Autowired
    private TransactionTemplate transactionTemplate;

    public AlphaService() {
        System.out.println("实例化");
    }
    // 下面这个注解表示这个init方法在构造器（构造方法）之后调用，一般是用来初始化属性的
    @PostConstruct
    public void init() {
        System.out.println("初始化");
    }
    // 下面这个注解表示在销毁对象之前去调用它
    @PreDestroy
    public void destroy() {
        System.out.println("销毁");
    }

    public String find() {
        return alphaDao.select();
    }

    /**
     * 讲解一下这个事务传播机制的含义。
     * 比如A方法和B方法，这两个方法都加了事务管理的注解，而且A方法执行过程当中调用了B方法，那么这个B方法到底是使用自己的隔离级别，还是使用A的隔离级别呢，这个事务传递就为了解决这个问题
     * A调用了B，相对于B来说，这个A就是外部事物，就是说对于被调用者来说，调用者就是外部事物
     */
    // 下面是对一些常用的事务的传播机制,下面的这些注解都是对被调用的事务加的
    // REQUIRED: 支持当前事务(外部事务),如果不存在则创建新事务.就是说A调B，如果A有自己的事务，那么B就按照A的来，如果A没有，那么B就自己创建自己的事务
    // REQUIRES_NEW: 创建一个新事务,并且暂停当前事务(外部事务).A调B，B无视A的事务，B永远使用自己的事务
    // NESTED: 如果当前存在事务(外部事务),则嵌套在该事务中执行(独立的提交和回滚),否则就会REQUIRED一样.就是说A调B，A也有事务，B也有事务，但是B自己还是按照自己的事务来，B有自己的提交和回滚，如果外部事物不存在就和REQUIRED一样了
    // 这里如果不使用事务管理，那么每一次执行mapper都会将数据提交，如果在这个方法过程中出现了报错，他是不会进行回滚事务的，这一个方法就相当于一个事务
    // 下面第一个参数是常见的并发异常使用的各种隔离级别，第二个参数就是事务的传播机制
    @Transactional(isolation = Isolation.READ_COMMITTED, propagation = Propagation.REQUIRED)
    public Object save1() {
        // 下面写的是第一类更新丢失
        // 新增用户
        User user = new User();
        user.setUsername("alpha");
        user.setSalt(CommunityUtil.generateUUID().substring(0, 5));
        user.setPassword(CommunityUtil.md5("123" + user.getSalt()));
        user.setEmail("123@qq.com");
        user.setHeaderUrl("http://image.newcoder.com/head/99t.png");
        user.setCreateTime(new Date());
        userMapper.insertUser(user);

        // 新增帖子
        DiscussPost post = new DiscussPost();
        post.setUserId(user.getId());
        post.setTitle("hello");
        post.setContent("haha");
        post.setCreateTime(new Date());
        discussPostMapper.insertDiscussPost(post);

        Integer.valueOf("abc");

        return "ok";
    }
    // 使用编程的方法进行事务管理，这里使用spring容器中自带的bean--TransactionTemplate
    public Object save2() {
        // 设置隔离等级
        transactionTemplate.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
        // 设置传播机制
        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);

        // 这里调用的是一个回调函数，要调的方法要自己编写，就写在了这个括号里面，在需要的时候系统在底层会自己调用
        return transactionTemplate.execute(new TransactionCallback<Object>() {
            @Override
            public Object doInTransaction(TransactionStatus transactionStatus) {
                // 下面写的是第一类更新丢失
                // 新增用户
                User user = new User();
                user.setUsername("alpha");
                user.setSalt(CommunityUtil.generateUUID().substring(0, 5));
                user.setPassword(CommunityUtil.md5("123" + user.getSalt()));
                user.setEmail("123@qq.com");
                user.setHeaderUrl("http://image.newcoder.com/head/99t.png");
                user.setCreateTime(new Date());
                userMapper.insertUser(user);

                // 新增帖子
                DiscussPost post = new DiscussPost();
                post.setUserId(user.getId());
                post.setTitle("hello");
                post.setContent("haha");
                post.setCreateTime(new Date());
                discussPostMapper.insertDiscussPost(post);

                Integer.valueOf("abc");

                return "ok";
            }
        });
    }

    // 让该方法在多线程环境下，被异步调用
    @Async
    public void execute1() {
        logger.debug("execute1");
    }

    // 注解定期执行任务的线程体
    // 第一个设置线程多久开始执行，第二个设置执行周期  单位都是毫秒
    // @Scheduled(initialDelay = 10000, fixedRate = 1000)
    public void execute2() {
        logger.debug("execute2");
    }
}
