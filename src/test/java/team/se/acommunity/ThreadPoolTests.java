package team.se.acommunity;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import team.se.acommunity.service.AlphaService;

import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@RunWith(SpringRunner.class)
@SpringBootTest
@ContextConfiguration(classes = AgriculturalCommunityApplication.class)
public class ThreadPoolTests {
    // 用来显示线程id和时间
    private static final Logger logger = LoggerFactory.getLogger(ThreadPoolTests.class);

    // JDK普通线程池
    // JDK种的线程池都是使用一个工厂类来创建的  下面这个5表示的是这个线程池创建了5个线程，所有的操作都是复用这5个线程
    private ExecutorService executorService = Executors.newFixedThreadPool(5);

    // JDK可执行定时任务的线程池
    private ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(5);

    // 注入spring普通线程池
    @Autowired
    private ThreadPoolTaskExecutor taskExecutor;

    // 注入spring可执行定时任务线程池
    @Autowired
    private ThreadPoolTaskScheduler taskScheduler;

    @Autowired
    private AlphaService alphaService;


    // 因为这里是test方法，所以和main方法启动有一些区别，在test方法中如果启动了一个线程之后，这个线程是和test线程并发执行的，但是如果test执行完了之后就会结束，连带着另一个线程也会结束，这一点就和main方法不同，main方法结束了， 其他线程也不会结束
    // 所以为了解决这个问题就是用一个sleep让test睡一会
    private void sleep(long m) {
        try {
            Thread.sleep(m);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    // 1，JDK普通线程池
    @Test
    public void testExecutorService() {
        // 首先创建一个线程体，线程体中存放了这个线程要执行的操作
        // Runnable这个类就是一个线程体类
        Runnable task = new Runnable() {
            @Override
            public void run() {
                logger.debug("hello ExecutorService");
            }
        };

        // executorService这个线程池中有5个已经创建好的线程供我们使用
        // 线程池的工作流程就是线程池中有创建好的线程，然后用submit方法传入一个线程体，从线程池中分配出一个线程来执行这个线程体。线程池中这5个线程就是被反复复用的，用来执行不同的任务
        for (int i = 0; i < 10; i++) {
            executorService.submit(task);
        }
        // 执行程序之后输出发现所使用的线程一致就是反复的使用线程池中的5个线程，通过日志中的线程号就能看出来
        sleep(10000);
    }


    // 2.JDK可执行定时任务的线程池
    @Test
    public void testScheduledExecutor() {
        Runnable task = new Runnable() {
            @Override
            public void run() {
                logger.debug("hello ScheduledExecutor");
            }
        };

        // 这个调用的是间隔某个时间进行周期执行，第一个参数是要执行的线程体，第二个参数是等待多长时间开始周期执行，就是说要等10000毫秒之后才开始周期执行线程，第三个参数是开始周期执行线程之后的执行周期是1000毫秒，每隔1000毫秒就执行一次，第四个参数是单位
        scheduledExecutorService.scheduleAtFixedRate(task, 10000, 1000, TimeUnit.MILLISECONDS);

        sleep(30000);
    }

    // 3.spring普通线程池
    @Test
    public void testThreadPoolTaskExecutor() {
        Runnable task = new Runnable() {
            @Override
            public void run() {
                logger.debug("hello ThreadPoolTaskExecutor");
            }
        };

        for (int i = 0; i < 10; i++) {
            taskExecutor.submit(task);
        }

        sleep(10000);

        // spring的线程池和jdk中的相比好处就是它可以设置动态扩容，还可以有等待队列，所以优先使用spring中的线程池
    }

    // 4.spring定时任务线程池
    @Test
    public void testThreadPoolTaskScheduler() {
        Runnable task = new Runnable() {
            @Override
            public void run() {
                logger.debug("hello ThreadPoolTaskScheduler");
            }
        };

        // spring提供的方法没有延迟执行参数，只有一个执行开始日期参数，所以这里创建一个在当前系统时间基础上延迟100000毫秒的日期得到的是相同的效果
        Date startTime = new Date(System.currentTimeMillis() + 10000);
        // 以1000毫秒为周期执行   这里默认单位就是毫秒
        taskScheduler.scheduleAtFixedRate(task, startTime, 1000);

        sleep(30000);
    }

    // 还有一种简便的调用方式，就是在普通的Java bean上加一个注解，spring就将这个bean作为一个线程体，可以将这个bean加到对应的线程池中去执行，就不用写runable了
    // 相关用法见alphaService
    // 5.spring普通线程池（简化）
    @Test
    public void testThreadPoolTaskExecutorSimple() {
        for (int i = 0; i < 10; i++) {
            alphaService.execute1();
        }

        sleep(10000);
    }

    // 6.spring定时任务线程池（简化）
    @Test
    public void testThreadPoolTaskSchedulerSimple() {
        // 这个不用写任何东西，因为定时任务都是被动被调的，只要是这个项目中有代码在跑，到了指定的时间之后他就会按照设置的规则自动执行
        sleep(30000);
    }
}
