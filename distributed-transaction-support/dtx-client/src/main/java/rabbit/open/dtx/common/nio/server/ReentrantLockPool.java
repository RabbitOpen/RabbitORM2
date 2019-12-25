package rabbit.open.dtx.common.nio.server;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 锁池
 * @author xiaoqianbin
 * @date 2019/12/25
 **/
public class ReentrantLockPool {

    private static Map<String, List<ReentrantLock>> pool = new ConcurrentHashMap<>();

    private static final int LOCK_POOL_SIZE = 50;

    private ReentrantLockPool() {

    }

    /***
     * 锁资源
     * @param	namespace   锁命名空间
	 * @param	id
     * @author  xiaoqianbin
     * @date    2019/12/25
     **/
    public static void lock(String namespace, String id) {
        if (!pool.containsKey(namespace)) {
            initLock(namespace);
        }
        pool.get(namespace).get(Math.abs(id.hashCode() % LOCK_POOL_SIZE)).lock();
    }

    /***
     * 解锁资源
     * @param	namespace   锁命名空间
     * @param	id
     * @author  xiaoqianbin
     * @date    2019/12/25
     **/
    public static void unlock(String namespace, String id) {
        pool.get(namespace).get(Math.abs(id.hashCode() % LOCK_POOL_SIZE)).unlock();
    }

    /***
     * 锁资源
     * @param	namespace   锁命名空间
     * @param	id
     * @author  xiaoqianbin
     * @date    2019/12/25
     **/
    public static boolean tryLock(String namespace, String id) {
        if (!pool.containsKey(namespace)) {
            initLock(namespace);
        }
        return pool.get(namespace).get(Math.abs(id.hashCode() % LOCK_POOL_SIZE)).tryLock();
    }

    private static synchronized  void initLock(String prefix) {
        if (pool.containsKey(prefix)) {
            return;
        }
        pool.put(prefix, new ArrayList<>(LOCK_POOL_SIZE));
        for (int i = 0; i < LOCK_POOL_SIZE; i++) {
            pool.get(prefix).add(new ReentrantLock());
        }
    }

}
