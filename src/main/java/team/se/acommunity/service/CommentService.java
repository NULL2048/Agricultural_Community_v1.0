package team.se.acommunity.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.HtmlUtils;
import team.se.acommunity.dao.CommentMapper;
import team.se.acommunity.entity.Comment;
import team.se.acommunity.util.CommunityConstant;
import team.se.acommunity.util.SensitiveFilter;

import java.util.List;

@Service
public class CommentService implements CommunityConstant {
    @Autowired
    private CommentMapper commentMapper;
    @Autowired
    private SensitiveFilter sensitiveFilter;
    @Autowired
    private DiscussPostService discussPostService;

    public List<Comment> listCommentsByEntity(int entityType, int entityId, int offset, int limit) {
        return commentMapper.listCommentsByEntity(entityType, entityId, offset, limit);
    }

    public int countCommentsByEntity(int entityType, int entityId) {
        return commentMapper.countCommentsByEntity(entityType, entityId);
    }

    /**
     * 这个方法里面涉及到了两次DML操作（也就是对数据库中的数据的操作），所以这个方法要引入事务管理
     * 这个方法有两个mapper操作，只要是有报错异常的地方，两个事务就会全部回滚
     * 添加评论并更新评论数量，在数据库里面每一篇文章都有一个字段记录着这篇文章的评论数
     * @param comment
     * @return
     */
    @Transactional(isolation = Isolation.READ_COMMITTED, propagation = Propagation.REQUIRED)
    public int saveComment(Comment comment) {
        if (comment == null) {
            throw new IllegalArgumentException("参数不能为空！");
        }
        // 添加评论
        // 先过滤一下HTML标签，将发表内容中的HTML标签转译成普通字符，否则浏览器会把它认定为HTML标键进行渲染，会造成界面的错乱
        comment.setContent(HtmlUtils.htmlEscape(comment.getContent()));
        // 过滤敏感词汇
        comment.setContent(sensitiveFilter.filter(comment.getContent()));

        int rows = commentMapper.insertComment(comment);


        // 更新评论数量
        // 只有文章才会统计评论数量
        if (comment.getEntityType() == ENTITY_TYPE_POST) {
            int count = commentMapper.countCommentsByEntity(comment.getEntityType(), comment.getEntityId());
            discussPostService.updateCommentCount(comment.getEntityId(), count);
        }

        return rows;
    }

    public Comment getCommentById(int id) {
        return commentMapper.getCommentById(id);
    }

    public List<Comment> listCommentsByUserId(int userId, int entityType, int offset, int limit) {
        return commentMapper.listCommentsByUserId(userId, entityType, offset, limit);
    }

    public int countCommentByUserId(int userId, int entityType) {
        return commentMapper.countCommentByUserId(userId, entityType);
    }
}
