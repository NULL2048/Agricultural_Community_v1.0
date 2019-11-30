package team.se.acommunity.controller.interceptor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;
import team.se.acommunity.entity.LoginTicket;
import team.se.acommunity.entity.User;
import team.se.acommunity.service.UserService;
import team.se.acommunity.util.CookieUtil;
import team.se.acommunity.util.HostHolder;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Date;

@Component
public class LoginTicketInterceptor implements HandlerInterceptor {
    @Autowired
    private UserService userService;

    @Autowired
    private HostHolder hostHolder;

    /**
     * 因为登录之后可能随时会用到用户信息，所以要在每一次请求之前就要将User取到，以备后面有可能会用到
     * 而且是每一次请求都会取数据库中取一次数据，取出来的数据只对当前这一次请求有效，这样就保证了如果用户更改了数据库信息，其他位置的信息也会实时的更新，不会出现读取脏数据的情况
     * @param request
     * @param response
     * @param handler
     * @return
     * @throws Exception
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 要获得浏览器端传过来的cookie，cookie是通过request传过来的
        // 获取ticket
        String ticket = CookieUtil.getValue(request, "ticket");

        // 如果取得ticket，说明用户已经登录
        if (ticket != null) {
            // 查询凭证
            LoginTicket loginTicket = userService.getLoginTicket(ticket);
            // 检查凭证是否有效                                                                 // 表示有效截至时间晚于当前时间，就返回true
            if (loginTicket != null && loginTicket.getStatus() == 0 && loginTicket.getExpired().after(new Date())) {
                // 根据凭证查询用户
                User user = userService.getUserById(loginTicket.getUserId());
                // 在本次请求中持有用户
                hostHolder.setUser(user);
                // 构建用户认证的结果，并存入SecurityContext，以便于Security进行授权   用户的权限就存在SecurityContext里
                Authentication authentication = new UsernamePasswordAuthenticationToken( // 这个的配置讲解可以看上次那个demo中的认证样例
                        user, user.getPassword(), userService.getAuthorities(user.getId()));
                // 整个security的流程是这样的，显示进行认证，认证通过之后将用户认证信息，包括用户信息，密码，还有用户所持有的权限保存到SecurityContext
                // 然后在后面如果有的请求需要有权限授权之后，security会取取得存储好的用户认证信息，比对看一下这个用户是否有访问这个请求的权限，如果有就放行，如果没有权限就执行相应的拒绝操作

                // 上面取到的authentication是用户凭证对象，要将凭证存入到SecurityContext中，就要透过SecurityContextHolder.setContext这个方法来实现
                // authentication要存到SecurityContext实例化对象当中，所以要new一个SecurityContextImpl并将authentication传入进行实例化
                SecurityContextHolder.setContext(new SecurityContextImpl(authentication));
            }
        }
        return true;
    }

    /**
     * 将存储在当前线程中的user对象传递给model，这样就可以在界面中使用
     * @param request
     * @param response
     * @param handler
     * @param modelAndView
     * @throws Exception
     */
    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
        User user = hostHolder.getUser();
        if (user != null && modelAndView != null) {
            modelAndView.addObject("loginUser", user);
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        // 本次请求结束后要将数据清理掉，线程是一直都在的，可能下一次这个线程就给了别的用户用了，所以在这个用户使用完这个线程之后就要把自己的数据给清除掉
        hostHolder.clear();

        // 请求结束之后将存在security中的用户凭证清理掉
        SecurityContextHolder.clearContext();
    }
}
