package team.se.acommunity.controller;

import com.google.code.kaptcha.Producer;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.thymeleaf.context.Context;
import team.se.acommunity.entity.User;
import team.se.acommunity.service.MessageService;
import team.se.acommunity.service.UserService;
import team.se.acommunity.util.CommunityConstant;
import team.se.acommunity.util.CommunityUtil;
import team.se.acommunity.util.RedisKeyUtil;

import javax.imageio.ImageIO;
import javax.jws.WebParam;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Controller
public class LoginController implements CommunityConstant {
    private static final Logger logger = LoggerFactory.getLogger(LoginController.class);

    @Autowired
    private MessageService messageService;

    @Autowired
    private UserService userService;

    @Autowired
    private Producer kaptchaProducer;

    @Autowired
    private RedisTemplate redisTemplate;
    // 将application.priperties中的server.servlet.context-path键值对注入，注意这个配置文件中的键值对全都被装配到了spring容器中了
    @Value("server.servlet.context-path")
    private String contextPath;

    @RequestMapping(path = "/register", method = RequestMethod.GET)
    public String getRegisterPage() {
        // 这个就是要返回这个controller操作要取得哪一个html页面，下面就写这个页面的路径就行，也是绝对路径，/表示templates文件夹，不用写.html后缀
        return "/site/register";
    }

    @RequestMapping(path = "/login", method = RequestMethod.GET)
    public String getLoginPage() {
        // 下面就是返回一个字符串给浏览器，浏览器就会根据这个字符串的路径去访问这个页面
        return "/site/login";
    }

    @RequestMapping(path = "/register", method = RequestMethod.POST)
    public String register(Model model, User user) {
        // user对象被自动放进了model中
        Map<String, Object> map = userService.register(user);
        if (map == null || map.isEmpty()) {
            model.addAttribute("msg", "恭喜您，注册成功，我们已经向您的邮箱发送了一封激活邮件，请尽快激活");
            model.addAttribute("target", "/index");
            // 将model传给下面这个return的界面，然后跳转到这个界面
            return "/site/operate-result";
        } else { // 注册失败
            // 取到service传过来的数据，存入到model中，用来将这些数据传送给/site/register这个界面
            model.addAttribute("usernameMsg", map.get("usernameMsg"));
            model.addAttribute("passwordMsg", map.get("passwordMsg"));
            model.addAttribute("emailMsg", map.get("emailMsg"));

            // 注册失败就跳转回/site/register这个界面
            return "/site/register";
        }
    }

    // http://localhost:8080/ac/activation/101/code
    // 下面这个路径就是按照这个格式的任意地址访问都会进入到这个方法，然后这个方法再去判断传进来的激活码对不对
    @RequestMapping(path = "/activation/{userId}/{code}", method = RequestMethod.GET)
                                          // 下面userId,code参数是从上面path中{}取出来的
    public String activation(Model model, @PathVariable("userId") int userId, @PathVariable("code") String code) {
        int result = userService.activation(userId, code);
        if (result == ACTIVATION_SUCCESS) {
            model.addAttribute("msg", "恭喜您，您的账号已经激活成功！");
            model.addAttribute("target", "/login");
        } else if (result == ACTIVATION_REPEAT) {
            model.addAttribute("msg", "无效操作，该账号已经激活过了");
            model.addAttribute("target", "/index");
        } else {
            model.addAttribute("msg", "激活失败，您的激活链接已失效");
            model.addAttribute("target", "/index");
        }
        return "/site/operate-result";
    }

    /**
     * 用来生成验证码图片
     * @param resp 这个要自己通过response将这个图片发送给浏览器
     * 每次访问这个方法的路径，这个网址显示的就是一个验证码图片，所以在页面段图片标签的src就是写/kaptcha这个路径获得图片
     */
    @RequestMapping(path = "/kaptcha", method = RequestMethod.GET)
    public void getKaptcha(HttpServletResponse resp/*, HttpSession session*/) {
        // 生成验证码
        // 生成四位随机字符
        String text = kaptchaProducer.createText();
        // 按照这个字符生成对应的验证码图片
        BufferedImage image = kaptchaProducer.createImage(text);

//        // 将验证码存入session
//        session.setAttribute("kaptcha", text);

        // 验证码的归属
        String kaptchaOwner = CommunityUtil.generateUUID();
        // 将临时凭证当道cookie中传给浏览器，让他在本地创建这个凭证
        Cookie cookie = new Cookie("kaptchaOwner", kaptchaOwner);
        // 设置临时凭着有效时间未60秒
        cookie.setMaxAge(60);
        // 设置cookie的有效路径 设置为整个项目下都有效
        cookie.setPath(contextPath);
        // 添加到response
        resp.addCookie(cookie);

        // 将验证码存入redis
        String redisKey = RedisKeyUtil.getKaptchaKay(kaptchaOwner);
        // 将验证码存入redis同时设置其在redis数据库中的有效时间为60秒
        redisTemplate.opsForValue().set(redisKey, text, 60, TimeUnit.SECONDS);

        // 将图片输出给浏览器
        // 设置传送数据类型
        resp.setContentType("image/png");
        try {
            // 获得resp的输出流，用来将上面获取的图片image传送到resp中的输出流中，这样resp对象就会跟着
            // 将图片写入输出流
            OutputStream os = resp.getOutputStream();
            ImageIO.write(image, "png", os);
        } catch (IOException e) {
            logger.error("响应验证码失败：" + e.getMessage());
        }
    }
    // 两个方法的访问路径可以是一样的，但是后面的请求方式必须不同，如果请求方式也相同那就是冲突了
    @RequestMapping(path = "/login", method = RequestMethod.POST)
    // 有一个规则，如果在这个controller里面的方法中参数不是一个基本数据类型或者String，而是一个对象，spring MVC会自动把这些对象传给model这个模板对象中来一并传送给下面返回给的页面
    // 但是像这个方法，里面都是基本数据类型或者String，那么他是不会自动将这些参数添加到model中的，所以想要在页面中取得这些数据只有两种办法
    // 一种是通过手动将数据添加到model中，一种是直接从respone中取得这些数据，因为这些数据给是一开始浏览器端发送请求过来的，服务器还需要把这些内容通过response响应回去，这些参数都还在response对象中，response对象的生命周期是一次请求，所以那个时候这个response对象还没有被销毁呢
    public String login(String username, String password, String code, boolean rememberme,
                        Model model, /*HttpSession session,*/ HttpServletResponse resp,
                        @CookieValue("kaptchaOwner") String kaptchaOwner) { // 用户凭证都存在了cookie中，这个注解就是取得浏览器端的cookie
        // 检验验证码
        // 获取用户输入进来的验证码文本
        //String kaptcha = (String) session.getAttribute("kaptcha");      // 这个也是比较字符串是否相同的方法，但是这个方法是忽略大小写的，验证码都是忽略大小写的

        String kaptcha = null;
        // 判断凭证是否失效
        if (StringUtils.isNotBlank(kaptchaOwner)) {
            String redisKey = RedisKeyUtil.getKaptchaKay(kaptchaOwner);
            // 取得当前用户应该输入的验证码答案
            kaptcha = (String) redisTemplate.opsForValue().get(redisKey);
        }

        if (StringUtils.isBlank(kaptcha) || StringUtils.isBlank(code) || !kaptcha.equalsIgnoreCase(code)) {
            model.addAttribute("codeMsg", "验证码不正确");
            return "/site/login";
        }

        // 验证账号，密码
        int expiredSeconds = rememberme ? REMEMBER_EXPIRED_SECONDS : DEFAULT_EXPIRED_SECONDS;
        // 执行业务层登录操作
        Map<String, Object> map = userService.login(username, password, expiredSeconds);
        if (map.containsKey("ticket")) { // 如果能找到传过来的ticket，说明登陆成功
            // 将登录凭证存储到cookie中
            Cookie cookie = new Cookie("ticket", map.get("ticket").toString());
            // 设置cookie有效范围，contextPath是项目根目录，所以整个项目都有效
            cookie.setPath(contextPath);
            // 设置cookie的有效时间
            cookie.setMaxAge(expiredSeconds);
            // 将cookie存到resp中传回给浏览器端
            resp.addCookie(cookie);
            // 重定向到主页
            logger.info(username + ":登陆成功");

            return "redirect:/index";
        } else {
            // 登录不成功返回错误信息
            model.addAttribute("usernameMsg", map.get("usernameMsg"));
            model.addAttribute("passwordMsg", map.get("passwordMsg"));
            // 下面这种跳转其实就是请求转发，就是说将request和response对象都转给下一个界面，本次请求没有结束，上面那个是重定向，表示本次请求中止
            return "/site/login";
        }
    }

    @RequestMapping(path = "/logout", method = RequestMethod.GET)
                        // 通过cookieValue这个注解来获取cookie中的内容
    public String logout(@CookieValue("ticket") String ticket) {
        userService.logout(ticket);
        logger.info("退出成功");

        // 退出时也要将用户凭证清理掉
        SecurityContextHolder.clearContext();
        return "redirect:/index";
    }

    @RequestMapping(path = "/forget", method = RequestMethod.GET)
    public String getForgetPage() {
        return "/site/forget";
    }

    @RequestMapping(path = "/code", method = RequestMethod.POST)
    @ResponseBody
    public String sendCode(String email, HttpSession session, Model model) {
        String code = userService.forget(email);

        session.setAttribute("code", code);
        session.setMaxInactiveInterval(300);
        return CommunityUtil.getJSONString(0);
    }

    @RequestMapping(path = "/forgetPwd", method = RequestMethod.POST)
    public String forgetPwd(String email, String code, String pwd, Model model, HttpSession session) {
        User user = userService.getUserByEmail(email);
        if (user == null) {
            model.addAttribute("emailMsg", "该邮箱未注册账户");
            return "/site/forget";
        }

        if (code == null) {
            model.addAttribute("codeMsg", "请输入验证码");
            return "/site/forget";
        }

        if (!code.equals(session.getAttribute("code"))) {
            model.addAttribute("codeMsg", "验证码错误");
            return "/site/forget";
        }

        if (userService.updatePassword(user.getId(), CommunityUtil.md5(pwd + user.getSalt())) == 0) {
            model.addAttribute("pwdMsg", "修改密码失败");
            return "/site/forget";
        }

        model.addAttribute("msg", "您的账号修改密码成功!");
        // 放的是controller访问路径
        model.addAttribute("target", "/login");

        return "/site/operate-result";
    }
}
