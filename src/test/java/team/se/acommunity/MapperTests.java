package team.se.acommunity;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import team.se.acommunity.dao.DiscussPostMapper;
import team.se.acommunity.dao.LoginTicketMapper;
import team.se.acommunity.dao.MessageMapper;
import team.se.acommunity.dao.UserMapper;
import team.se.acommunity.entity.DiscussPost;
import team.se.acommunity.entity.LoginTicket;
import team.se.acommunity.entity.Message;
import team.se.acommunity.entity.User;

import java.util.Date;
import java.util.List;

@RunWith(SpringRunner.class)
@SpringBootTest
@ContextConfiguration(classes = AgriculturalCommunityApplication.class)
public class MapperTests {
    @Autowired
    private UserMapper userMapper; // 测试一下分支

    @Autowired
    private DiscussPostMapper discussPostMapper;

    @Autowired
    private LoginTicketMapper loginTicketMapper;

    @Autowired
    private MessageMapper messageMapper;

    @Test
    public void testSelectUser() {
        User user = userMapper.getById(101);
        System.out.println(user);

        user = userMapper.getByName("liubei");
        System.out.println(user);

        user = userMapper.getByEmail("nowcoder101@sina.com");
        System.out.println(user);
    }

    @Test
    public void testInsertUser() {
        User user = new User();
        user.setUsername("test");
        user.setPassword("123456");
        user.setSalt("abc");
        user.setEmail("test@qq.com");
        user.setHeaderUrl("http://www.nowcoder.com/101.png"); // 这个图片是0-1000
        user.setCreateTime(new Date());

        int rows = userMapper.insertUser(user);
        System.out.println(rows);
        System.out.println(user.getId());
    }

    @Test
    public void updateUser() {
        int rows = userMapper.updateStatus(150, 1);
        System.out.println(rows);

        rows = userMapper.updateHeader(150, "http://www.nowcoder.com/102.png");
        System.out.println(rows);

        rows = userMapper.updatePassword(150, "hello");
        System.out.println(rows);
    }

    @Test
    public void testSelectPosts() {
        List<DiscussPost> list = discussPostMapper.listDiscussPosts(0, 0, 10, 0);
        for (DiscussPost post : list) {
            System.out.println(post);
        }

        int rows = discussPostMapper.getDiscussPostRows(0);
        System.out.println(rows);
    }

    @Test
    public void testInsertLoginTicket() {
        LoginTicket loginTicket = new LoginTicket();
        loginTicket.setUserId(101);
        loginTicket.setTicket("abc");
        loginTicket.setStatus(0);
        // System.currentTimeMillis()表示的是当前时间，后面是加了多少毫秒，表示从当前时间开始 1000*60*10毫秒之后到期，也就十分钟后到期
        loginTicket.setExpired(new Date(System.currentTimeMillis() + 1000 * 60 * 10));

        loginTicketMapper.insertLoginTicket(loginTicket);
    }

    @Test
    public void testSelectLoginTicket() {
        LoginTicket loginTicket = loginTicketMapper.getByTicket("abc");
        System.out.println(loginTicket);

        loginTicketMapper.updateStatus("abc", 1);
        loginTicket = loginTicketMapper.getByTicket("abc");
        System.out.println(loginTicket);
    }

    @Test
    public void testSelectLetters() {
        List<Message> temp = messageMapper.listConversations(111, 0, 20);
        for (Message t : temp) {
            System.out.println(t);
        }

        System.out.println(messageMapper.countConversation(111));

        temp = messageMapper.listLetters("111_112", 0, 20);
        for (Message t : temp) {
            System.out.println(t);
        }

        System.out.println(messageMapper.countLetter("111_112"));

        System.out.println(messageMapper.countUnreadLetter(131, "111_131"));

    }
}
