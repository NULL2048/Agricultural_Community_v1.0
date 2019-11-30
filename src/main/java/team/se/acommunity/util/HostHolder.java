package team.se.acommunity.util;

import org.springframework.stereotype.Component;
import team.se.acommunity.entity.User;

/**
 * 持有用户信息，用于代替session对象
 * 服务器在处理每一个用户发来的请求是一对多的关系，它是通过多线程来处理请求的，每一个请求就是一个线程
 * 这个工具类就是将某个数据存进去去，使它在本次请求(这个线程开始到这个线程结束)期间一直存在
 */
@Component
public class HostHolder {
    /**
     * 可以查看这个类的源码，他是先取出当前线程，然后将数据存储到当前线程中去，数据的生命周期就是这个线程的生命周期
     * 数据是被存到当前线程的map中
     */
    private ThreadLocal<User> users = new ThreadLocal<>();

    public void setUser(User user) {
        users.set(user);
    }

    public User getUser() {
        return users.get();
    }

    public void clear() {
        users.remove();
    }
}
