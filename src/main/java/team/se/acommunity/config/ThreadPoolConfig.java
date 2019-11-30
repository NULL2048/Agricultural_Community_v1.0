package team.se.acommunity.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
// 这里要写上EnableScheduling这注解才能启用spring中的线程池对象，否他它默认是不开启的
@EnableScheduling
// 这个注解是启用Async注释，可以使java bean设置为一个线程体
@EnableAsync
public class ThreadPoolConfig {
}
