package team.se.acommunity.controller.advice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import team.se.acommunity.util.CommunityUtil;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

// 这个ControllerAdvice注解表示的是这个类是controller的全职配置类，里面的属性annotations如果表示的是要扫描哪些bean，后面 写了controller.class就会扫描所有加了controller注解的bean
@ControllerAdvice(annotations = Controller.class)
public class ExceptionAdvice {
    private static final Logger logger = LoggerFactory.getLogger(ExceptionAdvice.class);

    // 下面这个注释是controller出现异常之后被调用，用来处理异常，括号里面写要处理哪些异常，这个Exception是所有异常的父类，所以这里就表示要处理所有异常
    @ExceptionHandler({Exception.class})
    // 方法必须是共有的并且没有返回值,方法名随意写， e对象用来接收异常类型对象，一般常用的参数就是下面三个
    public void handleException(Exception e, HttpServletRequest request, HttpServletResponse response) throws IOException {
        // 这个是将异常信息存入到日志当中，e.getMessage方法获得的是异常总总的信息，也就是平时报错的Cause By里面的内容，即一段异常最上面的那一句话
        logger.error("服务器发生异常：" + e.getMessage());

        // e.getStackTrace方法是用来获得一个存放着异常所有详细信息的数组，也就是Cause By后面那些比较详细的多条异常信息
        for (StackTraceElement element : e.getStackTrace()) {
            // 异常类型为StackTraceElement，将其转化成toString存入到日志中
            logger.error(element.toString());
        }

        // 因为异常有可能是普通请求的异常，那样直接重定向到500页面就行，但是也有可能是异步请求的异常，那样就需要返回一个json串，所以在这里需要一个判断
        // 通过request在请求头中取得请求类型
        String xRequestedWith = request.getHeader("x-requested-with");
        // XMLHttpRequest表示这个请求希望用XML响应，只有异步请求才可以用XML或者JSON或者普通字符串响应，所以这个就表示这是一个异步请求
        if ("XMLHttpRequest".equals(xRequestedWith)) {
            // 这里要设置响应内容的类型，下面这个表示/plain返回一个普通的字符串，字符串可以是json,xml等格式，在浏览去需要人为将普通字符串转正相应的数据
            // 因为所有的异步请求服务器都返回的是普通字符串，所以这里也返回普通字符串
            response.setContentType("application/plain;charset=utf-8");
            // 下面这个表示的是返回json格式，这样浏览器会自己把数据转换成Json
            // response.setContentType("application/json");
            // 获得response的输出流，用来向浏览器发送json数据
            PrintWriter writer = response.getWriter();
            // 向这个write输出流输入json字符串，写入writer对象之后，它会跟着response响应给浏览器，因为writer得到的也是在response中输出流的地址，所以对writer操作就是对response操作
            writer.write(CommunityUtil.getJSONString(1, "服务器异常!"));
        } else { // request.getContextPath()通过request的这个方法可以获得项目访问路径
            // 普通的请求一场直接重定向给异常页面
            response.sendRedirect(request.getContextPath() + "/error");
        }
    }
}
