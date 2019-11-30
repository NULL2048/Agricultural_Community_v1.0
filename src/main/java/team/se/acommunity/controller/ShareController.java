package team.se.acommunity.controller;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import team.se.acommunity.entity.Event;
import team.se.acommunity.event.EventProducer;
import team.se.acommunity.util.CommunityConstant;
import team.se.acommunity.util.CommunityUtil;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

@Controller
public class ShareController implements CommunityConstant {
    private static final Logger logger = LoggerFactory.getLogger(ShareController.class);

    @Autowired
    private EventProducer eventProducer;

    // 注入项目域名
    @Value("${community.path.domain}")
    private String domain;

    // 注入虚拟项目名
    @Value("${server.servlet.context-path}")
    private String contextPath;

    // 注入图片保存路径
    @Value("${wk.image.storage}")
    private String wkImageStorage;

    // 注入存储空间的名字
    @Value("${aliyun.bucket.share.name}")
    private String shareBucketName;

    // 注入存储空间的访问路径
    @Value("${aliyun.bucket.share.url}")
    private String shareBucketUrl;

    /**
     * 分享操作
     * @param htmlUrl 要生成图片的网页的url
     * @return
     */
    @RequestMapping(path="/share", method = RequestMethod.GET)
    @ResponseBody
    public String share(String htmlUrl) {
        // 文件名
        String fileName = CommunityUtil.generateUUID();

        // 通过事件来处理分享请求，这样当分享操作很多的时候就可以大大提高系统效率
        // 如果不同事件处理，当很多人点分享的时候，必须等执行完了分享的所有操作，代码才会继续向下执行，在这期间所有的人都要等着其他人的分享处理操作完成后，才会再自己的界面上显示分享操作
        // 如果使用了事件处理，用户点击了分享之后，会在界面上马上弹出分享成功，但实际上分享业务可能并没有完成呢，因为系统将当前这个分享操作直接作为一个事件丢到了kafka队列当中，去让队列异步的去一个个处理分享操作，如果时间太多就在队列里面等待排队，而调用处当把这个事件丢给kafka后就接着向下执行了
        // 异步生成长图
        Event event = new Event()
                .setTopic(TOPIC_SHARE)
                .setData("htmlUrl", htmlUrl) // 通过键值对的形式来对这个事件存储数据
                .setData("fileName", fileName) // 文件名
                .setData("suffix", ".png"); // 文件后缀
        eventProducer.fireEvent(event);

        // 返回访问路径，这个访问路径就是显示生成的长图
        Map<String, Object> map = new HashMap<>();
        // 将访问路径放在map中传回去  这里的访问路径是阿里云OSS中文件的访问路径
        // map.put("shareUrl", domain + contextPath + "/share/image/" + fileName);
        String url = shareBucketUrl + "/" + shareBucketName + "/" + fileName + ".png";
        map.put("shareUrl", url);

        return CommunityUtil.getJSONString(0, null, map);
    }

    // 这个方法废用，因为数据已经迁移到了阿里云中
    // 获取长图  返回类型void表示不进行任何跳转了，操作完之后自己返回一个response对象
    @RequestMapping(path = "/share/image/{fileName}", method = RequestMethod.GET)
    public void getShareImage(@PathVariable("fileName") String fileName, HttpServletResponse response) {
        if (StringUtils.isBlank(fileName)) {
            throw new IllegalArgumentException("文件名不能为空！");
        }

        // 因为要响应回去一个图片，所以就使用到了response
        // 设置响应数据类型
        response.setContentType("image/png");
        // 根据路径获得这个图片文件对象
        File file = new File(wkImageStorage + "/" + fileName + ".png");
        try {
            // 获得response的输出流
            OutputStream os = response.getOutputStream();
            // 获取文件输入流，将文件对象转换成输入流，用来写入response的输出流中
            FileInputStream fis = new FileInputStream(file);
            // 创建一个缓冲区，用来将输入流输入到输出流中，习惯定位1024的长度
            byte[] buffer = new byte[1024];
            // 下标
            int b = 0;
            // 一点点读取fis中的内容，将读取内容放到buffer中  b返回的是数据再buffer数组中最后位置的下标，如果返回-1说明已经不能从fis中读取数据了，已经读取完毕了
            while ((b = fis.read(buffer)) != -1) {
                // 向os输出流中输入fis文件输入流里的数据
                // 下面这个意思就是通过buffer这个缓冲区将fis中的数据，一点点输入到os当中  后面的是buffer数组中的数据范围，因为有可能最后一个buffer数组不是填满的，所以就要用b来标注一下存放数据最后一个下表位置
                os.write(buffer, 0, b);
            }
        } catch (IOException e) {
            logger.error("获取图片失败：" + e.getMessage());
        }
    }

}
