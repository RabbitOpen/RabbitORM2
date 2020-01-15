package rabbit.open.dtx.server.jedis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rabbit.open.dtx.server.PopInfo;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 池化的jedis客户端
 * @author xiaoqianbin
 * @date 2020/1/2
 **/
@SuppressWarnings("unchecked")
public class PooledJedisClient implements JedisClient {

    private Logger logger = LoggerFactory.getLogger(getClass());

    // casHset方法脚本
    protected String casHsetScript = "local status = redis.call('hget', ARGV[1], ARGV[2])\n" +
                                            "if status ~= ARGV[4] then\n" +
                                            "redis.call('hset', ARGV[1], ARGV[2], ARGV[3]) return 1\n" +
                                            "else return 0 end;";
    // casHsetScript对应的sha
    protected String casHsetScriptSha;

    protected String casLpopByPrefixScript = "local head = redis.call('lindex', ARGV[1], 0)\n" +
                                            "if string.sub(head,1,string.len(ARGV[2]))== ARGV[2] then \n" +
                                            "return {redis.call('lpop', ARGV[1]), redis.call('lindex', ARGV[1], 0)};" +
                                            "else return {} end;";

    protected String casLpopByPrefixScriptSha;

    protected String hsetGetAllScript = "redis.call('hset', ARGV[1], ARGV[2], ARGV[3])\n return redis.call('hgetall', ARGV[1])";

    protected String hsetGetAllScriptSha;

    protected String hgetAllAndDelScript = "local map = redis.call('hgetall', ARGV[1])\n " +
                                            "redis.call('del', ARGV[1])\n return map;";

    protected String hgetAllAndDelScriptSha;

    private JedisPool pool;

    public PooledJedisClient(JedisPool pool) {
        this.pool = pool;
        casHsetScriptSha = (String) execute(jedis -> jedis.scriptLoad(casHsetScript));
        casLpopByPrefixScriptSha = (String) execute(jedis -> jedis.scriptLoad(casLpopByPrefixScript));
        hsetGetAllScriptSha = (String) execute(jedis -> jedis.scriptLoad(hsetGetAllScript));
        hgetAllAndDelScriptSha = (String) execute(jedis -> jedis.scriptLoad(hgetAllAndDelScript));
    }

	@Override
    public Map<String, String> hgetAll(String key) {
        return (Map<String, String>) execute(jedis -> jedis.hgetAll(key));
    }

    @Override
    public Long hset(String key, String field, String value) {
        return (Long) execute(jedis -> jedis.hset(key, field, value));
    }

    /**
     * 先hset，然后hgetAll
     * @param	key
     * @param	field
     * @param	value
     * @author  xiaoqianbin
     * @date    2020/1/6
     **/
    @Override
    public Map<String, String> hsetGetAll(String key, String field, String value) {
        return list2map((List<String>) execute(jedis -> jedis.evalsha(hsetGetAllScriptSha, 0, key, field, value)));
    }

    @Override
    public Map<String, String> hgetAllAndDel(String key) {
        return list2map((List<String>) execute(jedis -> jedis.evalsha(hgetAllAndDelScriptSha, 0, key)));
    }

    @Override
    public String hget(String key, String field) {
        return (String) execute(jedis -> jedis.hget(key, field));
    }

    /**
     * 原子设值，如果map中字段{field}的值不等于exclude就设为expected, 否则什么都不做
     * @param	key
     * @param	field
     * @param	expected
     * @param	exclude
     * @author  xiaoqianbin
     * @date    2020/1/3
     * @return  1表示写数据成功，0表示未修改数据
     **/
    @Override
    public Long casHset(String key, String field, String expected, String exclude) {
        return (Long) execute(jedis -> jedis.evalsha(casHsetScriptSha, 0, key, field, expected, exclude));
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
    public Long rpush(String key, String... strings) {
        return (Long) execute(jedis -> jedis.rpush(key, strings));
    }

    /**
     * 如果当前头部是以指定的prefix开头的话，就取出并移除头部、获取但不移除key的头部信息
     * @param	key
     * @param	prefix
     * @author  xiaoqianbin
     * @date    2020/1/3
     **/
    @Override
    public PopInfo casLpopByPrefix(String key, String prefix) {
        List<Object> result = (List<Object>) execute(jedis -> jedis.evalsha(casLpopByPrefixScriptSha, 0, key, prefix));
        if (result.isEmpty()) {
            return new PopInfo(null, null);
        }
        return new PopInfo((String) result.get(0), (String) result.get(1));
    }

    @Override
    public synchronized void close() {
        if (pool.isClosed()) {
            return;
        }
        pool.close();
        logger.info("jedis client is closed");
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

    protected Map<String, String> list2map(List<String> list) {
        Map<String, String> map = new HashMap<>();
        for (int i = 0; i < list.size() - 1; i++) {
            if (0 == i % 2){
                map.put(list.get(i), list.get(i + 1));
            }
        }
        return map;
    }

    @FunctionalInterface
    public interface Callable {
        Object invoke(Jedis jedis);
    }
}
