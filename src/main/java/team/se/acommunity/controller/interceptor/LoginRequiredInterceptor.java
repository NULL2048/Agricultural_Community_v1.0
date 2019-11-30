package team.se.acommunity.controller.interceptor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import team.se.acommunity.annotation.LoginRequired;
import team.se.acommunity.util.CommunityUtil;
import team.se.acommunity.util.HostHolder;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

@Component
public class LoginRequiredInterceptor implements HandlerInterceptor {
    @Autowired
    private HostHolder hostHolder;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // HandlerMethod是spring MVC中提供的一个类型，表示如果handler拦截的是一个方法，那么他的类型就应该是HandlerMethod
        if (handler instanceof HandlerMethod) {
            HandlerMethod handlerMethod = (HandlerMethod) handler;
            // 获得要访问的方法对象
            Method method = handlerMethod.getMethod();
            // 获得这个方法的指定注解
            LoginRequired loginRequired = method.getAnnotation(LoginRequired.class);
            // 取到了loginRequired表明访问这个方法需要登陆，但是hostHolder取不到User说明没有登陆，这就说明出现了问题
            if (loginRequired != null && hostHolder.getUser() == null) {
                // 因为请求有两种，一种是普通请求，一种是异步请求，异步请求需要返回json数据回去，所以要在在这里判断一下请求是不是异步请求，这样判断的原因在项目笔记里面有
                if ("XMLHttpRequest".equals(request.getHeader("X-Requested-With"))) {
                    // 因为这里是异步请求，所以就不能跳转页面了，要返回JSON数据，就跟ajax一样，在浏览器端它会将这段ajax数据给解析出来，状态码0表示相应成功
                    String ajaxResp = CommunityUtil.getJSONString(1, "请登陆账号");

                    // 服务器响应异步请求，响应数据是通过response传回去的
                    // 这里先设置一下response的编码格式，但是下面的setContentType已经设置了charset，跟着一步重复了，这一步也可以不写
                    response.setCharacterEncoding("UTF-8");
                    // 下面这个是告诉浏览器response里面放的是json数据，但是咱们写的js接收服务器响应数据的时候是按response里面的数据是普通字符串来处理他，js里面写了一个将json格式的字符串转换成js对象的方法，所以这里要设置response响应数据是普通字符串类型
                    //response.setContentType("application/json; charset=utf-8");

                    // 这一句就是设置response里面的数据是普通字符串，如果不写这一句默认就是普通字符串
                    response.setContentType("text/plain; charset=utf-8");
                    // 将相应的json数据存入response的相应正文当中
                    response.getWriter().write(ajaxResp);
                    return false;
                } else {
                    response.sendRedirect(request.getContextPath() + "/login");
                    return false;
                }
            }
        }
        return true;
    }
}
