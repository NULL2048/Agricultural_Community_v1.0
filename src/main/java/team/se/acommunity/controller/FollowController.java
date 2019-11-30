package team.se.acommunity.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import team.se.acommunity.annotation.LoginRequired;
import team.se.acommunity.entity.Event;
import team.se.acommunity.entity.Page;
import team.se.acommunity.entity.User;
import team.se.acommunity.event.EventProducer;
import team.se.acommunity.service.FollowService;
import team.se.acommunity.service.UserService;
import team.se.acommunity.util.CommunityConstant;
import team.se.acommunity.util.CommunityUtil;
import team.se.acommunity.util.HostHolder;

import java.util.List;
import java.util.Map;

@Controller
public class FollowController implements CommunityConstant {
    @Autowired
    private FollowService followService;

    @Autowired
    private HostHolder hostHolder;

    @Autowired
    private UserService userService;

    // 将生产者事件注入
    @Autowired
    private EventProducer eventProducer;

    @LoginRequired
    @RequestMapping(path = "/follow", method = RequestMethod.POST)
    @ResponseBody
    /**
     * 关注请求，需要登陆凭证，异步请求
     */
    public String follow(int entityType, int entityId) {
        User user = hostHolder.getUser();

        followService.follow(user.getId(), entityType, entityId);

        // 触发关注事件
        Event event = new Event()
                .setTopic(TOPIC_FOLLOW) // 设置主题
                .setUserId(hostHolder.getUser().getId()) // 设置触发这个事件的用户
                .setEntityType(entityType) // 设置这个实体的类型
                .setEntityId(entityId) // 设置这个实体的id
                .setEntityUserId(entityId); // 设置被关注实体的所有者id

        eventProducer.fireEvent(event);

        return CommunityUtil.getJSONString(0, "已关注");
    }

    @LoginRequired
    @RequestMapping(path = "/unfollow", method = RequestMethod.POST)
    @ResponseBody
    public String unfollow(int entityType, int entityId) {
        User user = hostHolder.getUser();

        followService.unfollow(user.getId(), entityType, entityId);

        return CommunityUtil.getJSONString(0, "已取消关注");
    }

    /**
     * 查询某个用户的粉丝
     * @param userId
     * @param page
     * @param model
     * @return
     */
    @RequestMapping(path = "/followees/{userId}", method = RequestMethod.GET)
    public String getFollowees(@PathVariable("userId") int userId, Page page, Model model) {
        User user = userService.getUserById(userId);

        if (user == null) {
            throw new RuntimeException("该用户不存在");
        }
        model.addAttribute("user", user);

        page.setLimit(5);
        page.setPath("/followees/" + userId);
        page.setRows((int) followService.countFollowee(userId, CommunityConstant.ENTITY_TYPE_USER));

        List<Map<String, Object>> userList = followService.listFollowees(userId, page.getOffset(), page.getLimit());

        if (userList != null) {
            for (Map<String, Object> map : userList) {
                User u = (User) map.get("user");
                // 查看当前登录用户是否已经关注该用户
                map.put("hasFollowed", hasFollowed(u.getId()));
            }
        }
        model.addAttribute("users", userList);
        return "/site/followee";
    }

    /**
     * 查看当前用户是否已经关注了userId这个用户
     * @param userId 查询是否已经关注userId这个用户
     * @return
     */
    private boolean hasFollowed(int userId) {
        // 如果当前没有登录账号，那么肯定没有关注
        if (hostHolder.getUser() == null) {
            return false;
        }

        return followService.hasFollowed(hostHolder.getUser().getId(), CommunityConstant.ENTITY_TYPE_USER, userId);
    }

    /**
     * 查询当前用户关注的人
     * @param userId
     * @param page
     * @param model
     * @return
     */
    @RequestMapping(path = "/followers/{userId}", method = RequestMethod.GET)
    public String getFollowers(@PathVariable("userId") int userId, Page page, Model model) {
        User user = userService.getUserById(userId);

        if (user == null) {
            throw new RuntimeException("该用户不存在");
        }
        model.addAttribute("user", user);

        page.setLimit(5);
        page.setPath("/followees/" + userId);
        page.setRows((int) followService.countFollower(CommunityConstant.ENTITY_TYPE_USER, userId));

        List<Map<String, Object>> userList = followService.listFollowers(userId, page.getOffset(), page.getLimit());

        if (userList != null) {
            for (Map<String, Object> map : userList) {
                User u = (User) map.get("user");
                // 查看当前登录用户是否已经关注该用户
                map.put("hasFollowed", hasFollowed(u.getId()));
            }
        }
        model.addAttribute("users", userList);
        return "/site/follower";
    }
}
