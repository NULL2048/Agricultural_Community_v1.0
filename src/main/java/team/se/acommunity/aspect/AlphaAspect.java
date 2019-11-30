package team.se.acommunity.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.stereotype.Component;

import java.util.jar.JarOutputStream;

// 将这个方面组将设置成一个bean，这个不属于某一层，所以直接用普通的bean注解就可以
// @Component
// 声明这是一个aspect组件
// 使用它的好处就是我的后台代码不需要改动，直接添加这个切面组件，就可以实现对源代码看功能的拓冲和修改，这个原理就是动态代理
// @Aspect
public class AlphaAspect {
    // 声明一下切点pointcut方法，来设置那些bean,哪些方法是我们处理的目标,里面的含义就是第一个星表示的是要处理所有处理方法的返回值返回值，第二个星表示要处理这个包team.se.acommunity.service下面的所有类，第三个星表示处理这个类里面的所有方法，括号里面的..表示处理方法的所有参数
    // 也可以明确下来，不用*, 而是特指处理那些类，哪些方法，哪些参数，通过这些东西就能获得连接点pointcut
    @Pointcut("execution(* team.se.acommunity.service.*.*(..))")
    public void pointcut() {

    }
    // 这个表示在连接点之前处理内容，上面就是获取的连接点,括号里面写的就是切点
    @Before("pointcut()")
    public void before() {
        System.out.println("before");
    }
    // 在连接点之后织入代码
    @After("pointcut()")
    public void after() {
        System.out.println("after");
    }
    // 在有了返回值以后再处理
    @AfterReturning("pointcut()")
    public void afterReturning() {
        System.out.println("afterReturning");
    }
    // 在代码抛出异常的时候织入代码
    @AfterThrowing("pointcut()")
    public void afterThrowing() {
        System.out.println("afterThrowing");
    }
    // 既想在连接点前面织入逻辑，又想在连接点后面织入逻辑.这个方法需要参数，ProceedingJoinPoint表示的是连接点,指代程序织入的部位
    @Around("pointcut()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        // 在连接点之前输出
        System.out.println("around before");
        // 这个就是表示调用连接点位置的方法逻辑，这里调用的是被代理对象的原方法
        Object obj = joinPoint.proceed();
        // 在连接点之后输出
        System.out.println("around after");
        return obj;
    }
}
