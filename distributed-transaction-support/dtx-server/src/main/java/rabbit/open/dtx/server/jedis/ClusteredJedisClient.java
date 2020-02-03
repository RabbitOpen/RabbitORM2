package rabbit.open.dtx.server.jedis;

import rabbit.open.dtx.common.nio.pub.CallHelper;
import rabbit.open.dtx.server.PopInfo;
import redis.clients.jedis.JedisCluster;

import java.util.List;
import java.util.Map;

/**
 * 基于redis cluster的客户端
 * @author xiaoqianbin
 * @date 2020/1/21
 **/
@SuppressWarnings("unchecked")
public class ClusteredJedisClient extends PooledJedisClient {

    protected JedisCluster cluster;

    public ClusteredJedisClient(JedisCluster cluster) {
        this.cluster = cluster;
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
        return list2map((List<String>) execute(jedis -> ((JedisCluster)jedis).eval(hsetGetAllScript, 0, key, field, value)));
    }

    @Override
    public Map<String, String> hgetAllAndDel(String key) {
        return list2map((List<String>) execute(jedis -> ((JedisCluster)jedis).eval(hgetAllAndDelScript, 0, key)));
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
        return (Long) execute(jedis -> ((JedisCluster)jedis).eval(casHsetScript, 0, key, field, expected, exclude));
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
        List<Object> result = (List<Object>) execute(jedis -> ((JedisCluster)jedis).eval(casLpopByPrefixScript, 0, key, prefix));
        if (result.isEmpty()) {
            return new PopInfo(null, null);
        }
        return new PopInfo((String) result.get(0), (String) result.get(1));
    }

    @Override
    protected Object execute(Callable callable) {
        return callable.invoke(cluster);
    }

    @Override
    public synchronized void close() {
        CallHelper.ignoreExceptionCall(() -> {
            cluster.close();
            logger.info("jedis cluster is closed");
        });
    }
}
