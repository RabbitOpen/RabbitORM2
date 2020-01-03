package rabbit.open.dtx.common.nio.server;

import java.util.Map;
import java.util.Set;

/**
 * jedis客户端
 * @author xiaoqianbin
 * @date 2020/1/2
 **/
public interface JedisClient {

    Map<String, String> hgetAll(String key);

    Long hset(String key, String field, String value);

    String hget(String key, String field);

    /**
     * 原子设值，如果map中字段{field}的值不等于exclude就设为expected
     * @param	key
	 * @param	field
	 * @param	expected
	 * @param	exclude
     * @author  xiaoqianbin
     * @date    2020/1/3
     **/
    Long casHset(String key, String field, String expected, String exclude);

    Long incr(String key);

    Long hdel(String key, String... field);

    Long del(String key);

    Long zadd(String key, double score, String member);

    Long zrem(String key, String... members);

    Long zcount(String key, double min, double max);

    Set<String> zrangeByScore(String key, double min, double max);

    /**
     * 尾部插入数据，返回当前数据个数
     * @param	key
	 * @param	strings
     * @author  xiaoqianbin
     * @date    2020/1/2
     **/
    Long rpush(String key, String... strings);

    String lindex(String key, long index);

    /**
     * 取出并移除头部、获取但不移除key的头部信息
     * @param	key
     * @author  xiaoqianbin
     * @date    2020/1/3
     **/
    PopInfo casLpop(String key);

    void close();
}
