package team.se.acommunity.controller.interceptor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import team.se.acommunity.entity.User;
import team.se.acommunity.service.DataService;
import team.se.acommunity.util.HostHolder;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
public class DataInterceptor implements HandlerInterceptor {
    @Autowired
    private DataService dataService;
    @Autowired
    private HostHolder hostHolder;

    // 在请求之初进行统计
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 统计UV
        // 要获得发起请求的ip，可以直接通过request.getRemoteHost()这个方法得到
        String ip = request.getRemoteHost();
        dataService.recordUV(ip);

        // 统计DAU
        User user = hostHolder.getUser();
        // 用户登录了才统计DAU
        if (user != null) {
            dataService.recordDAU(user.getId());
        }
        return true;
    }
}
