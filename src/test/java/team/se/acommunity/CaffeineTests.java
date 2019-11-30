package team.se.acommunity;

import org.junit.Test;
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
public class CaffeineTests {
    @Autowired
    private DiscussPostService postService;

    @Test
    public void initDataForTest() {
        for (int i = 0; i < 300000; i++) {
            DiscussPost post = new DiscussPost();
            post.setUserId(111);
            post.setTitle("缓存测试");
            post.setContent("test");
            post.setCreateTime(new Date());
            post.setScore(Math.random() * 2000);
            postService.insertDiscussPost(post);
        }
    }

    @Test
    public void testCache() {
        System.out.println(postService.listDiscussPosts(0, 0, 10, 1));
        System.out.println(postService.listDiscussPosts(0, 0, 10, 1));
        System.out.println(postService.listDiscussPosts(0, 0, 10, 1));
        System.out.println(postService.listDiscussPosts(0, 0, 10, 0));

    }
}
