package team.se.acommunity.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import team.se.acommunity.annotation.LoginRequired;
import team.se.acommunity.entity.Event;
import team.se.acommunity.entity.User;
import team.se.acommunity.event.EventProducer;
import team.se.acommunity.service.LikeService;
import team.se.acommunity.util.CommunityConstant;
import team.se.acommunity.util.CommunityUtil;
import team.se.acommunity.util.HostHolder;
import team.se.acommunity.util.RedisKeyUtil;

import java.util.HashMap;
import java.util.Map;

@Controller
public class LikeController implements CommunityConstant {
    @Autowired
    private LikeService likeService;

    @Autowired
    private HostHolder hostHolder;

    // 将生产者事件注入
    @Autowired
    private EventProducer eventProducer;

    @Autowired
    private RedisTemplate redisTemplate;

    // 这个注释表明这个请求需要有登陆凭证，拦截器会拦截它
    @LoginRequired
    @RequestMapping(path = "/like", method = RequestMethod.POST)
    // 因为这里的点赞操作时异步请求，所以必须加上ResponseBody这个标签，通过这个路径发送请求的时候都是异步请求，不会刷新原有界面
    @ResponseBody // 加上他就表示是异步请求，从浏览器传过来的是json格式数据，他会自动根据key将json中的值取出来，赋值给这个方法的参数
    public String like(int entityType, int entityId, int entityUserId, int postId) {
        User user = hostHolder.getUser();

        // 点赞
        likeService.saveLike(user.getId(), entityType, entityId, entityUserId);

        // 获得点赞数量
        long likeCount = likeService.countEntityLike(entityType, entityId);
        // 获得状态
        int likeStatus = likeService.getEntityLikeStatus(user.getId(), entityType, entityId);

        // 将上面的两个值要发回给页面
        // 将这两个值封装一下，方便传输
        Map<String, Object> map = new HashMap<>();
        map.put("likeCount", likeCount);
        map.put("likeStatus", likeStatus);

        // 触发点赞事件
        // 只有点赞才需要发通知，取消赞是不用发通知的
        if (likeStatus == 1) {
            Event event = new Event()
                    .setTopic(TOPIC_LIKE) // 设置主题
                    .setUserId(hostHolder.getUser().getId()) // 设置触发这个事件的用户
                    .setEntityType(entityType) // 设置这个实体的类型
                    .setEntityId(entityId) // 设置这个实体的id
                    .setData("postId", postId) // 因为每一个系统消息会显示谁给自己评论，或者点赞了，会有一条链接点开直接到评论点赞的具体页面，这里就需要在附加数据存一下这个评论的文章的id，到时候会根据这个文章id直接定向到它的页面
                    .setEntityUserId(entityUserId); // 设置被点赞实体的所有者id

            eventProducer.fireEvent(event);
        }

        // 只有当点赞的是文章才计算分数
        if (entityType == ENTITY_TYPE_POST) {
            String redisKey = RedisKeyUtil.getPostScoreKey();
            // 存入要计算分数的帖子id
            redisTemplate.opsForSet().add(redisKey, postId);
        }

        // 因为这里是异步请求，所以就不能跳转页面了，要返回JSON数据，就跟ajax一样，在浏览器端它会将这段ajax数据给解析出来，状态码0表示相应成功
        return CommunityUtil.getJSONString(0, null, map);
    }
}
