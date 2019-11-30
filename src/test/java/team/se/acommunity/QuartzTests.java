package team.se.acommunity;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
@ContextConfiguration(classes = AgriculturalCommunityApplication.class)
public class QuartzTests {
    // 要想删除任务，就要使用任务调度器
    @Autowired
    private Scheduler scheduler;

    @Test
    public void testDeleteJob() {
        try {
            // 调用调度器中的删除方法，传入一个jobKey对象，jobKey对象中存的是job的名字和分组的名字
            // 但会一个布尔值，是否成功删除
            // 删除原理之就从数据库中将任务的相关配置数据删掉，也可以自己手动去数据库中删除
            boolean result = scheduler.deleteJob(new JobKey("alphaJob", "alphaJobGroup"));
        } catch (SchedulerException e) {
            e.printStackTrace();
        }
    }
}
