package team.se.acommunity;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import team.se.acommunity.util.MailClient;


@RunWith(SpringRunner.class)
@SpringBootTest
@ContextConfiguration(classes = AgriculturalCommunityApplication.class)
public class MailTests {
    @Autowired
    private MailClient mailClient;

    // 这个模板引擎是用来将HTML解析存储到对象中的
    @Autowired
    private TemplateEngine templateEngine;

    @Test
    public void testTextMail() {
        mailClient.sendMail("973071263@qq.com", "TEST", "Welcome");
    }

    @Test
    public void testHtmlMail() {
        // 用来存储要传输的数据
        Context context = new Context();
        context.setVariable("username", "sunday");

        // 对于网页的路径，最前面的那个/就表示的是templates目录,后面就写正常的目录路径就可以了，而且这里写HTML文件的时候不用写后面的.html后缀，直接写文件名就可以了
        // 将HTML模板也放入这个content，准备将HTML模板当成邮件发送过去
        String content = templateEngine.process("/mail/demo", context);
        System.out.println(content);

        mailClient.sendMail("973071263@qq.com", "HTML", content);
    }
}
