package team.se.acommunity.dao;

import org.springframework.stereotype.Repository;

// 访问数据库的类要用下面这个注解,默认bean名是类名首字母小写，但是像下面这种写法可以自定义bean名
@Repository("alphaH")
public class AlphaDaoHibernateImpl implements AlphaDao {
    @Override
    public String select() {
        return "Hibernate";
    }
}
