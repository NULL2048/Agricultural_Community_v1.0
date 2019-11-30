package team.se.acommunity;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import team.se.acommunity.util.SensitiveFilter;

@RunWith(SpringRunner.class)
@SpringBootTest
@ContextConfiguration(classes = AgriculturalCommunityApplication.class)
public class SensitiveTests {
    @Autowired
    private SensitiveFilter sensitiveFilter;

    @Test
    public void testSensitiveFilter() {
        String text = "cyfabc";
        text = sensitiveFilter.filter(text);
        System.out.println(text);

        text = "这里可以※赌※博※，可以※嫖※娼※，可以※吸※毒※，可以※开※票※，哈哈哈！";
        text = sensitiveFilter.filter(text);
        System.out.println(text);
    }
}
