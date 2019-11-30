package team.se.acommunity.aspect;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.SimpleFormatter;

@Component
@Aspect
public class ServiceLogAspect {
    private static final Logger logger = LoggerFactory.getLogger(ServiceLogAspect.class);

    @Pointcut("execution(* team.se.acommunity.service.*.*(..))")
    public void pointcut() {

    }

    @Before("pointcut()")
    public void before(JoinPoint joinPoint) {
        // 用户{1.2.3.4}，在{xxx}，访问了{team.se.acommunity.service.xxx()}
        // 从request对象中获取用户ip
        // 获取request对象
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        // 下面这一句是写完了消息队列消费者之后加的，因为类就是去监听所有的service方法，然后从中取得调用这个service的controller中的request对象那个，以前所有的service方法都是controller调用，所以不会出现问题，一定能取到request，但是消费者这个里面也调用了service方法，但是这个消费者方法中是没有request对象的，所以就要在这里判断一下，如果没有的话直接结束方法，避免出现空指针错误
        if (attributes == null) { // 表示这一次调用service不是通过controller调用的，是一次非常规调用
            return;
        }
        HttpServletRequest request = attributes.getRequest();
        // 获取ip
        String ip = request.getRemoteHost();
        // 获取请求时间，即当前时间
        String now = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        // 第一个是拼接的是连接点的类型名      第二个拼接的是连接点的类中的方法名
        String target = joinPoint.getSignature().getDeclaringType() + "." + joinPoint.getSignature().getName();
        // 将日志格式化输出
        logger.info(String.format("用户[%s],在[%s],访问了[%s].", ip, now, target));
    }
}
