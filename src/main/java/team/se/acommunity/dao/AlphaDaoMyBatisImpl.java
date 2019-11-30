package team.se.acommunity.dao;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

// 加了这个注解，这个bean就能被优先装配
@Primary
@Repository
public class AlphaDaoMyBatisImpl implements AlphaDao{
    public String select() {
        return "MyBatis";
    }
}
