package team.se.acommunity.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import team.se.acommunity.entity.DiscussPost;
import team.se.acommunity.entity.Page;
import team.se.acommunity.entity.User;
import team.se.acommunity.service.DiscussPostService;
import team.se.acommunity.service.LikeService;
import team.se.acommunity.service.UserService;
import team.se.acommunity.util.CommunityConstant;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// controller也是可以不用写访问路径的，如果不写controller的访问路径的话，到时候可以直接跳过controller路径，直接访问里面的方法的路径就行
@Controller
public class HomeController implements CommunityConstant {
    @Autowired
    private DiscussPostService discussPostService;
    @Autowired
    private UserService userService;
    @Autowired
    private LikeService likeService;

    @RequestMapping(path = "/index", method = RequestMethod.GET) // 设置界面传过来orderMode的默认值是0，如果没有传过来name为orderMode的值，就将参数orderMode设置为0
    public String getIndexPage(Model model, Page page, @RequestParam(name = "orderMode", defaultValue = "0") int orderMode) {
        // 方法调用前，spring MVC会会自动实例化model和page，并将page注入model，因为这两个都是参数
        // 所以，在Thymeleaf中可以直接访问page对象中的数据，不需要手动将page添加到model了
        page.setRows(discussPostService.getDiscussPostRows(0));
        // 这个分页的时候要加上排序规则，否则分页切换页的时候就有跳转到别的页了，因为排序原则参数使用过?拼接过来的
        page.setPath("/index?orderMode=" + orderMode); // 页面的访问路径

        List<DiscussPost> list = discussPostService.listDiscussPosts(0, page.getOffset(), page.getLimit(), orderMode);
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
        // 这个就是将discussPosts这个List对象作为值存入到model中，然后将他命名成discussPosts，以后从model中取discussPosts这个对象就要通过键名discussPosts来取得
        // 这个model就对被传送到界面，实现不同层级的数据传送
        model.addAttribute("discussPosts", discussPosts);
        // 返回的是模板的路径，也就是主页的路径,就是想要把model对象中存储的数据传给哪一个界面
        // 注意区分下面这个/index和上面那个/index。上面那个是在浏览器访问的时候写index就访问了这个controller，然后这个controller处理了数据后，return给了index.html这个模板，界面就给跳转到了indext.html了，下面这个return写的是html模板的名字

        model.addAttribute("orderMode", orderMode);

        return "/index";
    }

    @RequestMapping(path = "/error", method = RequestMethod.GET)
    public String getErrorPage() {
        return "/error/500";
    }

    // 拒接访问时的提示页面
    @RequestMapping(path = "/denied", method = RequestMethod.GET)
    public String getDeniedPage() {
        return "/error/404";
    }
}
