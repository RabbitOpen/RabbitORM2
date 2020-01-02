package rabbit.open.dtx.common.nio.server;

import java.util.Map;

/**
 * jedis客户端
 * @author xiaoqianbin
 * @date 2020/1/2
 **/
public interface JedisClient {

    Map<String, String> hgetAll(String key);

    Long hset(String key, String field, String value);

    Long incr(String key);

    Long hdel(String key, String... field);

    Long del(String key);

    Long zadd(String key, double score, String member);

    Long zrem(String key, String... members);

    Long zcount(String key, double min, double max);

    void close();
}
