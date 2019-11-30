package team.se.acommunity;

import org.checkerframework.checker.units.qual.A;
import org.junit.*;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import team.se.acommunity.entity.DiscussPost;
import team.se.acommunity.service.DiscussPostService;

import java.util.Date;

@RunWith(SpringRunner.class)
@SpringBootTest
@ContextConfiguration(classes = AgriculturalCommunityApplication.class)
public class SpringBootTests {
    @Autowired
    private DiscussPostService discussPostService;

    private DiscussPost data;

    // 因为这个是在测试类生成之前执行一次，所以这个方法必须得是static
    @BeforeClass
    public static void beforeClass() {
        System.out.println("beforeClass");
    }

    // 这个在类销毁之后执行一次，所以这个也得是static方法
    @AfterClass
    public static void afterClass() {
        System.out.println("afterClass");
    }

    // 下面这两个是在调用过程中执行，所以不用静态方法
    @Before
    public void before() {
        System.out.println("before");

        // 初始化测试数据
        data = new DiscussPost();
        data.setUserId(111);
        data.setTitle("test");
        data.setContent("test Content");
        data.setCreateTime(new Date());

        discussPostService.insertDiscussPost(data);
    }

    @After
    public void after() {
        System.out.println("after");

        // 将测试数据删除，防止项目中显示垃圾数据
        discussPostService.updateStatus(data.getId(), 2);
    }

    @Test
    public void test1() {
        System.out.println("test1");
    }

    @Test
    public void test2() {
        System.out.println("test2");
    }

    // 测试查询方法
    @Test
    public void testFindById() {
        DiscussPost post = discussPostService.getDiscussPostById(data.getId());

        // 使用断言来进行判断，得到的结果是不是正确的
        // 判断是否查询到数据
        Assert.assertNotNull(post);

        // 判断查询到的数据结果对不对，第一个参数是你期望的正确结果，第二个参数是查询结果
        Assert.assertEquals(data.getTitle(), post.getTitle());
        Assert.assertEquals(data.getContent(), post.getContent());
        // 上面这个也可以字节覆写DiscussPost类的equals方法，这样这里直接将对象传进去，它就会自动进行比较，判断查询结果正不正确

        // 上面的这些断言只要有不成立他就会抛出异常
    }

    @Test
    public void testUpdateScore() {
        int rows = discussPostService.updateScore(data.getId(), 2000.00);
        // 判断有没有修改成功
        Assert.assertEquals(1, rows);

        DiscussPost post = discussPostService.getDiscussPostById(data.getId());
        // 判断修改的结果对不对，因为计算机底层都是2进制，实际上是使用浮点计数法来存储小数的，所以这里要指定传入小数的精度，保留两个小数点
        Assert.assertEquals(2000.00, post.getScore(), 2);
    }
}
