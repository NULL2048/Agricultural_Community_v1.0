package team.se.acommunity.service;

import io.lettuce.core.RedisURI;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisStringCommands;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import team.se.acommunity.util.RedisKeyUtil;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

@Service
public class DataService {
    @Autowired
    private RedisTemplate redisTemplate;

    // 格式化日期格式
    private SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd");

    // 将指定的IP计入UV
    public void recordUV(String ip) {
        String redisKey = RedisKeyUtil.getUVKey(df.format(new Date()));
        redisTemplate.opsForHyperLogLog().add(redisKey, ip);
    }

    // 统计指定日期范围内的UV
    public long calculateUV(Date start, Date end) {
        if (start == null || end == null) {
            throw new IllegalArgumentException("参数不能为空");
        }

        // 整理该日期范围内的key
        List<String> keyList = new ArrayList<>();
        // 创建Calendar对象来进行日期计算
        Calendar calendar = Calendar.getInstance();
        // 创建start日期的calendar对象
        calendar.setTime(start);
        // 当事件没有超过end这个事件时就一直循环
        while (!calendar.getTime().after(end)) {
            // 得到当前时间的redisKey
            String key = RedisKeyUtil.getUVKey(df.format(calendar.getTime()));
            // 将key存入到集合中
            keyList.add(key);
            // 将日期以天为单位，加一
            calendar.add(Calendar.DATE, 1);
        }

        // 合并这些数据
        String redisKey = RedisKeyUtil.getUVKey(df.format(start), df.format(end));
        // 传入要合并的key的数组，记得要使用toArray方法，因为参数要求是数组，而不是集合
        redisTemplate.opsForHyperLogLog().union(redisKey, keyList.toArray());

        // 返回统计结果
        return redisTemplate.opsForHyperLogLog().size(redisKey);
    }

    // 将指定用户计入DAU
    public void recordDAU(int userId) {
        String redisKey = RedisKeyUtil.getDAUKey(df.format(new Date()));
        redisTemplate.opsForValue().setBit(redisKey, userId, true);
    }

    // 统计指定日期范围内的DAU
    public long calculateDAU(Date start, Date end) {
        if (start == null || end == null) {
            throw new IllegalArgumentException("参数不能为空");
        }

        // 整理该日期范围内的key  bitmap要的key的格式必须是byte数组类型
        List<byte[]> keyList = new ArrayList<>();
        // 创建Calendar对象来进行日期计算
        Calendar calendar = Calendar.getInstance();
        // 创建start日期的calendar对象
        calendar.setTime(start);
        // 当事件没有超过end这个事件时就一直循环
        while (!calendar.getTime().after(end)) {
            // 得到当前时间的redisKey
            String key = RedisKeyUtil.getDAUKey(df.format(calendar.getTime()));
            // 将key存入到集合中
            keyList.add(key.getBytes());
            // 将日期以天为单位，加一
            calendar.add(Calendar.DATE, 1);
        }

        // 进行or运算    只要是指定的范围内这个用户登陆过一次，那么求or运算得到的就是true，就算是活跃
        return (long) redisTemplate.execute(new RedisCallback() {
            @Override
            public Object doInRedis(RedisConnection redisConnection) throws DataAccessException {
                // 将结果存到一个新Key中
                String redisKey = RedisKeyUtil.getDAUKey(df.format(start), df.format(end));
                // 这里就要注意因为bitMap的key要求时byte数组，所以要想存一个byte数组的数组，那一定要转换成二维数组，所以要写toArray(new byte[0][0])
                redisConnection.bitOp(RedisStringCommands.BitOperation.OR,
                        redisKey.getBytes(), keyList.toArray(new byte[0][0]));
                // 返回统计值，取or运算之后的结果中是true的元素的个数
                return redisConnection.bitCount(redisKey.getBytes());
            }
        });
    }
}
