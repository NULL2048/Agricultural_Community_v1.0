package team.se.acommunity.event;

import com.alibaba.fastjson.JSONObject;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.PutObjectRequest;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.omg.SendingContext.RunTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.security.core.parameters.P;
import org.springframework.stereotype.Component;
import team.se.acommunity.entity.DiscussPost;
import team.se.acommunity.entity.Event;
import team.se.acommunity.entity.Message;
import team.se.acommunity.service.DiscussPostService;
import team.se.acommunity.service.ElasticsearchService;
import team.se.acommunity.service.MessageService;
import team.se.acommunity.util.CommunityConstant;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;

@Component
public class EventConsumer implements CommunityConstant {
    private static final Logger logger = LoggerFactory.getLogger(EventConsumer.class);

    @Autowired
    private MessageService messageService;

    @Autowired
    private DiscussPostService discussPostService;

    @Autowired
    private ElasticsearchService elasticsearchService;

    // 注入要执行命令的所在目录
    @Value("${wk.image.command}")
    private String wkImageCommand;
    // 注入图片保存路径
    @Value("${wk.image.storage}")
    private String wkImageStorage;

    // 注入用户密钥
    @Value("${aliyun.key.access}")
    private String accessKey;

    // 注入加密密钥
    @Value("${aliyun.key.secret}")
    private String secretKey;

    // 注入存储空间的名字
    @Value("${aliyun.bucket.share.name}")
    private String shareBucketName;

    // 注入存储空间的访问路径
    @Value("${aliyun.bucket.share.url}")
    private String shareBucketUrl;

    // 注入定时任务线程池
    @Autowired
    private ThreadPoolTaskScheduler taskScheduler;

    // 一个方法可以消费多个主题，一个主题也可以被多个方法消费
    // 下面这个注解就表示这个方法要消费这三个主题  参数中的ConsumerRecord固定写法，被动接受的消息都在这个对象中的value里
    @KafkaListener(topics = {TOPIC_COMMENT, TOPIC_LIKE, TOPIC_FOLLOW})
    public void handleCommentMessage(ConsumerRecord record) {
        if (record == null || record.value() == null) {
            logger.error("消息的内容为空！");
            return;
        }

        // 将json字符串转换为原有的对象，因为json字符串只是为了方便传输，提高传输效率用的
        Event event = JSONObject.parseObject(record.value().toString(), Event.class);

        if (event == null) {
            logger.error("消息格式错误");
            return;
        }

        // 发送站内通知  消费者需要将envent事件中的消息数据取出来，封装成message对象然后再进行消费
        Message message = new Message();
        // 系统通知，所以发送者都是这个
        message.setFromId(SYSTEM_USER_ID);
        // 接收者者就是事件对象中的实体用户id
        message.setToId(event.getEntityUserId());
        // conversationId在系统消息中直接就填这中类型的消息所在主题就可以
        message.setConversationId(event.getTopic());
        message.setCreateTime(new Date());

        // 将event相关的其他数据存入到message的content字段中
        Map<String, Object> content = new HashMap<>();
        // 将这个事件是谁触发的存入map
        content.put("userId", event.getUserId());
        content.put("entityType", event.getEntityType());
        content.put("entityId", event.getEntityId());

        // 判断一下事件有没有额外的数据，如果有就添加进来
        if (!event.getData().isEmpty()) {
            for (Map.Entry<String, Object> entry : event.getData().entrySet()) {
                content.put(entry.getKey(), entry.getValue());
            }
        }
        // 将content的内容转换成json字符串放到message的content字段中
        message.setContent(JSONObject.toJSONString(content));

        // 将消息保存到mysql数据库中
        messageService.saveMessage(message);
    }

    // 消费发帖事件
    @KafkaListener(topics = {TOPIC_PUBLISH})
    public void handlePublishMessage(ConsumerRecord record) {
        if (record == null || record.value() == null) {
            logger.error("消息的内容为空！");
            return;
        }

        // 将json字符串转换为原有的对象，因为json字符串只是为了方便传输，提高传输效率用的
        Event event = JSONObject.parseObject(record.value().toString(), Event.class);

        if (event == null) {
            logger.error("消息格式错误");
            return;
        }

        // 将这个帖子找到
        DiscussPost post = discussPostService.getDiscussPostById(event.getEntityId());
        // 存入es服务器当中
        elasticsearchService.saveDiscussPost(post);
    }

    // 消费删帖事件
    @KafkaListener(topics = {TOPIC_DELETE})
    public void handleDeleteMessage(ConsumerRecord record) {
        if (record == null || record.value() == null) {
            logger.error("消息的内容为空！");
            return;
        }

        // 将json字符串转换为原有的对象，因为json字符串只是为了方便传输，提高传输效率用的
        Event event = JSONObject.parseObject(record.value().toString(), Event.class);

        if (event == null) {
            logger.error("消息格式错误");
            return;
        }

        // 从es中删除
        elasticsearchService.removeDiscussPost(event.getEntityId());
    }

    // 消费分享事件
    @KafkaListener(topics = TOPIC_SHARE)
    public void handleShareMessage(ConsumerRecord record) {
        if (record == null || record.value() == null) {
            logger.error("消息的内容为空！");
            return;
        }

        // 将json字符串转换为原有的对象，因为json字符串只是为了方便传输，提高传输效率用的
        Event event = JSONObject.parseObject(record.value().toString(), Event.class);

        if (event == null) {
            logger.error("消息格式错误");
            return;
        }

        String htmlUrl = (String) event.getData().get("htmlUrl");
        String fileName = (String) event.getData().get("fileName");
        String suffix = (String) event.getData().get("suffix");

        // quality加了这个参数就是对生成的图片压缩，压缩率75是一个比较合适的值，既能保证质量还不错，又能大大压缩图片大小，减轻服务器压力
        String cmd = wkImageCommand + " --quality 75 "
                + htmlUrl + " " + wkImageStorage + "/" + fileName + suffix;

        try {
            Runtime.getRuntime().exec(cmd);
            logger.info("生成长图成功：" + cmd);
        } catch (IOException e) {
            logger.error("生成长图失败：" + e.getMessage());
        }

        // 之所以要用ThreadPoolTaskScheduler是因为之前说过Runtime.getRuntime().exec(cmd);这个方法执行是异步的，调用完了之后这边的代码就接着向下执行了
        // 就会出现执行到这里的时候长图还没有生成，就会导致获取不到长度，但是如果将代码在这个位置阻塞，那么其他的进程也就得不到他的资源，这样效率极低
        // 所以这里就用到了定时线程池，在这里设置过1秒就检查一下有没有生成长图，如果过了30秒还没有生成，就认定生成长图失败
        // 之所以这里不用Quartz,因为这里不会出现之前讲的那种分布式冲突问题，因为这里是写在消费者中的，消费者的机制就是抢占式的，每一个服务器的消费者谁抢到了这个消息，谁就去执行，其他的消费者是不会去消费这个消息的，所以不会出现之前分布式冲突抢占的问题
        // 之前那个是因为服务已启动，就去执行定时调度计算文章分数，所以才会出现这种问题

        // 启动定时器，监视改图片，一旦生成了图片就上传至阿里云
        UploadTask uploadTask = new UploadTask(fileName, suffix);
        // 每隔半秒钟检查一次图片是否存在
        Future future = taskScheduler.scheduleAtFixedRate(uploadTask, 500);
        // 趁着500毫秒的间隔将返回的future对象放入uploadTask对象，用来终止定时任务。这个对象中包含了任务的执行情况信息
        uploadTask.setFuture(future);
    }

    // 因为上传图片的过程比较麻烦，单独创一个内部线程
    class UploadTask implements Runnable {
        // 文件名
        private String fileName;
        // 文件后缀
        private String suffix;
        // 启动任务的返回值，启动任务是就会返回这个对象，它可以用来停止定时器。
        private Future future;
        // 开始时间
        private long startTime;
        // 上传次数
        private int uploadTimes;

        public UploadTask(String fileName, String suffix) {
            this.fileName = fileName;
            this.suffix = suffix;
            this.startTime = System.currentTimeMillis();
        }

        public void setFuture(Future future) {
            this.future = future;
        }

        @Override
        public void run() {
            // 生成长图失败
            if (System.currentTimeMillis() - startTime > 30000) {
                // 当超过三十秒还没有上传图片成功，就终止操作
                logger.error("执行时间过长，终止任务：" + fileName);
                // 终止定时器任务
                future.cancel(true);
                return;
            }

            // 上传失败
            if (uploadTimes > 3) {
                // 当上传大于三次还没有成功，就可能是网络问题或者是服务器问题，就直接终止定时任务
                logger.error("上传次数过多，终止任务：" + fileName);
                future.cancel(true);
                return;
            }

            // 在本地存放生成长图的路径
            String path = wkImageStorage + "/" + fileName + suffix;
            File file = new File(path);
            // 如果文件存在说明长图已经生成完毕
            if (file.exists()) {
                logger.info(String.format("开始第%d次上传[%s].", ++uploadTimes, fileName));

                // 这个就是OSS服务器的所在区域地址
                String endpoint = "http://oss-cn-beijing.aliyuncs.com";
                // 创建OSSClient实例。
                OSS ossClient = new OSSClientBuilder().build(endpoint, accessKey, secretKey);

                // 创建PutObjectRequest对象。
                PutObjectRequest putObjectRequest = new PutObjectRequest("acommunity-share", fileName, file);

                // 上传文件。
                ossClient.putObject(putObjectRequest);

                // 关闭OSSClient。
                ossClient.shutdown();
            } else {
                logger.info("等待图片生成[" + fileName + "]");
            }
        }
    }
}
