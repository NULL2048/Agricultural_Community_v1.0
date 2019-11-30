package team.se.acommunity.config;

import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.SimpleTrigger;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.quartz.JobDetailFactoryBean;
import org.springframework.scheduling.quartz.SimpleTriggerFactoryBean;
import team.se.acommunity.quartz.AlphaJob;
import team.se.acommunity.quartz.PostScoreRefreshJob;

// 配置Quartz第一次启用是通过java代码去配置他，然后它就会将配置信息存放到数据库中，以后再读取配置信息就直接去数据库中读取了
// 配置 -> 数据库 -> 调用
@Configuration
public class QuartzConfig {
    // FactoryBean可简化Bean的实例化过程
    // 1.通过FactoryBean封装bean的实例化过程
    // 2.将FactoryBean装配到Spring容器中
    // 3.将FactoryBean注入给其他的bean
    // 4.该bean得到的是FactoryBean所管理的对象实例


    // 任务详情
    // 配置JobDetail   JobDetail是job的配置类
    // @Bean 将bean注释掉，不想让项目启动的时候执行这个实例Job了
    public JobDetailFactoryBean alphaJobDetail() {
        JobDetailFactoryBean factoryBean = new JobDetailFactoryBean();
        // 设置JobDetailFactoryBean管理的对象类型
        factoryBean.setJobClass(AlphaJob.class);
        // 设置job的名字，给任务起一个名字，但是千万不能和其他任务重名，否则会出错
        factoryBean.setName("alphaJob");
        // 给任务分配一个组，多个任务可以同属于一组
        factoryBean.setGroup("alphaJobGroup");
        // 设置这个任务是持久保存吗，true就是持久保存   就是说以后这个任务不再执行了，这个任务的触发器都没有了，但是这个任务还会保存着
        factoryBean.setDurability(true);
        // 当前任务是不是可恢复的
        factoryBean.setRequestsRecovery(true);

        return factoryBean;
    }

    // 触发器
    // 配置Trigger  这个类是用来配置任务的执行方案   配置Trigger可以使用两个方法，一个是SimpleTriggerFactoryBean 一个是CronTriggerFactoryBean
    // 前者是做简单的quartz配置时用的，比如说每隔3天执行一个任务
    // 后者是做复杂的quartz配置时用的，比如果在每个月最后天半夜两点执行一个任务
    // 配置Trigger需要依赖JobDetail，这里传入的是alphaJobDetail这个JobDetailFactoryBean类型的bean，但是如同上面讲解factoryBean一样，将factoryBean传入这个trigger配置方法中得到的不是factoryBean，得到的是factoryBean管理的对象，在这里也就是JobDetail实例化对象
    // @Bean
    public SimpleTriggerFactoryBean alphaTrigger(JobDetail alphaJobDetail) {
        // 用相同的方式配置trigger
        SimpleTriggerFactoryBean factoryBean = new SimpleTriggerFactoryBean();
        // 设置trigger的jobDetail  设置这个trigger是对哪个job进行配置
        factoryBean.setJobDetail(alphaJobDetail);

        factoryBean.setName("alphaTrigger");
        factoryBean.setGroup("alphaTriggerGroup");
        // 设置执行间隔，每3000毫秒执行一次
        factoryBean.setRepeatInterval(3000);
        // 设置用来存储任务状态的类  使用内置的JobDataMap类型来存储job状态
        factoryBean.setJobDataMap(new JobDataMap());

        return factoryBean;
    }

    // 刷新帖子分数的任务
    @Bean
    public JobDetailFactoryBean postScoreRefreshJobDetail() {
        JobDetailFactoryBean factoryBean = new JobDetailFactoryBean();
        factoryBean.setJobClass(PostScoreRefreshJob.class);
        factoryBean.setName("postScoreRefreshJob");
        factoryBean.setGroup("communityJobGroup");
        factoryBean.setDurability(true);
        factoryBean.setRequestsRecovery(true);

        return factoryBean;
    }

    // 刷新帖子分数的任务触发器
    @Bean
    public SimpleTriggerFactoryBean postScoreRefreshTrigger(JobDetail postScoreRefreshJobDetail) {
        SimpleTriggerFactoryBean factoryBean = new SimpleTriggerFactoryBean();
        factoryBean.setJobDetail(postScoreRefreshJobDetail);

        factoryBean.setName("postScoreRefreshTrigger");
        factoryBean.setGroup("communityTriggerGroup");
        // 设置执行间隔，每五分钟执行一次，实际上不应该这么少，但是为了方便测试，就设置这么少了
        factoryBean.setRepeatInterval(1000 * 60 * 5);
        // 设置用来存储任务状态的类  使用内置的JobDataMap类型来存储job状态
        factoryBean.setJobDataMap(new JobDataMap());

        return factoryBean;
    }
}
