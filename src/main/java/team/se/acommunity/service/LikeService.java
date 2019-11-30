package team.se.acommunity.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.stereotype.Service;
import team.se.acommunity.util.RedisKeyUtil;

@Service
public class LikeService {
    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 点赞
     * @param userId 当前要点赞的用户
     * @param entityType 点赞对象的实体类型
     * @param entityId 点赞对象的id
     * @param entityUserId 点赞对象实体的作者id
     */
    public void saveLike(int userId, int entityType, int entityId, int entityUserId) {
//        // 因为点赞value是set类型，所以先判断userId是不是已经点过赞了，即已经存在在set当中了
//        String entityLikeKey = RedisKeyUtil.getEntityLikeKey(entityType, entityId);
//        // 检查当前用户是否已经点赞
//        boolean isMember = redisTemplate.opsForSet().isMember(entityLikeKey, userId);
//
//        // 已点赞就取消点赞，未点赞就添加点赞
//        if (isMember) {
//            redisTemplate.opsForSet().remove(entityLikeKey, userId);
//        } else {
//            redisTemplate.opsForSet().add(entityLikeKey, userId);
//        }

        // 现需要加入redis数据库的事务管理，所以重构以前的代码
        redisTemplate.execute(new SessionCallback() {
            @Override
            public Object execute(RedisOperations redisOperations) throws DataAccessException {
                String entityLikeKey = RedisKeyUtil.getEntityLikeKey(entityType, entityId);
                String userLikeKey = RedisKeyUtil.getUserLikeKey(entityUserId);

                boolean isMember = redisOperations.opsForSet().isMember(entityLikeKey, userId);

                // 这个命令是启用事务，之前说过redis的事务管理方法是将命令加到队列中，然后一并提交执行，所以上面的查询是要写在事务启用之前的，否则是查询不到东西的。
                redisOperations.multi();

                if (isMember) {
                    redisOperations.opsForSet().remove(entityLikeKey, userId);
                    // 将当前实体作者的点赞数减一
                    redisOperations.opsForValue().decrement(userLikeKey);
                } else {
                    redisOperations.opsForSet().add(entityLikeKey, userId);
                    // 将当前实体作者的点赞数加一
                    redisOperations.opsForValue().increment(userLikeKey);
                }
                return redisOperations.exec();
            }
        });
    }

    /**
     * 统计某实体点赞数量
     * @param entityType
     * @param entityId
     * @return
     */
    public long countEntityLike(int entityType, int entityId) {
        String entityLikeKey = RedisKeyUtil.getEntityLikeKey(entityType, entityId);
        // 直接统计这个key的value值个数就可以
        return redisTemplate.opsForSet().size(entityLikeKey);
    }

    /**
     * 查询某个人对某实体的点赞状态，这里选用int为返回值是为了为以后业务功能扩充做准备，比如以后如果想添加一个踩的功能，这样这个int就可以表示多种状态了
     * @param userId
     * @param entityType
     * @param entityId
     * @return
     */
    public int getEntityLikeStatus(int userId, int entityType, int entityId) {
        String entityLikeKey = RedisKeyUtil.getEntityLikeKey(entityType, entityId);

        return redisTemplate.opsForSet().isMember(entityLikeKey, userId) ? 1 : 0;
    }

    public int countUserLike(int userId) {
        String userLikeKey  = RedisKeyUtil.getUserLikeKey(userId);
        // 因为默认返回Object，所以只能将其强制转换成Integet，不能转换成基础变量int，因为int不是对象，不是Object的子类
        Integer count = (Integer) redisTemplate.opsForValue().get(userLikeKey);
        // 判断一下，如果没有人给这个人点赞就像count置为0
        return count == null ? 0 : count.intValue();
    }
}
