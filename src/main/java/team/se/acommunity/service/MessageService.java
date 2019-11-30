package team.se.acommunity.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;
import team.se.acommunity.dao.MessageMapper;
import team.se.acommunity.entity.Message;
import team.se.acommunity.util.SensitiveFilter;

import java.util.List;

@Service
public class MessageService {
    @Autowired
    private MessageMapper messageMapper;
    @Autowired
    private SensitiveFilter sensitiveFilter;

    public List<Message> listConversations(int userId, int offset, int limit) {
        return messageMapper.listConversations(userId, offset, limit);
    }

    public int countConversation(int userId) {
        return messageMapper.countConversation(userId);
    }

    public List<Message> listLetters(String conversationId, int offset, int limit) {
        return messageMapper.listLetters(conversationId, offset, limit);
    }

    public int countLetter(String conversationId) {
        return messageMapper.countLetter(conversationId);
    }

    public int countUnreadLetter(int userId, String conversationId) {
        return messageMapper.countUnreadLetter(userId, conversationId);
    }

    public int saveMessage(Message message) {
        // 过滤HTML标签
        message.setContent(HtmlUtils.htmlEscape(message.getContent()));
        // 过滤敏感词
        message.setContent(sensitiveFilter.filter(message.getContent()));

        return messageMapper.insertMessage(message);
    }

    public int updateReadMessage(List<Integer> ids) {
        // 将消息状态改为已读
        return messageMapper.updateStatus(ids, 1);
    }

    public Message getLastedNotice(int userId, String topic) {
        return messageMapper.getLastedNotice(userId, topic);
    }

    public int countNotice(int userId, String topic) {
        return messageMapper.countNotice(userId, topic);
    }

    public int countUnreadNotice(int userId, String topic) {
        return messageMapper.countUnreadNotice(userId, topic);
    }

    public List<Message> listNotices(int userId, String topic, int offset, int limit) {
        return messageMapper.listNotices(userId, topic, offset, limit);
    }
}
