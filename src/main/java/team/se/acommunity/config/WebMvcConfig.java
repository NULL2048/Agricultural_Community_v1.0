package team.se.acommunity.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import team.se.acommunity.controller.interceptor.*;
import team.se.acommunity.service.DataService;

@Configuration
public class WebMvcConfig  implements WebMvcConfigurer {
    // 将拦截器注入
    @Autowired
    private AlphaInterceptor alphaInterceptor;

    @Autowired
    private LoginTicketInterceptor loginTicketInterceptor;

    @Autowired
    private DataInterceptor dataInterceptor;

    // 引入security后就不用以前写的这个权限控制了，把这个拦截器注释掉就行了
//    @Autowired
//    private LoginRequiredInterceptor loginRequiredInterceptor;

    @Autowired
    private MessageInterceptor messageInterceptor;

    /**
     * spring容器会把registry这个对象传给这个方法，然后再将注入的拦截器alphaInterceptor注册进这个registry这个对象就可以了
     * @param registry
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
                 // 将这个alphaInterceptor添加进去   // 这个excludePathPatterns表示有些请求不需要拦截器拦截，后面就写哪些路径不需要被拦截
        registry.addInterceptor(alphaInterceptor)  // 一些静态资源，拦截器是不需要拦截
                .excludePathPatterns("/**/*.css", "/**/*.js", "/**/*.png", "/**/*.jpg", "/**/*.jpeg")// 静态资源都是放在templates中二级目录下，所以直接把所有二级目录的.css和.js全部排除就可以了
                .addPathPatterns("/register", "/login"); // 设置拦截器指定拦截那些路径

        registry.addInterceptor(loginTicketInterceptor) // 不指定拦截器路径他就会所有的路径都拦截
                .excludePathPatterns("/**/*.css", "/**/*.js", "/**/*.png", "/**/*.jpg", "/**/*.jpeg");

//        registry.addInterceptor(loginRequiredInterceptor)
//                .excludePathPatterns("/**/*.css", "/**/*.js", "/**/*.png", "/**/*.jpg", "/**/*.jpeg");

        registry.addInterceptor(messageInterceptor)
                .excludePathPatterns("/**/*.css", "/**/*.js", "/**/*.png", "/**/*.jpg", "/**/*.jpeg");

        registry.addInterceptor(dataInterceptor)
                .excludePathPatterns("/**/*.css", "/**/*.js", "/**/*.png", "/**/*.jpg", "/**/*.jpeg");
    }
}
