package team.se.acommunity.actuator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.stereotype.Component;
import team.se.acommunity.util.CommunityUtil;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

// 自定义端点  查看当前数据库连接是否正常
@Component
// 指定这个端点的访问id，到时候就通过/actuator/database来访问
@Endpoint(id = "database")
public class DatabaseEndpoint {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseEndpoint.class);

    // 获取连接池就要用这个类，DataSource是连接池的顶层接口，他也是默认用spring容器管理的，所以直接注入
    @Autowired
    private DataSource dataSource;

    // 这个注解表示使用get请求访问
    @ReadOperation
    public String checkConnection() {
        try (
                // 在括号中获得的对象它会自动在下面加一个finally然后将其关掉，就不用自己写了
                Connection conn = dataSource.getConnection();
        ) {
            return CommunityUtil.getJSONString(0, "获取连接成功!");
        } catch (SQLException e) {
            logger.error("获取连接失败:" + e.getMessage());
            return CommunityUtil.getJSONString(1, "获取连接失败!");
        }
    }
}
