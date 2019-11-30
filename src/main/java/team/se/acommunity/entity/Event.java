package team.se.acommunity.entity;

import java.util.HashMap;
import java.util.Map;

public class Event {
    // 消息的主题
    private String topic;
    // 触发事件的对象  比如a给b的评论点在，这个userId就得是a
    private int userId;
    // 设置事件是处理的什么类型的实体和具体哪一个实体和这个实体的作者是谁
    private int entityType;
    private int entityId; // 这个id就是a给b点赞的那一条评论的id
    private int entityUserId; // 这个就是b

    // 事件额外的数据，以后如果功能有拓展的时候，就可以把其他相关信息放在这个里面
    private Map<String, Object> data = new HashMap<>();

    public String getTopic() {
        return topic;
    }

    public Event setTopic(String topic) {
        this.topic = topic;
        return this;
    }

    public int getUserId() {
        return userId;
    }

    public Event setUserId(int userId) {
        this.userId = userId;
        return this;
    }

    public int getEntityType() {
        return entityType;
    }

    public Event setEntityType(int entityType) {
        this.entityType = entityType;
        return this;
    }

    public int getEntityId() {
        return entityId;
    }

    public Event setEntityId(int entityId) {
        this.entityId = entityId;
        return this;
    }

    public int getEntityUserId() {
        return entityUserId;
    }

    public Event setEntityUserId(int entityUserId) {
        this.entityUserId = entityUserId;
        return this;
    }

    public Map<String, Object> getData() {
        return data;
    }

    public Event setData(String key, Object value) {
        this.data.put(key, value);
        return this;
    }
}
