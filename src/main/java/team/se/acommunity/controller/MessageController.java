package team.se.acommunity.controller;

import com.alibaba.fastjson.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.util.HtmlUtils;
import team.se.acommunity.entity.Message;
import team.se.acommunity.entity.Page;
import team.se.acommunity.entity.User;
import team.se.acommunity.service.MessageService;
import team.se.acommunity.service.UserService;
import team.se.acommunity.util.CommunityConstant;
import team.se.acommunity.util.CommunityUtil;
import team.se.acommunity.util.HostHolder;

import java.util.*;

@Controller
public class MessageController implements CommunityConstant {
    @Autowired
    private MessageService messageService;
    // 因为处理的是当前用户的操作，所以要注入HostHolder
    @Autowired
    private HostHolder hostHolder;
    @Autowired
    private UserService userService;

    // 私信列表
    @RequestMapping(path = "/letter/list", method = RequestMethod.GET)
    public String getLetterList(Model model, Page page) {
        User user = hostHolder.getUser();

        // 设置分页信息
        // 每一页显示五条数据
        page.setLimit(5);
        // 设置访问地址
        page.setPath("/letter/list");
        // 设置数据总条数
        page.setRows(messageService.countConversation(user.getId()));

        // 会话列表
        List<Message> conversationList = messageService.listConversations(user.getId(), page.getOffset(), page.getLimit());

        // 存储页面相关信息，如各个未读消息数等等
        List<Map<String, Object>> conversations = new ArrayList<>();
        if (conversationList != null) {
            for (Message message : conversationList) {
                Map<String, Object> map = new HashMap<>();
                // 存储每一个会话最新私信内容
                map.put("conversation", message);
                // 存储每一条会话中私信的数量
                map.put("letterCount", messageService.countLetter(message.getConversationId()));
                // 存储未读会话数
                map.put("unreadCount", messageService.countUnreadLetter(user.getId(), message.getConversationId()));

                // 取得会话的对方身份，要先判断一下相对于当前用户是fromID是对方用户还是toID是对方用户
                int targetId = user.getId() == message.getFromId() ? message.getToId() : message.getFromId();
                map.put("target", userService.getUserById(targetId));

                conversations.add(map);
            }
        }

        model.addAttribute("conversations", conversations);

        // 查询用户未读消息总数
        int letterUnreadCount = messageService.countUnreadLetter(user.getId(), null);
        model.addAttribute("letterUnreadCount", letterUnreadCount);

        // 查询未读通知的数量
        int noticeUnreadCount = messageService.countUnreadNotice(user.getId(), null);
        model.addAttribute("noticeUnreadCount", noticeUnreadCount);

        return "/site/letter";
    }

    /**
     * 获得私信详细信息
     * @param conversationId
     * @param page
     * @param model
     * @return
     */
    @RequestMapping(path = "/letter/detail/{conversationId}", method = RequestMethod.GET)
    public String getLetterDetail(@PathVariable("conversationId") String conversationId, Page page, Model model) {
        // 设置分页信息
        page.setLimit(5);
        page.setPath("/letter/detail/" + conversationId);
        page.setRows(messageService.countLetter(conversationId));

        // 得到得到一页私信的集合，以后点下一页就会重新进到这个cottoller中，因为page的path就是这个路径，并且点击下一页之后page中的offset也会改变，所以就会取到下一页数据
        List<Message> letterList = messageService.listLetters(conversationId, page.getOffset(), page.getLimit());

        List<Map<String, Object>> letters = new ArrayList<>();
        if (letterList != null) {
            for (Message message : letterList) {
                Map<String, Object> map = new HashMap<>();
                map.put("letter", message);
                // 将每一条私信的发从者取出来，因为会在页面中显示
                map.put("fromUser", userService.getUserById(message.getFromId()));

                letters.add(map);
            }
        }

        model.addAttribute("letters", letters);
        // 查询私信目标
        model.addAttribute("target", getLetterTarget(conversationId));

        // 设置已读
        List<Integer> ids = getUnreadLetterIds(letterList);
        // 这里要先判断ids是否为空，如果为空就说明没有未读消息，这里需要注意需要用isEmpty这个方法，因为ids永远不可能为null，因为在方法里面已经创建好了实例
        if (!ids.isEmpty()) {
            messageService.updateReadMessage(ids);
        }

        return "/site/letter-detail";
    }

    /**
     * 用来取得当前登陆账号中会话的对方的用户信息，这里构建一个私有方法，方便以后复用
     * @param conversationId 会话ID
     * @return
     */
    private User getLetterTarget(String conversationId) {
        // 因为会话ID就是会话双方的ID拼接而成的，所以直接用方法把String拆分成两个字符串
        String[] ids = conversationId.split("_");
        // 将字符串ID转换成数值ID
        int id1 = Integer.valueOf(ids[0]);
        int id2 = Integer.valueOf(ids[1]);

        // 取出当前登录用户，并且和上面的两个ID比较，不一样的就是会话对方用户
        if (hostHolder.getUser().getId() != id1) {
            return userService.getUserById(id1);
        } else {
            return userService.getUserById(id2);
        }
    }

    /**
     * 写一个私有方法，用来取得消息集合中所有未读消息的id
     * @param letterList
     * @return
     */
    private List<Integer> getUnreadLetterIds(List<Message> letterList) {
        List<Integer> ids = new ArrayList<>();

        if (letterList != null) {
            for (Message message : letterList) {
                // 要保证当前用户是私信的接收方才行，这样它对这条消息才有已读未读的区别
                if (hostHolder.getUser().getId() == message.getToId() && message.getStatus() == 0) {
                    ids.add(message.getId());
                }
            }
        }

        return ids;
    }
    // 只要是要提交数据的请求，请求方法都需要用post
    @RequestMapping(path = "/letter/send", method = RequestMethod.POST)
    @ResponseBody // 这个方法是异步的，所以这里要加ResponseBody这个标签
    public String sendMessage(String toName, String content) {
        User target = userService.getUserByName(toName);

        if (target == null) {
            return CommunityUtil.getJSONString(1, "目标用户不存在！");
        }

        Message message = new Message();
        message.setFromId(hostHolder.getUser().getId());
        message.setToId(target.getId());
        message.setContent(content);
        // 会话ID要小的拼接在前面
        if (message.getToId() < message.getFromId()) {
            message.setConversationId(message.getToId() + "_" + message.getFromId());
        } else {
            message.setConversationId(message.getFromId() + "_" + message.getToId());
        }
        message.setCreateTime(new Date());

        messageService.saveMessage(message);
        // 如果没有问题直接给页面返回一个0状态就可以了
        return CommunityUtil.getJSONString(0);
    }

    @RequestMapping(path = "/notice/list", method = RequestMethod.GET)
    public String getNoticeList(Model model) {
        // 获得当前登录用户
        User user = hostHolder.getUser();

        // 查询评论类通知
        Message message = messageService.getLastedNotice(user.getId(), TOPIC_COMMENT);
        // 用一个Map来封装聚合数据
        // 必须都要先判断当前这个用户用没有这一类的通知，如果没有这一类的通知也就不需要实例化messageVO了，页面上也不需要显示相关内容
        if (message != null) {
            Map<String, Object> messageVO = new HashMap<>();

            if (messageVO != null) {
                // 先将这个对象存入
                messageVO.put("message", message);

                // 之前发数据的时候都是将html标签内容转译，现在是将转移的html再变回来,因为content里面存的是json字符串，方便将json转换成map格式
                String content = HtmlUtils.htmlUnescape(message.getContent());
                Map<String, Object> data = JSONObject.parseObject(content, HashMap.class);

                // 将content里面的内容都取出来放入messageVO中
                messageVO.put("user", userService.getUserById((Integer) data.get("userId")));
                messageVO.put("entityType", data.get("entityType"));
                messageVO.put("entityId", data.get("entityId"));
                messageVO.put("postId", data.get("postId"));

                int count = messageService.countNotice(user.getId(), TOPIC_COMMENT);
                messageVO.put("count", count);

                int unread = messageService.countUnreadNotice(user.getId(), TOPIC_COMMENT);
                messageVO.put("unread", unread);
            }
            model.addAttribute("commentNotice", messageVO);
        }

        // 查询点赞类通知
        message = messageService.getLastedNotice(user.getId(), TOPIC_LIKE);
        // 用一个Map来封装聚合数据
        if (message != null) {
            Map<String, Object> messageVO = new HashMap<>();
            if (messageVO != null) {
                // 先将这个对象存入
                messageVO.put("message", message);

                // 之前发数据的时候都是将html标签内容转译，现在是将转移的html再变回来,因为content里面存的是json字符串，方便将json转换成map格式
                String content = HtmlUtils.htmlUnescape(message.getContent());
                Map<String, Object> data = JSONObject.parseObject(content, HashMap.class);

                // 将content里面的内容都取出来放入messageVO中
                messageVO.put("user", userService.getUserById((Integer) data.get("userId")));
                messageVO.put("entityType", data.get("entityType"));
                messageVO.put("entityId", data.get("entityId"));
                messageVO.put("postId", data.get("postId"));

                int count = messageService.countNotice(user.getId(), TOPIC_LIKE);
                messageVO.put("count", count);

                int unread = messageService.countUnreadNotice(user.getId(), TOPIC_LIKE);
                messageVO.put("unread", unread);
            }
            model.addAttribute("likeNotice", messageVO);
        }

        // 查询关注类通知
        message = messageService.getLastedNotice(user.getId(), TOPIC_FOLLOW);
        // 用一个Map来封装聚合数据
        if (message != null) {
            Map<String, Object> messageVO = new HashMap<>();
            if (messageVO != null) {
                // 先将这个对象存入
                messageVO.put("message", message);

                // 之前发数据的时候都是将html标签内容转译，现在是将转移的html再变回来,因为content里面存的是json字符串，方便将json转换成map格式
                String content = HtmlUtils.htmlUnescape(message.getContent());
                Map<String, Object> data = JSONObject.parseObject(content, HashMap.class);

                // 将content里面的内容都取出来放入messageVO中
                messageVO.put("user", userService.getUserById((Integer) data.get("userId")));
                messageVO.put("entityType", data.get("entityType"));
                messageVO.put("entityId", data.get("entityId"));

                int count = messageService.countNotice(user.getId(), TOPIC_FOLLOW);
                messageVO.put("count", count);

                int unread = messageService.countUnreadNotice(user.getId(), TOPIC_FOLLOW);
                messageVO.put("unread", unread);
            }
            model.addAttribute("followNotice", messageVO);

        }

        // 查询未读消息的数量
        int letterUnreadCount = messageService.countUnreadLetter(user.getId(), null);
        model.addAttribute("letterUnreadCount", letterUnreadCount);

        // 查询未读通知的数量
        int noticeUnreadCount = messageService.countUnreadNotice(user.getId(), null);
        model.addAttribute("noticeUnreadCount", noticeUnreadCount);

        return "/site/notice";
    }

    @RequestMapping(path = "/notice/detail/{topic}", method = RequestMethod.GET)
    public String getNoticeDetail(@PathVariable("topic") String topic, Page page, Model model) {
        User user = hostHolder.getUser();

        page.setLimit(5);
        page.setRows(messageService.countNotice(user.getId(), topic));
        page.setPath("/notice/detail/" + topic);

        List<Message> noticeList = messageService.listNotices(user.getId(), topic, page.getOffset(), page.getLimit());
        List<Map<String, Object>> noticeVoList = new ArrayList<>();
        if (noticeList != null) {
            for (Message notice : noticeList) {
                Map<String, Object> map = new HashMap<>();

                // 通知
                map.put("notice", notice);
                // 通知内容  因为content是json格式，所以需要转换一下
                String content = HtmlUtils.htmlUnescape(notice.getContent());
                Map<String, Object> data = JSONObject.parseObject(content, HashMap.class);

                map.put("user", userService.getUserById((Integer) data.get("userId")));
                map.put("entityType", data.get("entityType"));
                map.put("entityId", data.get("entityId"));
                map.put("postId", data.get("postId"));

                // 通知的作者
                map.put("fromUser", userService.getUserById(notice.getFromId()));

                noticeVoList.add(map);
            }
        }
        model.addAttribute("notices", noticeVoList);

        // 设置已读
        List<Integer> ids = getUnreadLetterIds(noticeList);
        if (!ids.isEmpty()) {
            messageService.updateReadMessage(ids);
        }

        return "/site/notice-detail";
    }
}
