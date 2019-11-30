package team.se.acommunity.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.util.Date;

// 下面四个属性分别是索引，类型，分片，副本
// 含义就是spring在做es整合的时候将标识了document标注的类中的数据自动映射到es服务器中
// 就需要设置这个数据要映射到哪个索引上，它的类型是什么，要创建多少个分片，创建多少个副本
// 本次调用api的时候他就会根据配置去将数据进行映射，如果设置的索引，分片等不存在，他就会自动创建
// 因为es6将类型已经弱化了，所有的类型就固定为_doc
@Document(indexName = "discusspost", type = "_doc", shards = 6, replicas = 3)
public class DiscussPost {
    // 配置完了上面的Document之后系统就知道了这个实体类和discusspost这个索引存在对应关系，下面就需要配置实体类中的属性与es的对应关系
    // 要把这个实体类中的每一个字段和es中索引中的字段创建关联

    // 主键一般就加id注解
    @Id
    private int id;

    // 普通的字段就这样设置就行
    @Field(type = FieldType.Integer)
    private int userId;

    // 这个tile虽然不是主键，但是在搜索的时候他是一个关键的字段，所以要特殊配置
    // analyzer是存储解析器   searchAnalyzer是搜索解析器
    // 打个比方，比如文章标题是互联网校招，在将这篇文章存入es服务器的时候要先给他分词，分处尽可能多的关键词，这样用户在搜索的时候才能更容易搜到这篇文章
    // 比如这个题目就可以被分成 互联网，联网，校招，网校等等这些词汇，这个功能就是有咱们之前下载的中文分词器完成，ik_max_word这个就是ik分词器中的其中一个，他叫最大分词器，他能尽可能多的分出更多词汇
    // 再有就是用户搜索的时候，他希望能根据用户想要搜索内容的含义来进行搜索，搜的时候就不会把联网这种完全不相干的内容找出来。这时就需要有一个只能分词器，来将用户想要搜索的信息的分词划分出来，这就需要一个只能分刺激ik_smart
    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String title;

    // content同理
    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String content;

    @Field(type = FieldType.Integer)
    private int type;

    @Field(type = FieldType.Integer)
    private int status;

    @Field(type = FieldType.Date)
    private Date createTime;

    @Field(type = FieldType.Integer)
    private int commentCount;

    @Field(type = FieldType.Double)
    private double score;

    @Override
    public String toString() {
        return "DiscussPost{" +
                "id=" + id +
                ", userId=" + userId +
                ", title='" + title + '\'' +
                ", content='" + content + '\'' +
                ", type=" + type +
                ", status=" + status +
                ", createTime=" + createTime +
                ", commentCount=" + commentCount +
                ", score=" + score +
                '}';
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public int getCommentCount() {
        return commentCount;
    }

    public void setCommentCount(int commentCount) {
        this.commentCount = commentCount;
    }

    public double getScore() {
        return score;
    }

    public void setScore(double score) {
        this.score = score;
    }
}
