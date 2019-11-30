package team.se.acommunity.event;

import com.alibaba.fastjson.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import team.se.acommunity.entity.Event;

@Component
public class EventProducer {
    @Autowired
    private KafkaTemplate kafkaTemplate;

    // 处理事件
    public void fireEvent(Event event) {
        // 将事件发布到指定主题   第二个参数应该传输这个事件对象的全部内容，而json格式的数据更方便传入，而且传输效率更好，所以将这个对象转换为json字符串
        kafkaTemplate.send(event.getTopic(), JSONObject.toJSONString(event));
    }
}
