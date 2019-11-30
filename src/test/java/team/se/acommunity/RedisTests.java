package team.se.acommunity;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisStringCommands;
import org.springframework.data.redis.core.*;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import team.se.acommunity.util.SensitiveFilter;

import java.util.concurrent.TimeUnit;

@RunWith(SpringRunner.class)
@SpringBootTest
@ContextConfiguration(classes = AgriculturalCommunityApplication.class)
public class RedisTests {
    // 将配置好的RedisTemplate从spring容器中注入进来
    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 测试String类型数据
     */
    @Test
    public void testString() {
        // 存入数据    redis规范建议将key值以一个单词开头然后加:后面写key的名字
        String redisKey = "test:count";
        // 像这个key中存入value
        redisTemplate.opsForValue().set(redisKey, 1);

        // 取数据
        System.out.println(redisTemplate.opsForValue().get(redisKey));
        // 将值自增
        System.out.println(redisTemplate.opsForValue().increment(redisKey));
        // 将值自减
        System.out.println(redisTemplate.opsForValue().decrement(redisKey));
    }

    @Test
    public void testHash() {
        String redisKey = "test:user";

        // 向hash里面存入值
        redisTemplate.opsForHash().put(redisKey, "id", 1);
        redisTemplate.opsForHash().put(redisKey, "username", "zhangsan");

        System.out.println(redisTemplate.opsForHash().get(redisKey, "id"));
        System.out.println(redisTemplate.opsForHash().get(redisKey, "username"));
    }

    @Test
    public void testLists() {
        String redisKey = "test:ids";

        // 从左边插入数据
        redisTemplate.opsForList().leftPush(redisKey, 101);
        redisTemplate.opsForList().leftPush(redisKey, 102);
        redisTemplate.opsForList().leftPush(redisKey, 103);

        // 获得这个列表的大小
        System.out.println(redisTemplate.opsForList().size(redisKey));
        // 获取指定下标位置的值
        System.out.println(redisTemplate.opsForList().index(redisKey, 0));
        // 获取某一个下标范围的数据
        System.out.println(redisTemplate.opsForList().range(redisKey, 0, 2));

        // 从左边弹出数据
        System.out.println(redisTemplate.opsForList().leftPop(redisKey));
        System.out.println(redisTemplate.opsForList().leftPop(redisKey));
        System.out.println(redisTemplate.opsForList().leftPop(redisKey));

    }

    @Test
    public void testSet() {
        String redisKey = "test:teachers";
        // 向set中添加数据，参数可以写多个数据
        redisTemplate.opsForSet().add(redisKey, "刘备", "关羽", "张飞", "诸葛亮");

        // 集合中数据的个数
        System.out.println(redisTemplate.opsForSet().size(redisKey));
        // 从集合中删除一个数据
        System.out.println(redisTemplate.opsForSet().pop(redisKey));
        // 输出集合中的元素
        System.out.println(redisTemplate.opsForSet().members(redisKey));
    }

    @Test
    public void testSortedSet() {
        String redisKey = "test:students";

        // 有序集合存入数据有点像hash表，但是他插入的数据的key不能重复
        redisTemplate.opsForZSet().add(redisKey, "唐僧", 80);
        redisTemplate.opsForZSet().add(redisKey, "悟空", 90);
        redisTemplate.opsForZSet().add(redisKey, "八戒", 70);
        redisTemplate.opsForZSet().add(redisKey, "沙僧", 60);

        // 统计集合中的个数
        System.out.println(redisTemplate.opsForZSet().zCard(redisKey));
        // 查询某一个人的分数
        System.out.println(redisTemplate.opsForZSet().score(redisKey, "八戒"));
        System.out.println(redisTemplate.opsForZSet().score(redisKey, "悟空"));

        // 统计某一个人的排名，根据value来排名，这个是从小到大,返回的是索引
        System.out.println(redisTemplate.opsForZSet().rank(redisKey, "沙僧"));
        // 这个是从大到小排名
        System.out.println(redisTemplate.opsForZSet().reverseRank(redisKey, "沙僧"));
        // 取某一个范围内的数据, 这个范围是排名的范围，这个是从小到大排名
        System.out.println(redisTemplate.opsForZSet().range(redisKey, 0, 2));
        // 这个是从大到小排名，取排名范围
        System.out.println(redisTemplate.opsForZSet().reverseRange(redisKey, 0, 2));

    }

    @Test
    public void testKeys() {
        // 删除这个key的数据
        redisTemplate.delete("test:user");

        // 查看这个key是否存在，不存在返回false
        System.out.println(redisTemplate.hasKey("test:user"));

        // 设置数据的生命周期，设置生效时常,最后一个参数是指明时间单位
        redisTemplate.expire("test:students", 10, TimeUnit.SECONDS);
    }

    /**
     * 多次访问同一个Key,以绑定的形式来存在
     */
    @Test
    public void testBoundOperations() {
        String redisKey = "test:count";

        // 将operations这个对象和这个redisKey绑定起来，以后想要操作这个key里面的数据，就可以直接用这个对象，后面boundValueOps这个方法根据value的类型不同是使用不同方法的
        BoundValueOperations operations = redisTemplate.boundValueOps(redisKey);
        // 自增
        operations.increment();
        operations.increment();
        operations.increment();
        operations.increment();
        System.out.println(operations.get());
    }

    // 编程式事务
    @Test
    public void testTransactional() {
        // 要覆写这个SessionCallback类，将这个类通过execute方法传入redisTemplate，当执行redis命令的时候他会自动执行
        Object obj = redisTemplate.execute(new SessionCallback() {
            // redisOperations这个对象就是redis命令执行对象,下面就不用redisOperations这个来执行命令了，而是直接用redisOperations这个对象来执行
            @Override
            public Object execute(RedisOperations redisOperations) throws DataAccessException {
                String redisKey = "test:tx";

                // 启用事务
                redisOperations.multi();

                // 添加数据
                redisOperations.opsForSet().add(redisKey, "zhangsan");
                redisOperations.opsForSet().add(redisKey, "lisi");
                redisOperations.opsForSet().add(redisKey, "wangwu");

                // 因为redis的事务事先将每一个命令添加到队列里面，然后一并提交到redis数据库执行，所以此时查询是查不到数据的
                System.out.println(redisOperations.opsForSet().members(redisKey));

                // 提交事务
                return redisOperations.exec();
            }
        });
        // 这个输出的前三个数据，是每一条命令影响的行数
        System.out.println(obj);
    }

    // 统计20万个重复数据的独立总数
    @Test
    public void testHyperLogLog() {
        String redisKey = "test:hll:01";

        for (int i = 1; i <= 100000; i++) {
            redisTemplate.opsForHyperLogLog().add(redisKey, i);
        }

        for (int i = 1; i <= 100000; i++) {
            int r = (int) (Math.random() * 100000 + 1);
            redisTemplate.opsForHyperLogLog().add(redisKey, r);
        }
        // 最后的去重结果一定是100000，因为求得随机数就是100000范围内的，去重之后一定就是第一次循环插入的那100000个数据，不会超出这个范围
        long size = redisTemplate.opsForHyperLogLog().size(redisKey);
        // 得到了一个估值，但是误差很小 结果是99553
        System.out.println(size);
    }

    // 将三组数据合并，再统计合并后的重复数据的独立总数
    @Test
    public void testHyperLogLogUnion() {
        String redisKey2 = "test:hll:02";
        for (int i = 1; i <= 10000; i++) {
            redisTemplate.opsForHyperLogLog().add(redisKey2, i);
        }

        String redisKey3 = "test:hll:03";
        for (int i = 5001; i <= 15000; i++) {
            redisTemplate.opsForHyperLogLog().add(redisKey3, i);
        }

        String redisKey4 = "test:hll:04";
        for (int i = 10001; i <= 20000; i++) {
            redisTemplate.opsForHyperLogLog().add(redisKey4, i);
        }

        // 上面数据的范围是1-20000，所以最后合并完统计的的独立总数一定是20000
        // redis对HtperLogLog合并就是将要合并的数据合并之后再存入到一个新的HtperLogLog数据中，所以要再创建一个新的key
        String unionKey = "test:hll:union";
        redisTemplate.opsForHyperLogLog().union(unionKey, redisKey2, redisKey3, redisKey4);

        long size = redisTemplate.opsForHyperLogLog().size(unionKey);
        // 结果也是不精确的19833
        System.out.println(size);
    }

    // 统计一组数据的布尔值
    @Test
    public void testBitMap() {
        String redisKey = "test:bm:01";

        // 记录
        // BiMap并不是一个独立的数据类型，他其实就是一个数组，只不过是按位存储
        // 他是一个变长的数组，根据传入的值进行动态扩充，默认数组元素是false，注意虽然传入的值是一个逻辑值true/false，但实际存储的时候就是按01存储的
        // 下面第二个参数是索引位置（数组下表位置），存入的是true
        redisTemplate.opsForValue().setBit(redisKey, 1, true);
        redisTemplate.opsForValue().setBit(redisKey, 4, true);
        redisTemplate.opsForValue().setBit(redisKey, 7, true);

        // 查询
        System.out.println(redisTemplate.opsForValue().getBit(redisKey, 0));
        System.out.println(redisTemplate.opsForValue().getBit(redisKey, 1));
        System.out.println(redisTemplate.opsForValue().getBit(redisKey, 2));

        // 统计
        // 统计的方法没有封装在对象中，要通过获取redis底层连接来进行统计
        // 执行execute方法的时候他会自动创建RedisCallback，然后就会自动执行doInRedis这个方法
        Object obj = redisTemplate.execute(new RedisCallback() {
            @Override
            public Object doInRedis(RedisConnection redisConnection) throws DataAccessException {
                // redisConnection传入的这个参数就是redis连接
                // 直接调用连接对象中的bitCount这个方法，传入的key规定要转为byte数组类型，他会统计这个bitmap数组中值为1的元素个数
                return redisConnection.bitCount(redisKey.getBytes());
            }
        });
        System.out.println(obj);
    }

    // 统计3组数据的布尔值，并对这3组数据做or运算
    @Test
    public void testBitMapOperation() {
        String redisKey2 = "test:bm:02";
        redisTemplate.opsForValue().setBit(redisKey2, 1, true);
        redisTemplate.opsForValue().setBit(redisKey2, 2, true);
        redisTemplate.opsForValue().setBit(redisKey2, 3, true);

        String redisKey3 = "test:bm:03";
        redisTemplate.opsForValue().setBit(redisKey2, 2, true);
        redisTemplate.opsForValue().setBit(redisKey2, 3, true);
        redisTemplate.opsForValue().setBit(redisKey2, 4, true);

        String redisKey4 = "test:bm:04";
        redisTemplate.opsForValue().setBit(redisKey2, 4, true);
        redisTemplate.opsForValue().setBit(redisKey2, 5, true);
        redisTemplate.opsForValue().setBit(redisKey2, 6, true);

        // 和上面那个合并的方法类此，他们求or运算后会将结果存放到一个新的key中，所以要声明一个新key
        String redisKey = "test:bm:or";
        Object obj = redisTemplate.execute(new RedisCallback() {
            @Override
            public Object doInRedis(RedisConnection redisConnection) throws DataAccessException {
                // 是将三组数据合并起来，对应的位置一一做或运算
                // 需要调用链接对象中的运算符方法bitOp   第一个参数传入的是要进行什么运算  第二个参数是将结果存到哪个key中，后面就是要参加运算的值的key，注意全部要转换成byte传入
                redisConnection.bitOp(RedisStringCommands.BitOperation.OR,
                        redisKey.getBytes(), redisKey2.getBytes(), redisKey3.getBytes(), redisKey4.getBytes());
                // 返回统计结果
                return redisConnection.bitCount(redisKey.getBytes());
            }
        });

        // 取或运算结果一定是6，因为每一个位置都有true,就算是和false取或结果结果还是true
        System.out.println(obj);
    }
}
