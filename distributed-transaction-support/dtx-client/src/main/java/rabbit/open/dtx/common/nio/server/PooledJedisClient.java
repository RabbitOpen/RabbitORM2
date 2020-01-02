package rabbit.open.dtx.common.nio.server;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisCommands;
import redis.clients.jedis.JedisPool;

import java.util.Map;
import java.util.Set;

/**
 * 池化的jedis客户端
 * @author xiaoqianbin
 * @date 2020/1/2
 **/
public class PooledJedisClient implements JedisClient {

    private JedisPool pool;

    public PooledJedisClient() {}

    public PooledJedisClient(JedisPool pool) {
        this();
        this.pool = pool;
    }


    @Override
    public Map<String, String> hgetAll(String key) {
        return (Map<String, String>) execute(jedis -> jedis.hgetAll(key));
    }

    @Override
    public Long hset(String key, String field, String value) {
        return (Long) execute(jedis -> jedis.hset(key, field, value));
    }

    @Override
    public Long incr(String key) {
        return (Long) execute(jedis -> jedis.incr(key));
    }

    @Override
    public Long hdel(String key, String... field) {
        return (Long) execute(jedis -> jedis.hdel(key, field));
    }

    @Override
    public Long del(String key) {
        return (Long) execute(jedis -> jedis.del(key));
    }

    @Override
    public Long zadd(String key, double score, String member) {
        return (Long) execute(jedis -> jedis.zadd(key, score, member));
    }

    @Override
    public Long zrem(String key, String... members) {
        return (Long) execute(jedis -> jedis.zrem(key, members));
    }

    @Override
    public Long zcount(String key, double min, double max) {
        return (Long) execute(jedis -> jedis.zcount(key, min, max));
    }

    @Override
    public Set<String> zrangeByScore(String key, double min, double max) {
        return (Set<String>) execute(jedis -> jedis.zrangeByScore(key, min, max));
    }

    @Override
    public void close() {
        pool.close();
    }

    protected Object execute(Callable callable) {
        Jedis jedis = null;
        try {
            jedis = pool.getResource();
            return callable.invoke(jedis);
        } finally {
             if (null != jedis) {
                 jedis.close();
             }
        }
    }

    @FunctionalInterface
    public interface Callable {
        Object invoke(JedisCommands jedisCommands);
    }
}
