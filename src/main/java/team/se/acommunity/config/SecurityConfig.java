package team.se.acommunity.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import team.se.acommunity.util.CommunityConstant;
import team.se.acommunity.util.CommunityUtil;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

@Configuration
public class SecurityConfig extends WebSecurityConfigurerAdapter implements CommunityConstant {
    // 重新覆写两个configure方法,我们只配置授权，不配置认证，认证他会走我们自己写好的逻辑，只有授权操作才会走security

    @Override
    public void configure(WebSecurity web) throws Exception {
        // 所有resources目录下的静态资源都可以访问
        web.ignoring().antMatchers("/resources/**");
    }

    // 因为之前已经写过登录操作了，所以这里直接用以前写好的认证操作就可以了，不用security进行认证操作了

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        // 授权
        http.authorizeRequests()
                .antMatchers(   // 下面这两段的含义就是antMatchers中的这些访问路径，只要是有了hasAnyAuthority中的任意一个权限，就可以访问
                        "/user/setting",
                        "/user/upload",
                        "/discuss/add",
                        "/comment/add/**",
                        "/letter/**",
                        "/notice/**",
                        "/like",
                        "/follow",
                        "/unfollow",
                        "/user/updatepwd"
                )
                .hasAnyAuthority(
                        AUTHORITY_ADMIN,
                        AUTHORITY_USER,
                        AUTHORITY_MODERATOR
                )
                .antMatchers( // 配置版主权限
                        "/discuss/top",
                        "/discuss/wonderful"
                )
                .hasAnyAuthority(
                        AUTHORITY_MODERATOR
                )
                .antMatchers( // 设置管理员权限
                        "/discuss/delete",
                        "/data/**",
                        "/actuator/**"    // 将断电路径设置访问权限，只能让管理员访问，不能谁都可以访问，不安全
                )
                .hasAnyAuthority(
                        AUTHORITY_ADMIN
                )
                .anyRequest().permitAll() // 除了上面的那些请求之外，其他的请求全部允许
                .and().csrf().disable(); // 关掉security默认开启的自动生成csrf凭证，这样就不用把所有的页面重新翻新了，但实际是开了更好

        // 权限不够时的处理
        http.exceptionHandling()
                .authenticationEntryPoint(new AuthenticationEntryPoint() { // 没有登陆时的处理方案
                    @Override
                    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException e) throws IOException, ServletException {
                        String xRequestedWith = request.getHeader("x-requested-with"); // 从请求头中取到这个x-requested-with的值，就能判断当前请求时同步请求还是异步请求
                        if ("XMLHttpRequest".equals(xRequestedWith)) { // 如果前端期望你返回一个XMLHttp的请求，说明这是一个异步请求
                            // 设置响应数据编码格式和字符集
                            response.setContentType("application/plain;charset=utf-8");
                            PrintWriter writer = response.getWriter();
                            // 一般没有登陆的状态码就是403
                            writer.write(CommunityUtil.getJSONString(403, "你还没有登陆哦！"));
                        } else {
                            // 不是异步请求直接重定向
                            response.sendRedirect(request.getContextPath() + "/login");
                        }
                    }
                })
                .accessDeniedHandler(new AccessDeniedHandler() { // 登陆了但是权限不够时的处理方案
                    @Override
                    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException e) throws IOException, ServletException {
                        String xRequestedWith = request.getHeader("x-requested-with"); // 从请求头中取到这个x-requested-with的值，就能判断当前请求时同步请求还是异步请求
                        if ("XMLHttpRequest".equals(xRequestedWith)) { // 如果前端期望你返回一个XMLHttp的请求，说明这是一个异步请求
                            // 设置响应数据编码格式和字符集
                            response.setContentType("application/plain;charset=utf-8");
                            PrintWriter writer = response.getWriter();
                            // 一般没有登陆的状态码就是403
                            writer.write(CommunityUtil.getJSONString(403, "你没有访问此功能的权限"));
                        } else {
                            // 不是异步请求直接重定向   这个路径是跳转到404界面
                            response.sendRedirect(request.getContextPath() + "/denied");
                        }
                    }
                });

        // Security底层默认会拦截/logout请求，进行退出处理
        // 因为项目之前已经写过了退出处理，所以这里我们覆盖它的默认逻辑，这样才能执行我们自己的退出代码
        http.logout().logoutUrl("/securitylogout");
    }
}
