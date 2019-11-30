package team.se.acommunity.controller;

import com.sun.org.apache.xpath.internal.operations.Mod;
import org.apache.commons.lang3.StringUtils;
import org.checkerframework.checker.units.qual.A;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import team.se.acommunity.annotation.LoginRequired;
import team.se.acommunity.entity.Comment;
import team.se.acommunity.entity.DiscussPost;
import team.se.acommunity.entity.Page;
import team.se.acommunity.entity.User;
import team.se.acommunity.service.*;
import team.se.acommunity.util.CommunityConstant;
import team.se.acommunity.util.CommunityUtil;
import team.se.acommunity.util.HostHolder;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/user")
public class UserController implements CommunityConstant {
    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    @Value("${community.path.upload}")
    private String uploadPath;

    @Value("${community.path.domain}")
    private String domain;

    @Value("${server.servlet.context-path}")
    private String contextPath;

    @Autowired
    private UserService userService;

    @Autowired
    private HostHolder hostHolder;

    @Autowired
    private LikeService likeService;

    @Autowired
    private FollowService followService;

    @Autowired
    private DiscussPostService discussPostService;

    @Autowired
    private CommentService commentService;

    // 注入用户密钥
    @Value("${aliyun.key.access}")
    private String accessKey;

    // 注入加密密钥
    @Value("${aliyun.key.secret}")
    private String secretKey;

    // 注入存储空间的名字
    @Value("${aliyun.bucket.header.name}")
    private String headerBucketName;

    // 注入存储空间的访问路径
    @Value("${aliyun.bucket.header.url}")
    private String headerBucketUrl;

    @LoginRequired
    @RequestMapping(path = "/setting", method = RequestMethod.GET)
    public String getSettingPage(Model model) {
        // 上传文件名称
        // 最好使用随机文件名，因为这样用户上传过的所有头像都会保留，方便以后开发历史查询功能，并且因为服务器都有缓存，可能上传同名文件虽然会覆盖原文件，但是不会马上生效，所以避免这种问题及直接用随机文件名，因为对象存储空间比较便宜，所以也不会加大太多成本
        String fileName = CommunityUtil.generateUUID();
        String url = headerBucketUrl + "/" + fileName;

        model.addAttribute("fileName", fileName);
        model.addAttribute("url", url);

        return "/site/setting";
    }

    // 更新数据库中的头像路径
    @RequestMapping(path = "/header/url", method = RequestMethod.POST)
    @ResponseBody
    public String updateHeaderUrl(String fileName) {
        if (StringUtils.isBlank(fileName)) {
            return CommunityUtil.getJSONString(1, "文件名不能为空");
        }

        // 设置文件的访问路径  访问路径就是OSS空间的域名 + /存储对象名 + /文件名   就是上传文件的访问路径
        // 这里的文件名必须是带文件后缀的，否则访问不到
        String url = headerBucketUrl + "/" + headerBucketName + "/" + fileName;
        userService.updateHeader(hostHolder.getUser().getId(), url);

        // 返回数据，告知修改成功
        return CommunityUtil.getJSONString(0);
    }

    // 将数据迁移到云数据库中，这个方法作废
    // 上传的时候表单方法必须为post
    @LoginRequired
    @RequestMapping(path = "/upload", method = RequestMethod.POST)
    public String uploadHeader(MultipartFile headerImage, Model model) {
        if (headerImage == null) {
            model.addAttribute("error", "您还没有选择图片");
            return "/site/setting";
        }

        String fileName = headerImage.getOriginalFilename();
        // 学习string的这些方法
        String suffix = fileName.substring(fileName.lastIndexOf('.'));
        if (StringUtils.isBlank(suffix)) {
            model.addAttribute("error", "文件的格式不正确");
            return "/site/setting";
        }

        // 生成随机文件名
        fileName = CommunityUtil.generateUUID() + suffix;
        // 确定文件存放的路径
        File dest = new File(uploadPath + "/" + fileName);
        try {
            // 将文件内容写入file
            headerImage.transferTo(dest);
        } catch (IOException e) {
            logger.error("上传文件失败：" + e.getMessage());
            throw new RuntimeException("上传文件失败，服务器发生异常");
        }

        // 更新当前用户的头像路径（web访问路径）
        // http://locahost:8080/ac/user/header/xxx.png
        User user = hostHolder.getUser();
        String headerUrl = domain + contextPath + "/user/header/" + fileName;
        userService.updateHeader(user.getId(), headerUrl);

        return "redirect:/index";
    }
    /*
    * return “/site/index”是返回一个模板路径，本次请求没有处理完，DispatcherServlet会将Model中的数据和对应的模板提交给模板引擎，让它继续处理完这次请求。
    * return "redirect:/index"是重定向，表示本次请求已经处理完毕，但是没有什么合适的数据展现给客户端，建议客户端再发一次请求，访问"/index"以获得合适的数据。
    * */

    // 将数据迁移到云数据库中，这个方法作废
    // 获取图片的方法，路径要按照上面那个方法设置的那样，在这里用{}引入一个变量，这个方法用get就行
    @RequestMapping(path = "/header/{fileName}", method = RequestMethod.GET)
    // 这个方法不是向浏览器放回一个字符串也不是像浏览器返回一个页面，而是返回一个二进制数据，所以这里的返回值类型用void就可以，利用responese就可以把数据传过去
                           // 取出访问路径中的{fileName}变量，将其赋值给参数String fileName
    public void getHeader(@PathVariable("fileName") String fileName, HttpServletResponse response) {
        // 服务器存放路径
        fileName = uploadPath + "/" + fileName;
        // 文件的后缀
        String suffix = fileName.substring(fileName.lastIndexOf("."));
        // 相应图片
        response.setContentType("image/" + suffix);
        try { // 这个是1.7的语法，单独写在这个括号里编译的时候会自动加上finally，然后将这些流都关闭
            // 这个输出流是response的输出流，response是由spring mvc管理的，它会自动将这个输出流关闭
            OutputStream os = response.getOutputStream();
            // 但是这个文件输入流不是spring mvc管理的，需要手动关闭
            // 将路径中的文件写入到这个file输入流中
            FileInputStream fis = new FileInputStream(fileName);
            {
                // 使用一个字节流对象作为缓冲区，来分批将图片传入到response输出流中
                byte[] buffer = new byte[1024];
                int b = 0;
                // 如果返回-1 说明取不出来东西了
                // read是从fis中读取出内容
                while ((b = fis.read(buffer)) != -1) {
                    // write是从buffer中向os写入内容
                    os.write(buffer, 0, b);
                }
            }
        } catch (IOException e) {
            logger.error("读取头像失败：" + e.getMessage());
        }
    }

    /**
     * 个人主页
     * @param userId
     * @param model
     * @return
     */
    @RequestMapping(path = "/profile/{userId}", method = RequestMethod.GET)
    public String getProfilePage(@PathVariable("userId") int userId, Model model) {
        // 因为用户主页页面所有的账号都可以看，所以获得user对象不能用过hostHold
        User user = userService.getUserById(userId);

        // 为了防止恶意攻击，防止恶意注入，黑客从地址栏填入id来检查这个id存不存在，所以在这里要判断这个user查没查到，没查到直接抛出异常
        if (user == null) {
            throw new RuntimeException("给用户不存在！");
        }

        // 将用户信息传给界面
        model.addAttribute("user", user);
        // 点赞数量
        int likeCount = likeService.countUserLike(userId);
        model.addAttribute("likeCount", likeCount);

        // 关注数量
        long followeeCount = followService.countFollowee(userId, ENTITY_TYPE_USER);
        model.addAttribute("followeeCount", followeeCount);
        // 粉丝数量
        long followerCount = followService.countFollower(ENTITY_TYPE_USER, userId);
        model.addAttribute("followerCount", followerCount);
        // 是否已关注
        boolean hasFollowed = false;
        if (hostHolder.getUser() != null) {
            hasFollowed = followService.hasFollowed(hostHolder.getUser().getId(), ENTITY_TYPE_USER, userId);
        }
        model.addAttribute("hasFollowed", hasFollowed);

        model.addAttribute("userId", userId);

        return "/site/profile";
    }

    /**
     * 查看该用户发表的所有文章
     * @param userId
     * @param model
     * @param page
     * @return
     */
    @RequestMapping(path = "/posts/{userId}", method = RequestMethod.GET)
    public String listUserPosts(@PathVariable("userId") int userId, Model model, Page page) {
        int rows = discussPostService.getDiscussPostRows(userId);
        page.setRows(rows);
        page.setPath("/user/posts/" + userId);

        List<DiscussPost> list = discussPostService.listDiscussPosts(userId, page.getOffset(), page.getLimit(), 0);
        List<Map<String, Object>> discussPosts = new ArrayList<>();
        if (list != null) {
            for (DiscussPost post : list) {
                Map<String, Object> map = new HashMap<>();
                map.put("post", post);

                User user = userService.getUserById(post.getUserId());
                map.put("user", user);
                // 查询当前帖子点赞数量
                long likeCount = likeService.countEntityLike(ENTITY_TYPE_POST, post.getId());
                map.put("likeCount", likeCount);
                discussPosts.add(map);
            }
        }

        model.addAttribute("discussPosts", discussPosts);
        model.addAttribute("postCount", rows);
        model.addAttribute("userId", userId);


        return "/site/my-post";
    }

    /**
     * 查看该用户的所有对文章的评论
     * @param userId
     * @param model
     * @param page
     * @return
     */
    @RequestMapping(path = "/comments/{userId}", method = RequestMethod.GET)
    public String listUserComments(@PathVariable("userId") int userId, Model model, Page page) {
        int rows = commentService.countCommentByUserId(userId, ENTITY_TYPE_POST);
        page.setRows(rows);
        page.setPath("/user/comment/" + userId);

        List<Comment> list = commentService.listCommentsByUserId(userId, ENTITY_TYPE_POST, page.getOffset(), page.getLimit());

        List<Map<String, Object>> comments = new ArrayList<>();
        if (list != null) {
            for (Comment comment : list) {
                Map<String, Object> map = new HashMap<>();
                map.put("comment", comment);

                DiscussPost post = discussPostService.getDiscussPostById(comment.getEntityId());
                map.put("post", post);

                comments.add(map);
            }
        }

        model.addAttribute("comments", comments);
        model.addAttribute("commentsCount", rows);
        model.addAttribute("userId", userId);

        return "/site/my-reply";
    }

    @LoginRequired
    @RequestMapping(path = "/updatepwd", method = RequestMethod.POST)
    public String updatePassword(String oldPassword, String newPassword, String newPassword1, Model model, @CookieValue("ticket") String ticket) {
        User user = hostHolder.getUser();

        if (StringUtils.isBlank(oldPassword)) {
            model.addAttribute("oldPwdMsg", "原密码不能为空");
            return "/site/setting";
        }

        if (StringUtils.isBlank(newPassword)) {
            model.addAttribute("newPwdMsg", "新密码不能为空");
            return "/site/setting";
        }

        if (!newPassword.equals(newPassword1)) {
            model.addAttribute("newPwdMsg1", "两次密码不一致");
            return "/site/setting";
        }

        if (newPassword.length() < 8) {
            model.addAttribute("newPwdMsg", "密码长度不能小于8位");
            return "/site/setting";
        }

        oldPassword = CommunityUtil.md5(oldPassword + user.getSalt());
        if (!user.getPassword().equals(oldPassword)) {
            userService.updatePassword(user.getId(), CommunityUtil.md5(newPassword + user.getSalt()));
            model.addAttribute("msg", "修改密码成功");

            userService.logout(ticket);
            return "redirect:/login";
        } else {
            model.addAttribute("oldPwdMsg", "原密码不正确");
            return "/site/setting";
        }
    }
}
