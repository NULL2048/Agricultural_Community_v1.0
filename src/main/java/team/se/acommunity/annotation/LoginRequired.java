package team.se.acommunity.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// 设置这个注解要写在什么位置，这里设置的是写在方法上
@Target(ElementType.METHOD)
// 设置这个注解的有效时长，这里设置的是这个注解只要是在程序运行的时候就有效
@Retention(RetentionPolicy.RUNTIME)
public @interface LoginRequired {
}
