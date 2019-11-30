package team.se.acommunity.controller.interceptor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;
import team.se.acommunity.entity.User;
import team.se.acommunity.service.MessageService;
import team.se.acommunity.util.HostHolder;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
public class MessageInterceptor implements HandlerInterceptor {
    @Autowired
    private HostHolder hostHolder;

    @Autowired
    private MessageService messageService;

    // 因为所有的页面导航栏都需要显示未读消息数，但是未读消息数的Mode只有在点击查看私心的时候才会被传到界面，所以就需要写一个拦截器来实现所有的请求都会返回未读消息数的model
    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
        User user = hostHolder.getUser();
        if (user != null && modelAndView != null) {
            int letterUnreadCount = messageService.countUnreadLetter(user.getId(), null);
            int noticeUnreadCount = messageService.countUnreadNotice(user.getId(), null);
            modelAndView.addObject("allUnreadCount", letterUnreadCount + noticeUnreadCount);
        }
    }
}
