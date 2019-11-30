package team.se.acommunity.util;

import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.DigestUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CommunityUtil {
    // 生成随机字符串
    public static String generateUUID() {
        // UUID是一个系统提供的生成所及字符串的对象，因为里面有很多-，所以我们就要用replace方法将-都给去掉
        return UUID.randomUUID().toString().replace("-", "");
    }

    // MD5加密
    // hello -> abc123f456
    // 这个MD5加密有局限性，就是所有的字符串加密之后得到的密文都是固定的，不是动态密码，所以用户如果设置了一个很简单的密码就很容易破译，因为黑客都有一些简单单词库，都可以暴力破解
    // 所以我们数据库有一个salt字段，就是将一些复杂随机的字符串拼接到用户设置的密码，这样MD5加密的密文就不容易被破译了
    public static String md5(String key) {
        // 这个方法当key指向null,空字符串，空格他都会判定这个key字符串是空的，都会返回true
        if (StringUtils.isBlank(key)) {
            return null;
        }
        // 这个实现MD5加密也是这个包里自带的工具类，这个方法要求传入的参数是二进制数据，所以要使用getBytes转换一下
        return DigestUtils.md5DigestAsHex(key.getBytes());
    }

    public static String getJSONString(int code, String msg, Map<String, Object> map) {
        JSONObject json = new JSONObject();
        json.put("code", code);
        json.put("msg", msg);
        if (map != null) {
            for (String key : map.keySet()) {
                json.put(key, map.get(key));
            }
        }
        // 返回json格式的字符串，根据json对象返回其对应的字符串
        return json.toJSONString();
    }

    public static String getJSONString(int code, String msg) {
        return getJSONString(code, msg, null);
    }

    public static String getJSONString(int code) {
        return getJSONString(code, null, null);
    }

    public static void main(String[] args) {
        Map<String, Object> map = new HashMap<>();
        map.put("name", 12);
        System.out.println(getJSONString(0, "ok", map));
    }
}
