package team.se.acommunity.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.io.File;

// 加了这个注解就会将这个bean作为配置类装配入spring容器中
@Configuration
public class WkConfig {
    private static final Logger logger = LoggerFactory.getLogger(WkConfig.class);

    @Value("${wk.image.storage}")
    private String wkImageStorage;

    // 下面这个注解是初始化注解，就是说spring在将bean装配入容器中时就会先调用初始化方法对这个bean进行初始化，所以加了这个注解，这个bean就会首先执行这个方法进行初始化并且只执行一次
    @PostConstruct
    public void init() {
        // 创建wk图片目录
        File file = new File(wkImageStorage);
        // 判断这个目录存不存在，如果不存在再自行创建这个目录
        if (!file.exists()) {
            file.mkdir();
            logger.info("创建wk图片目录：" + wkImageStorage);
        }
    }
}
