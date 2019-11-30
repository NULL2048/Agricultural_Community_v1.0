package team.se.acommunity;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
@ContextConfiguration(classes = AgriculturalCommunityApplication.class)
public class KafkaTests {
    @Autowired
    private KafkaProducer kafkaProducer;


    // 运行之前必须在命令行启动了kafka才行
    @Test
    public void testKafka() {
        kafkaProducer.sendMessage("test", "nihao");
        kafkaProducer.sendMessage("test", "zaima");
        // 不用手动启动消费者，kafka会自动让消费者监听消息的
        // 也就是说生产者发消息是主动去调的，消费者接收消息是被动接受的
        try {
            Thread.sleep(1000 * 10);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

// 将这个生产者装配进spring容器
@Component
class KafkaProducer {
    // kafka的操作类已经内置在spring容器中了，直接注入进来
    @Autowired
    private KafkaTemplate kafkaTemplate;

    /**
     * 发消息
     * @param topic 消息的主题
     * @param content 消息的具体内容
     */
    public void sendMessage(String topic, String content) {
        kafkaTemplate.send(topic, content);
    }
}

@Component
class KafkaConsumer {
    // 设置生产者要监听的主题   启动kafka这个生产者进程就会一直监听指定的主题是否有消息传入，没有消息的话这个进程就是阻塞状态，有消息的话他就会被唤醒读取消息
    @KafkaListener(topics = {"test"})
    // 消费者接收到消息之后会自动将消息封装到ConsumerRecord这个类中，传到handleMessage这个方法中
    public void handleMessage(ConsumerRecord record) {
        System.out.println(record.value());
    }
}
