package team.se.acommunity.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import team.se.acommunity.service.DataService;

import java.util.Date;


@Controller
public class DataController {
    @Autowired
    private DataService dataService;

    // 统计页面     下面这样写表示这个路径既可以处理get请求，也可以处理post请求
    @RequestMapping(path = "/data", method = {RequestMethod.GET, RequestMethod.POST})
    public String getDataPage() {
        return "/site/admin/data";
    }

    // 统计网站UV   页面要想统计UV，他是要点一个按钮进行统计，有统计范围数据，实际就是提交了一个表单，所以要用post请求
    @RequestMapping(path = "/data/uv", method = RequestMethod.POST)
    // 因为页面传过来的数据都是字符串数据，而这里的参数是Date型，所以要让字符串自动转化为date要先告诉系统传过来的字符串是什么格式，他再按照这个格式生成对应的Date，就是用了DateTimeFormat注解
    // pattern中写的是传过来的日期字符串的格式
    public String getUV(@DateTimeFormat(pattern = "yyyy-MM-dd") Date start,
                        @DateTimeFormat(pattern = "yyyy-MM-dd") Date end, Model model) {
        // 统计
        long uv = dataService.calculateUV(start, end);
        model.addAttribute("uvResult", uv);
        // 跳转回原界面上应该显示者结果的日期范围，所以还要把这个日期范围传回给界面
        model.addAttribute("uvStartDate", start);
        model.addAttribute("ubEndDate", end);

        // 下面这句话的意思就是将请求转发到/data这个路径的方法，这是一个请求转发，转发的请求是同一个请求，所以上面那个/data要支持两种请求，这个请求是post请求，所以他转发到/data也是一个post请求的格式
        return "forward:/data";
        // 要区分return"/site/admin/data"
        // 上面这个就是单纯的返回这个模板，并不是请求转发
        // 区别就是return"/site/admin/data"就是单纯的返回这个指定路径的模板，如果模板里面需要什么信息直接通过model传过去
        // 但是return "forward:/data"是一个请求转发，它是将请求又转发到了/data这个路径下的方法中，虽然在这里效果是一样的，但是如果/data方法中还有处理逻辑的代码，那就相当于又执行了一份代码，所以这两个本质是不同的
    }

    // 统计活跃用户
    @RequestMapping(path = "/data/dau", method = RequestMethod.POST)
    public String getDAU(@DateTimeFormat(pattern = "yyyy-MM-dd") Date start,
                        @DateTimeFormat(pattern = "yyyy-MM-dd") Date end, Model model) {
        // 统计
        long dau = dataService.calculateDAU(start, end);

        model.addAttribute("dauResult", dau);
        // 跳转回原界面上应该显示者结果的日期范围，所以还要把这个日期范围传回给界面
        model.addAttribute("dauStartDate", start);
        model.addAttribute("dauEndDate", end);

        return "forward:/data";
    }
}
