package team.se.acommunity.dao;

import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;
import team.se.acommunity.entity.User;

// 数据访问层的类都需要加上一个注解，装配到spring容器中，以前dao层都是用Repository这个注解，但是mybatis有自己的这个mapper注解，所以以后用这个就行
@Mapper
//@Repository
public interface UserMapper {
    // 想使用这些方法还需要写一个配置文件，配置文件中写了这些方法所需要的sql命令
    // mybatis会根据配置文件自动生成这个mapper接口的实现类，一般这个配置文件写在resource文件夹中的mapper文件夹中

    // 根据id查找用户
    User getById(int id);
    // 根据名字查找用户
    User getByName(String name);
    // 根据邮箱地址查找用户
    User getByEmail(String email);
    // 添加新用户
    int insertUser(User user);
    // 更新用户状态
    int updateStatus(int id, int status);
    // 修改头像
    int updateHeader(int id, String headerUrl);
    // 修改密码
    int updatePassword(int id, String password);
}
