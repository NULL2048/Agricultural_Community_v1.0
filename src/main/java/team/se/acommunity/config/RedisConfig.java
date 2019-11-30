package team.se.acommunity.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;


@Configuration
public class RedisConfig {
    // 使用bean标注，定义第三方bean，他会自动把自定义的bean装载入spring容器中
    // 覆写这个方法通过RedisTemplate这个类取得到数据库连接对象，通过RedisConnectionFactory这个类来获得，这个类已经被spring容器装配了
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        // 将数据库工厂给RedisTemplate，这个对象就有了访问redis数据库的能力
        template.setConnectionFactory(factory);

        // 设置key的序列化方式   将key设置成string类型构造器
        template.setKeySerializer(RedisSerializer.string());
        // 设置普通value的序列化方式    将value设置成json类型的构造器，因为json格式方便转化,方便接收各种类型数据
        template.setValueSerializer(RedisSerializer.json());
        // 设置hash的key的序列化方式
        template.setHashKeySerializer(RedisSerializer.string());
        // 设置hash的value的序列化方式
        template.setHashValueSerializer(RedisSerializer.json());

        // 设置完上面的配置之后要调用这个方法表示生效
        template.afterPropertiesSet();

        return template;
    }
}
