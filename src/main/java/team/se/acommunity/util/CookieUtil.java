package team.se.acommunity.util;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

/**
 * 因为经常会有从request中取得cookie值的操作，所以在这里直接封装一个方法类，提高代码复用
 */
public class CookieUtil {
    // 设置成静态方法，不用spring容器来管理了，用的时候直接调用
    public static String getValue(HttpServletRequest request, String name) {
        // 判空
        if (request == null || name == null) {
            throw new IllegalArgumentException("参数为空！");
        }

        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals(name)) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }
}
