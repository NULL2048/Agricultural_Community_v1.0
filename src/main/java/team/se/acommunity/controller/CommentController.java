package team.se.acommunity.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import team.se.acommunity.entity.Comment;
import team.se.acommunity.entity.DiscussPost;
import team.se.acommunity.entity.Event;
import team.se.acommunity.event.EventProducer;
import team.se.acommunity.service.CommentService;
import team.se.acommunity.service.DiscussPostService;
import team.se.acommunity.util.CommunityConstant;
import team.se.acommunity.util.HostHolder;
import team.se.acommunity.util.RedisKeyUtil;

import java.util.Date;

@Controller
@RequestMapping(path = "/comment")
public class CommentController implements CommunityConstant {
    @Autowired
    private CommentService commentService;
    // 添加评论需要知道是谁添加的，所以需要通过这个hostHolder方法得到当前登录的用户信息
    @Autowired
    private HostHolder hostHolder;

    // 将生产者事件注入
    @Autowired
    private EventProducer eventProducer;

    @Autowired
    private DiscussPostService discussPostService;

    @Autowired
    private RedisTemplate redisTemplate;

    // 因为添加评论之后希望重定向到当前的文章页面，但是文章页面的访问路径中有动态变量帖子id，所以添加评论的访问地址也需要将文章id传进来
    @RequestMapping(path = "add/{discussPostId}", method = RequestMethod.POST)
    public String addComment(@PathVariable("discussPostId") int discussPostId, Comment comment) {
        comment.setUserId(hostHolder.getUser().getId());
        comment.setStatus(0);
        comment.setCreateTime(new Date());

        commentService.saveComment(comment);

        // 触发评论事件  可以学习下面的方法调用写法，比较方便
        Event event = new Event()
                .setTopic(TOPIC_COMMENT) // 设置主题
                .setUserId(hostHolder.getUser().getId()) // 设置触发这个事件的用户
                .setEntityType(comment.getEntityType()) // 设置这个实体的类型
                .setEntityId(comment.getEntityId()) // 设置这个实体的id
                .setData("postId", discussPostId); // 因为每一个系统消息会显示谁给自己评论，或者点赞了，会有一条链接点开直接到评论点赞的具体页面，这里就需要在附加数据存一下这个评论的文章的id，到时候会根据这个文章id直接定向到它的页面

        // 还差一个entityUserId
        if (comment.getEntityType() == ENTITY_TYPE_POST) { // 当前评论的是一个帖子
            DiscussPost target = discussPostService.getDiscussPostById(comment.getEntityId());
            event.setEntityUserId(target.getUserId());
        } else if (comment.getEntityType() == ENTITY_TYPE_COMMENT) { // 当前回复的是一个评论
            Comment target = commentService.getCommentById(comment.getEntityId());
            event.setEntityUserId(target.getUserId());
        }

        // 触发事件  触发了之后剩下的工作就是消息队列在别的线程中进行了，这个线程会直接接着像后面执行，进行跳转页面。这是一个并发的操作，两种操作一起执行，效率更高，如果这个文章热度比较高，评论多他们就直接将评论事件放到消息队列中了，起到了一个缓冲作用，这就是消息队列的好处，提高系统支持高并发能力
        eventProducer.fireEvent(event);

        // 当文章有了评论之后，文章的评论数量就变了，它的排名分数也就变了，就相当于这篇文章有了修改，所以要重新修改es服务器中的文章数据
        // 只有评论给帖子的时候才需要触发发帖事件
        if (comment.getEntityType() == ENTITY_TYPE_POST) {
            event = new Event()
                    .setTopic(TOPIC_PUBLISH) // 设置事件主题
                    .setUserId(comment.getUserId()) // 设置谁触发了这个事件
                    .setEntityType(ENTITY_TYPE_POST) // 设置事件主体类型
                    .setEntityId(discussPostId); // 设置事件主体id
            eventProducer.fireEvent(event);

            // 只有对帖子评论才要进行计算分数，所以写到这个if代码块中
            String redisKey = RedisKeyUtil.getPostScoreKey();
            // 存入要计算分数的帖子id
            redisTemplate.opsForSet().add(redisKey, discussPostId);
        }
        // 结束本次请求，重定向到评论的帖子界面。因为这个方法不需要想别的页面返回什么信息了，因为在这里重定向本次请求就结束了，所以这个方法就没有model这个对象
        return "redirect:/discuss/detail/" + discussPostId;
    }
}
