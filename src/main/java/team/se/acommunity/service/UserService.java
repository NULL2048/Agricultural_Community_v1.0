package team.se.acommunity.service;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import team.se.acommunity.dao.LoginTicketMapper;
import team.se.acommunity.dao.UserMapper;
import team.se.acommunity.entity.LoginTicket;
import team.se.acommunity.entity.User;
import team.se.acommunity.util.CommunityConstant;
import team.se.acommunity.util.CommunityUtil;
import team.se.acommunity.util.MailClient;
import team.se.acommunity.util.RedisKeyUtil;

import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
public class UserService implements CommunityConstant {
    @Autowired
    private UserMapper userMapper;
    // 注入邮件客户端
    @Autowired
    private MailClient mailClient;
    // 注入模板引擎，用来给邮箱发送HTML网页
    @Autowired
    private TemplateEngine templateEngine;

    @Autowired
    private LoginTicketMapper loginTicketMapper;

    @Autowired
    private RedisTemplate redisTemplate;

    // 将域名给注入
    @Value("${community.path.domain}")
    private String domain;
    // 注入项目名,这个项目名已经在application.properties里面设置过了，已经存在了键值对里，注入进了spring容器，直接通过key来取得值就可以了
    @Value("${server.servlet.context-path}")
    private String contextPath;

    public User getUserById(int id) {
        //return userMapper.getById(id);
        User user = getCache(id);
        if (user == null) {
            user = initCache(id);
        }
        return user;
    }

    public User getUserByEmail(String email) {
        return userMapper.getByEmail(email);
    }

    public int updatePassword(int id, String password) {
        return userMapper.updatePassword(id, password);
    }

    public Map<String, Object> register(User user) {
        Map<String, Object> map = new HashMap<>();

        // 控制判断
        if (user == null) {
            throw new IllegalArgumentException("参数不能为空！");
        }

        if (StringUtils.isBlank(user.getUsername())) {
            map.put("usernameMsg", "账号不能为空！");
            return map;
        }

        if (StringUtils.isBlank(user.getPassword())) {
            map.put("passwordMsg", "密码不能为空！");
            return map;
        }

        if (StringUtils.isBlank(user.getEmail())) {
            map.put("emailMsg", "邮箱不能为空！");
            return map;
        }

        // 验证账号是否已存在
        User u = userMapper.getByName(user.getUsername());
        if (u != null) {
            map.put("usernameMsg", "该账号已存在");
            return map;
        }
        // 验证邮箱是否已注册
        u = userMapper.getByEmail(user.getEmail());
        if (u != null) {
            map.put("emailMsg", "该邮箱已被注册");
            return map;
        }

        // 注册用户
        // 产生随机数将其存入数据库的salt字段中来和密码拼接，取得是5位随机数
        user.setSalt(CommunityUtil.generateUUID().substring(0, 5));
        user.setPassword(CommunityUtil.md5(user.getPassword() + user.getSalt()));
        // 设置用户类型，0是普通用户
        user.setType(0);
        // 设置帐号状态，0是未激活
        user.setStatus(0);
        // 设置这个账号的激活码
        user.setActivationCode(CommunityUtil.generateUUID());
        // 设置这个账号的默认随即头像，String.format这个方法就可以用占位符%d在字符串中占一个位置，然后再通过参数去设置这个字符串的最终内容，这里通过随机数方法来获取0-3的随机数
        user.setHeaderUrl(String.format("https://acommunity-header.oss-cn-beijing.aliyuncs.com/acommunity-header/timg%d.jpg", new Random().nextInt(3)));
        user.setCreateTime(new Date());
        userMapper.insertUser(user);

        // 发送激活邮件
        // 设置要传送的模板内容,这个Context对象是用来存储键值对的
        Context context = new Context();
        context.setVariable("email", user.getEmail());
        // http://localhost:8080/ac/activation/101/code
        // 将要访问的url也动态写出来
        String url = domain + contextPath + "/activation/" + user.getId() + "/" + user.getActivationCode();
        context.setVariable("url", url);
        // content才是真正要通过邮件发送出去的内容  通过模板引擎对象将context键值对传送给HTML静态模板，并且解析出来给context字符串
        String content = templateEngine.process("/mail/activation", context);
        // 将content字符串通过邮箱发送
        mailClient.sendMail(user.getEmail(), "惠农网激活账号", content);
        // 返回的map是空的就表示注册成功
        return map;
    }

    /**
     * 校验验证码激活账号
     * @param userId
     * @param code
     * @return
     */
    public int activation(int userId, String code) {
        User user = userMapper.getById(userId);
        if (user.getStatus() == 1) {
            return ACTIVATION_REPEAT;
        } else if (user.getActivationCode().equals(code)) {
            userMapper.updateStatus(userId, 1);
            // 因为对用户信息有修改，所以要清除redis数据库中的user信息
            clearCache(userId);
            return ACTIVATION_SUCCESS;
        } else {
            return ACTIVATION_FAILURE;
        }
    }

    /**
     * 登录业务层
     * @param username 登录账号
     * @param password 登陆密码
     * @param expiredSeconds 生成登陆凭证有效时间
     * @return
     */
    public Map<String, Object> login(String username, String password, int expiredSeconds) {
        Map<String, Object> map = new HashMap<>();

        // 空值处理
        if (StringUtils.isBlank(username)) {
            map.put("usernameMsg", "账号不能为空！");
            return map;
        }
        if (StringUtils.isBlank(password)) {
            map.put("passwordMsg", "密码不能为空！");
            return map;
        }

        // 验证账号
        User user = userMapper.getByName(username);
        if (user == null) {
            map.put("usernameMsg", "该账号不存在！");
            return map;
        }
        // 验证状态
        if (user.getStatus() == 0) {
            map.put("usernameMsg", "该账号未激活！");
            return map;
        }
        // 验证密码
        password = CommunityUtil.md5(password + user.getSalt());
        if (!user.getPassword().equals(password)) {
            map.put("passwordMsg", "密码不正确！");
            return map;
        }

        // 生成登陆凭证
        LoginTicket loginTicket = new LoginTicket();
        loginTicket.setUserId(user.getId());
        // 添加登录凭证，通过随机生成字符串方法生成登录凭证
        loginTicket.setTicket(CommunityUtil.generateUUID());
        // 设置为有效凭证
        loginTicket.setStatus(0);
        loginTicket.setExpired(new Date(System.currentTimeMillis() + expiredSeconds * 1000));
        //loginTicketMapper.insertLoginTicket(loginTicket);

        // 获得凭证的key
        String redisKey = RedisKeyUtil.getTicketKey(loginTicket.getTicket());
        // 将凭证存入redis中  这个是将登陆凭证这个对象设为value,redis会自动将这个对象解析成json字符串存进去的
        redisTemplate.opsForValue().set(redisKey, loginTicket);

        // 将登陆凭证传给controller
        map.put("ticket", loginTicket.getTicket());
        return map;
    }

    // 忘记密码
    public String forget(String email) {
        // 生成6位随机码
        String code = CommunityUtil.generateUUID().substring(0, 6);

        // 发送激活邮件
        // 设置要传送的模板内容,这个Context对象是用来存储键值对的
        Context context = new Context();
        context.setVariable("email", email);
        context.setVariable("code", code);

        // content才是真正要通过邮件发送出去的内容  通过模板引擎对象将context键值对传送给HTML静态模板，并且解析出来给context字符串
        String content = templateEngine.process("/mail/forget", context);

        // 将content字符串通过邮箱发送
        mailClient.sendMail(email, "惠农网找回密码", content);

        return code;
    }

    // 账号退出
    public void logout(String ticket) {
        // 将登陆凭证修改为无效
        //loginTicketMapper.updateStatus(ticket, 1);
        String redisKey = RedisKeyUtil.getTicketKey(ticket);
        LoginTicket loginTicket = (LoginTicket) redisTemplate.opsForValue().get(redisKey);
        loginTicket.setStatus(1);

        // 这里是不用删除的，因为每一个用户的登陆凭证就一个，因为redisKey对每一个用户来说是唯一的，所以每一次创建登陆凭证，删除登陆凭证只是就该这个键值对中loginTiket的属性状态和有效时常而已，并不是真的在数据库中创建新的数据或删除数据
        redisTemplate.opsForValue().set(redisKey, loginTicket);
    }

    public LoginTicket getLoginTicket(String ticket) {
        //return loginTicketMapper.getByTicket(ticket);
        String redisKey = RedisKeyUtil.getTicketKey(ticket);
        return (LoginTicket) redisTemplate.opsForValue().get(redisKey);
    }
    // 这个也是同一个方法中对数据库有两次修改操作，所以应该也引入事务管理，但是这两个操作是对两种数据库的操作，所以就没有办法引入事务管理
    public int updateHeader(int userId, String headerUrl) {
        int rows = userMapper.updateHeader(userId, headerUrl);
        clearCache(userId);
        return rows;
    }

    public User getUserByName(String username) {
        return userMapper.getByName(username);
    }

    // 1,优先从缓存中取值
    private User getCache(int userId) {
        String redisKey = RedisKeyUtil.getUserKey(userId);
        return (User) redisTemplate.opsForValue().get(redisKey);
    }
    // 2,从缓存中取不到时初始化缓存数据，并且返回用户信息
    private User initCache(int userId) {
        // 从mysql中获取用户信息
        User user = userMapper.getById(userId);
        String redisKey = RedisKeyUtil.getUserKey(userId);
        // 将用户信息存入redis，并且设置1小时的有效时常
        redisTemplate.opsForValue().set(redisKey, user, 3600, TimeUnit.SECONDS);
        return user;
    }
    // 3,数据变更时清除缓存数据
    private void clearCache(int userId) {
        String redisKey = RedisKeyUtil.getUserKey(userId);
        // 直接将这个key的数据删除
        redisTemplate.delete(redisKey);
    }

    /**
     * 查询某个用户的权限
     * @param userId 要查询的用户
     * @return
     */
    public Collection<? extends GrantedAuthority> getAuthorities(int userId) {
        User user = this.getUserById(userId);

        List<GrantedAuthority> list = new ArrayList<>();
        list.add(new GrantedAuthority() {
            @Override
            public String getAuthority() {
                switch (user.getType()) {
                    case 1:
                        return AUTHORITY_ADMIN;
                    case 2:
                        return AUTHORITY_MODERATOR;
                    default:
                        return AUTHORITY_USER;
                }
            }
        });
        return list;
    }
}
