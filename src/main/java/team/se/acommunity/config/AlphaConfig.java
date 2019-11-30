package team.se.acommunity.config;

import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.text.SimpleDateFormat;

/**
 * 通过配置类，通过bean注解来装配第三方的bean
 * 因为第三方的bean好多都是jar包，是别人写的东西，自己是不能在人家的源码上加注解的
 */
// 下面这个注解用来标识配置类
@Configuration
public class AlphaConfig {
    // 下面就是bean注解，注意方法名就是这个bean的名字，以后就通过这个方法的名字可以来取得返回的这个类
    // 这个表示的含义就是这个方法返回的对象就会被放到spring容器里,之所以用配置类的方法装配第三方类，是因为要想装配到spring容器，必须在这个类上面有注解，而第三方包不能在人家源码上做修改，所以只能用这个方法
    @Bean
    public SimpleDateFormat simpleDateFormat() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    }
}
