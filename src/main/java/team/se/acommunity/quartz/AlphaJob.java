package team.se.acommunity.quartz;


import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

// 使用quartz就要实现job接口  创建一个任务   Job
public class AlphaJob implements Job {
    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        // 就打印一下线程名字，当前线程执行一个quartz任务
        System.out.println(Thread.currentThread().getName() + ": execute a quartz job.");
    }
}
