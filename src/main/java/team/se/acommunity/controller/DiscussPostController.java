package team.se.acommunity.controller;

import io.lettuce.core.RedisURI;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import team.se.acommunity.entity.*;
import team.se.acommunity.event.EventProducer;
import team.se.acommunity.service.CommentService;
import team.se.acommunity.service.DiscussPostService;
import team.se.acommunity.service.LikeService;
import team.se.acommunity.service.UserService;
import team.se.acommunity.util.CommunityConstant;
import team.se.acommunity.util.CommunityUtil;
import team.se.acommunity.util.HostHolder;
import team.se.acommunity.util.RedisKeyUtil;

import java.util.*;

@Controller
@RequestMapping("/discuss")
public class DiscussPostController implements CommunityConstant {
    @Autowired
    private DiscussPostService discussPostService;
    @Autowired
    private UserService userService;

    @Autowired
    private HostHolder hostHolder;

    @Autowired
    private CommentService commentService;

    @Autowired
    private LikeService likeService;

    @Autowired
    private EventProducer eventProducer;

    @Autowired
    private RedisTemplate redisTemplate;

    @RequestMapping(path = "/add", method = RequestMethod.POST)
    @ResponseBody // 不是返回网页，所以加上ResponseBody标签

    public String addDiscussPost(String title, String content) {
        User user = hostHolder.getUser();
        // 403表示没有权限
        if (user == null) {
            return CommunityUtil.getJSONString(403, "你还没有登录！");
        }

        DiscussPost post = new DiscussPost();
        post.setUserId(user.getId());
        post.setTitle(title);
        post.setContent(content);
        post.setCreateTime(new Date());
        discussPostService.insertDiscussPost(post);

        // 将文章发表存入mysql数据库之后，就要再将这篇文章存入es服务器，要触发发表文章事件
        Event event = new Event()
                .setTopic(TOPIC_PUBLISH) // 设置事件主题
                .setUserId(user.getId()) // 设置谁触发了这个事件
                .setEntityType(ENTITY_TYPE_POST) // 设置事件主体类型
                .setEntityId(post.getId()); // 设置事件主体id
        eventProducer.fireEvent(event);

        // 计算帖子的分数
        // 先将这个帖子放到redis数据库中，以后用来标记哪些帖子有过变化，需要重新计算分数
        // 最好使用set这种能去重的结构，因为如果使用普通的表结构，他是存储，如果同一时间文章A被点了3次赞，就会被存入表3次，显然是重复的，因为我们只需要知道那些文章有变化，而不去关注被点赞评论的多少次，更不去关注其中的顺序，所以就可以采用自动去重并且无需的set数据结构来存储
        // 上面这个也是选用set数据结构时的一个判定标准
        String redisKey = RedisKeyUtil.getPostScoreKey();
        // 存入要计算分数的帖子id
        redisTemplate.opsForSet().add(redisKey, post.getId());

        // 报错的情况将来统一处理
        return CommunityUtil.getJSONString(0, "发布成功！");
    }
    // 返回文章的详细页，因为不同的文章有不同的文章id，所以这里的访问路径是动态的根据不同文章id来的访问的
    @RequestMapping(path = "/detail/{discussPostId}", method = RequestMethod.GET)
    public String getDiscussPost(@PathVariable("discussPostId") int discussPostId, Model model, Page page) {
        // 帖子
        DiscussPost post = discussPostService.getDiscussPostById(discussPostId);
        model.addAttribute("post", post);
        //作者
        User user = userService.getUserById(post.getUserId());
        model.addAttribute("user", user);
        // 点赞数量
        long likeCount = likeService.countEntityLike(ENTITY_TYPE_POST, discussPostId);
        model.addAttribute("likeCount", likeCount);
        // 点赞状态 检查当前用户是否对这个文章点过赞   这里要先判断用户是否登录，如果登陆了再进行相关查询，如果没有登陆就直接赋值成0，因为没有登陆也是要显示点赞数量的，只不过是没有点赞状态
        int likeStatus = hostHolder.getUser() == null ? 0 :
                likeService.getEntityLikeStatus(hostHolder.getUser().getId(), ENTITY_TYPE_POST, discussPostId);
        model.addAttribute("likeStatus", likeStatus);
        // 评论分页信息
        page.setLimit(5);
        page.setPath("/discuss/detail/" + discussPostId);
        page.setRows(post.getCommentCount());
        // 评论：给帖子的评论
        // 回复：给评论的评论
        // 评论列表
        List<Comment> commentList = commentService.listCommentsByEntity(ENTITY_TYPE_POST, post.getId(), page.getOffset(),
                page.getLimit());
        // 评论VO列表   VO表示就是要显示在页面上的列表 VO表示展示对象，在笔记中的Java命名规范领域模型命名规约中
        List<Map<String, Object>> commentVoList = new ArrayList<>();
        if (commentList != null) {
            for (Comment comment : commentList) {
                // 评论VO
                Map<String, Object> commentVO = new HashMap<>();
                // 评论
                commentVO.put("comment", comment);
                // 评论作者
                commentVO.put("user", userService.getUserById(comment.getUserId()));
                // 点赞数量
                likeCount = likeService.countEntityLike(ENTITY_TYPE_COMMENT, comment.getId());
                commentVO.put("likeCount", likeCount);
                // 点赞状态 检查当前用户是否对这个文章点过赞   这里要先判断用户是否登录，如果登陆了再进行相关查询，如果没有登陆就直接赋值成0，因为没有登陆也是要显示点赞数量的，只不过是没有点赞状态
                likeStatus = hostHolder.getUser() == null ? 0 :
                        likeService.getEntityLikeStatus(hostHolder.getUser().getId(), ENTITY_TYPE_COMMENT, comment.getId());
                commentVO.put("likeStatus", likeStatus);

                // 回复列表                                                                         // 这个标识最大值，也就是有多少条查多少条，不做分页了,然后在界面通过循环实现分页显示
                List<Comment> replyList = commentService.listCommentsByEntity(ENTITY_TYPE_COMMENT, comment.getId(), 0, Integer.MAX_VALUE);
                // 回复VO列表
                List<Map<String, Object>> replyVoList = new ArrayList<>();
                if (replyList != null) {
                    for (Comment reply : replyList) {
                        Map<String, Object> replyVO = new HashMap<>();
                        replyVO.put("reply", reply);
                        replyVO.put("user", userService.getUserById(reply.getUserId()));
                        // 回复的目标
                        User target = reply.getTargetId() == 0 ? null : userService.getUserById(reply.getTargetId());
                        replyVO.put("target", target);

                        // 点赞数量
                        likeCount = likeService.countEntityLike(ENTITY_TYPE_COMMENT, reply.getId());
                        replyVO.put("likeCount", likeCount);
                        // 点赞状态 检查当前用户是否对这个文章点过赞   这里要先判断用户是否登录，如果登陆了再进行相关查询，如果没有登陆就直接赋值成0，因为没有登陆也是要显示点赞数量的，只不过是没有点赞状态
                        likeStatus = hostHolder.getUser() == null ? 0 :
                                likeService.getEntityLikeStatus(hostHolder.getUser().getId(), ENTITY_TYPE_COMMENT, reply.getId());
                        replyVO.put("likeStatus", likeStatus);

                        replyVoList.add(replyVO);
                    }
                }
                commentVO.put("replys", replyVoList);

                // 回复数量
                int replyCount = commentService.countCommentsByEntity(ENTITY_TYPE_COMMENT, comment.getId());
                commentVO.put("replyCount", replyCount);

                commentVoList.add(commentVO);
            }
        }

        model.addAttribute("comments", commentVoList);
        return "/site/discuss-detail";
    }

    // 置顶  是一个异步请求
    @RequestMapping(path = "/top", method = RequestMethod.POST)
    @ResponseBody
    public String setTop(int id) {
        discussPostService.updateType(id, 1);

        // 触发贴子发布时间，将贴子添加到es服务器中
        Event event = new Event()
                .setTopic(TOPIC_PUBLISH) // 设置事件主题
                .setUserId(hostHolder.getUser().getId()) // 设置谁触发了这个事件
                .setEntityType(ENTITY_TYPE_POST) // 设置事件主体类型
                .setEntityId(id); // 设置事件主体id
        eventProducer.fireEvent(event);

        return CommunityUtil.getJSONString(0);
    }

    // 加精
    @RequestMapping(path = "/wonderful", method = RequestMethod.POST)
    @ResponseBody
    public String setWonderful(int id) {
        discussPostService.updateStatus(id, 1);

        // 触发贴子发布时间，将贴子添加到es服务器中
        Event event = new Event()
                .setTopic(TOPIC_PUBLISH) // 设置事件主题
                .setUserId(hostHolder.getUser().getId()) // 设置谁触发了这个事件
                .setEntityType(ENTITY_TYPE_POST) // 设置事件主体类型
                .setEntityId(id); // 设置事件主体id
        eventProducer.fireEvent(event);

        String redisKey = RedisKeyUtil.getPostScoreKey();
        // 存入要计算分数的帖子id
        redisTemplate.opsForSet().add(redisKey, id);

        return CommunityUtil.getJSONString(0);
    }

    // 删除帖子
    @RequestMapping(path = "/delete", method = RequestMethod.POST)
    @ResponseBody
    public String setDelete(int id) {
        // 这里的删除只是状态改变，并没有将贴子在数据库中删除
        discussPostService.updateStatus(id, 2);

        // 触发一个删帖事件
        Event event = new Event()
                .setTopic(TOPIC_DELETE) // 设置事件主题
                .setUserId(hostHolder.getUser().getId()) // 设置谁触发了这个事件
                .setEntityType(ENTITY_TYPE_POST) // 设置事件主体类型
                .setEntityId(id); // 设置事件主体id
        eventProducer.fireEvent(event);

        return CommunityUtil.getJSONString(0);
    }
}
