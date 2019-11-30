package team.se.acommunity;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

import javax.annotation.PostConstruct;

// 通常是程序的入口用这个注解，这个类里面就有main方法，所以是程序入口
@SpringBootApplication
public class AgriculturalCommunityApplication {
	// 这个注解是用来管理bean的生命周期的，它用来管理bean的初始化，由这个注解标识的方法，会在构造器调用完以后被执行，所以一般用他来标识初始化方法
	@PostConstruct
	public void init() {
		// 解决netty启动冲突问题
		// Netty4Utils.setAvailableProcessors()
		System.setProperty("es.set.netty.runtime.available.processors", "false");
	}

	public static void main(String[] args) {
		// 这个方法是启动spring，其中就包括启动tomcat，而且自动创建了spring容器,并且将bean装配到spring容器中，他会把所有添加了注解的bean放入spring容器中
		SpringApplication.run(AgriculturalCommunityApplication.class, args);
	}

}
