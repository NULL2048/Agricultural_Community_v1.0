package team.se.acommunity.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.stereotype.Service;
import team.se.acommunity.entity.User;
import team.se.acommunity.util.CommunityConstant;
import team.se.acommunity.util.RedisKeyUtil;
import team.se.acommunity.util.SensitiveFilter;

import java.util.*;

@Service
public class FollowService implements CommunityConstant {
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private UserService userService;

    /**
     * 处理关注业务
     * @param userId
     * @param entityType
     * @param entityId
     */
    public void follow(int userId, int entityType, int entityId) {
        // 关注业务首先是设置followee,然后设置follower，一共有两次存储操作，所以这里也要进行事务管理
        redisTemplate.execute(new SessionCallback() {
            @Override
            public Object execute(RedisOperations redisOperations) throws DataAccessException {
                // 设置要操作的两个key，查询要在启用事务之前
                String followeeKey = RedisKeyUtil.getFolloweeKey(userId, entityType);
                String followerKey = RedisKeyUtil.getFollowerKey(entityType, entityId);

                // 启用事务
                redisOperations.multi();
                // currentTimeMillis()获取当前时间 后面的这个属性就是这个值的分数，sortedset排序的时候就是按照这个参数为标准来排序的,这个时间单位是毫秒，是一个double类型的数据
                redisOperations.opsForZSet().add(followeeKey, entityId, System.currentTimeMillis());
                redisOperations.opsForZSet().add(followerKey, userId, System.currentTimeMillis());

                return redisOperations.exec();
            }
        });
    }

    /**
     * 取消关注
     * @param userId
     * @param entityType
     * @param entityId
     */
    public void unfollow(int userId, int entityType, int entityId) {
        // 关注业务首先是设置followee,然后设置follower，一共有两次存储操作，所以这里也要进行事务管理
        redisTemplate.execute(new SessionCallback() {
            @Override
            public Object execute(RedisOperations redisOperations) throws DataAccessException {
                // 设置要操作的两个key，查询要在启用事务之前
                String followeeKey = RedisKeyUtil.getFolloweeKey(userId, entityType);
                String followerKey = RedisKeyUtil.getFollowerKey(entityType, entityId);

                // 启用事务
                redisOperations.multi();
                // 将指定key中的entityId的set删除
                redisOperations.opsForZSet().remove(followeeKey, entityId);
                redisOperations.opsForZSet().remove(followerKey, userId);

                return redisOperations.exec();
            }
        });
    }

    /**
     * 查询一个用户关注的实体的数量
     * @param userId 要查询关注数量的用户
     * @param entityType 要查询这个用户什么类型实体的关注数量
     * @return
     */
    public long countFollowee(int userId, int entityType) {
        String followeeKey = RedisKeyUtil.getFolloweeKey(userId, entityType);
        // 查询这个key的元素数量
        return redisTemplate.opsForZSet().zCard(followeeKey);
    }

    /**
     * 查询实体的粉丝数量
     * @param entityType 实体类型
     * @param entityId 实体id
     * @return
     */
    public long countFollower(int entityType, int entityId) {
        String followerKey = RedisKeyUtil.getFollowerKey(entityType, entityId);
        return redisTemplate.opsForZSet().zCard(followerKey);
    }

    /**
     * 查询当前用户是否已经关注该实体
     * @param userId
     * @param entityType
     * @param entityId
     * @return
     */
    public boolean hasFollowed(int userId, int entityType, int entityId) {
        String followeeKey = RedisKeyUtil.getFolloweeKey(userId, entityType);
        // 这个就是查询这个followeeKey中set集合中的entityId这个元素，查询它的score分数，也就是关注时间，如果能查询到，说明该用户已经关注这个实体了
        return redisTemplate.opsForZSet().score(followeeKey, entityId) != null;
    }

    /**
     * 查询某用户关注的人
     * @param userId 因为这里暂时就先不开发关注别的实体了，就写死了实体就是user，所以这里直接传userID就可以了
     * @param offset
     * @param limit
     * @return
     */
    public List<Map<String, Object>> listFollowees(int userId, int offset, int limit) {
        String followeeKey = RedisKeyUtil.getFolloweeKey(userId, ENTITY_TYPE_USER);

        // 这个是查询followeeKey这个key的set集合，并且按照从小到大排序，显示的范围就是[offset, offset + limit - 1]就是排序好的set集合然后从里面取这个范围里的数据
        Set<Integer> targetIds =  redisTemplate.opsForZSet().reverseRange(followeeKey, offset, offset + limit - 1);

        if (targetIds == null) {
            return null;
        }

        List<Map<String, Object>> list = new ArrayList<>();
        for (Integer targetId : targetIds) {
            Map<String, Object> map = new HashMap<>();
            User user = userService.getUserById(targetId);
            map.put("user", user);

            // 获得当前followeeKey的value集合中的这个targetId元素的分数，也就是当时传进去的时间毫秒数，这里可也再将他转换成日期数据
            Double score = redisTemplate.opsForZSet().score(followeeKey, targetId);
            // 将毫秒编程日期数据
            map.put("followTime", new Date(score.longValue()));

            list.add(map);
        }
        return list;
    }

    /**
     * 查询某用户的粉丝
     * @param userId
     * @param offset
     * @param limit
     * @return
     */
    public List<Map<String, Object>> listFollowers(int userId, int offset, int limit) {
        String followerKey = RedisKeyUtil.getFollowerKey(ENTITY_TYPE_USER, userId);
        Set<Integer> targetIds = redisTemplate.opsForZSet().reverseRange(followerKey, offset, offset + limit - 1);

        if (targetIds == null) {
            return null;
        }

        List<Map<String, Object>> list = new ArrayList<>();
        for (Integer targetId : targetIds) {
            Map<String, Object> map = new HashMap<>();
            User user = userService.getUserById(targetId);

            map.put("user", user);
            Double score = redisTemplate.opsForZSet().score(followerKey, targetId);
            map.put("followTime", new Date(score.longValue()));

            list.add(map);
        }
        return list;
    }
}
