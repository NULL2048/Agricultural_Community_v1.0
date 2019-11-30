package team.se.acommunity.dao;

import org.apache.ibatis.annotations.*;
import team.se.acommunity.entity.LoginTicket;

@Mapper
// 这个注解的意思是这个接口不推荐使用了，因为ticket已经存到了redis当中了
@Deprecated
public interface LoginTicketMapper {
    // 以前去实现mapper借口就是再写一个mapper实现文件XML，其实还可以直接在这个接口里面通过注解取实现这个mapper接口
    // 这些个标签里面都可以写很多字符串，他会自己给你拼接，你可以选择把一整段sql命令写成一个字符串，也可以选择拆分成多个让他自动拼接。但是要记住，分割出来的字符串之间要用空格分隔，否则拼接起来会连在一起，容易出错
    @Insert({"insert into login_ticket(user_id, ticket, status, expired) ",
             "values(#{userId}, #{ticket}, #{status}, #{expired})"
    })             // #{}是从对象loginTicket中取出对应属性值
    @Options(useGeneratedKeys = true, keyProperty = "id") // 因为id属性是自动生成的，所以这里要启用自动生成组件，将数据库自动生成的id取出来，放到loginTicket对象中的id属性中去
    int insertLoginTicket(LoginTicket loginTicket);

    @Select({"select id, user_id, ticket, status, expired ",
             "from login_ticket where ticket=#{ticket}"
    })
    LoginTicket getByTicket(String ticket);

    // 这些sql注解和XML文件一样也支持动态sql
    @Update({ // 想要在这里面拼接if标签，必须在最外面用<script>标签括起来
            "<script>",
            "update login_ticket set status=#{status} where ticket=#{ticket} ",
            "<if test=\"ticket!=null\"> ",
            "and 1 = 1 ",
            "</if>",
            "</script>"
    })
    int updateStatus(String ticket, int status);
}
