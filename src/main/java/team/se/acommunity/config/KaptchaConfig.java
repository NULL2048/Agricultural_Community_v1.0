package team.se.acommunity.config;

import com.google.code.kaptcha.Producer;
import com.google.code.kaptcha.impl.DefaultKaptcha;
import com.google.code.kaptcha.util.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Properties;

// 因为Kaptcha这个自动生成验证码的小工具spring没有内置对他进行管理，所以要自己写一个配置类注入到spring容器中
@Configuration
public class KaptchaConfig {
    @Bean
    public Producer kaptchaProducer() {
        // 创建配置内容对象
        Properties properties = new Properties();
        // 设置验证码图片宽度
        properties.setProperty("kaptcha.image.width", "100");
        // 设置验证码图片长度
        properties.setProperty("kaptcha.image.height", "40");
        // 设置验证码字体大小
        properties.setProperty("kaptcha.textproducer.font.size", "32");
        // 设置验证码字体颜色
        properties.setProperty("kaptcha.textproducer.font.color", "0,0,0");
        // 设置验证码由哪些字符组成
        properties.setProperty("kaptcha.textproducer.char.string", "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ");
        // 设置验证码有多少个字符
        properties.setProperty("kaptcha.textproducer.char.length", "4");
        // 设置验证码的噪声，用来干扰机器人识别验证码
        properties.setProperty("kaptcha.noise.impl", "com.google.code.kaptcha.impl.NoNoise");
        // 创建验证码生成对象
        DefaultKaptcha kaptcha = new DefaultKaptcha();
        // 使用配置内容对象生成配置对象
        Config config = new Config(properties);
        // 将配置对象添加进验证码生成对象中
        kaptcha.setConfig(config);
        // 将验证码对象注入到spring容器中
        return kaptcha;
    }
}
