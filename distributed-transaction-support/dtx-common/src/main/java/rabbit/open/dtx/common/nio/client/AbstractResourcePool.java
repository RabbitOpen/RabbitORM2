package rabbit.open.dtx.common.nio.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rabbit.open.dtx.common.exception.GetConnectionTimeoutException;

import java.util.concurrent.locks.ReentrantLock;

/**
 * 抽象资源池
 * @author xiaoqianbin
 * @date 2019/12/7
 **/
public abstract class AbstractResourcePool<T extends PooledResource> {

    protected Logger logger = LoggerFactory.getLogger(getClass());

    protected RoundList<T> roundList;

    protected ReentrantLock createLock = new ReentrantLock();

    public AbstractResourcePool() {
        roundList = new RoundList<>();
    }

    // 获取当前连接数
    public int getResourceCount() {
        return roundList.size();
    }

    /**
     * 获取一个资源
     * @author xiaoqianbin
     * @date 2019/12/7
     **/
    public T getResource() {
        return getResource(0L);
    }

    /**
     * 获取一个资源
     * @param	timeoutMilliSeconds
     * @author  xiaoqianbin
     * @date    2019/12/12
     **/
    private T getResource(long timeoutMilliSeconds) {
        T resource;
        if (0 != timeoutMilliSeconds) {
            resource = roundList.browse(timeoutMilliSeconds);
            if (null == resource) {
                throw new GetConnectionTimeoutException(timeoutMilliSeconds);
            }
        } else {
            resource = roundList.browse();
        }
        if (null != resource) {
            return resource;
        } else {
            return getResource(10L * 1000);
        }
    }

    /**
     * 释放连接
     * @param    resource
     * @author xiaoqianbin
     * @date 2019/12/7
     **/
    public void release(T resource) {
        // to do: 因为RoundList并没有真正的取出节点，所以release什么都不做
    }

    /**
     * 移除资源
     * @param    resource
     * @author xiaoqianbin
     * @date 2019/12/7
     **/
    public void removeResource(T resource) {
        roundList.remove(resource);
        logger.debug("remove resource from pool");
    }

    /**
     * 尝试新建一个资源
     * @author xiaoqianbin
     * @date 2019/12/7
     **/
    protected void tryCreateResource() {
        if (!canCreate()) {
            return;
        }
        try {
            createLock.lock();
            if (canCreate()) {
                roundList.add(newResource());
            }
        } finally {
            createLock.unlock();
        }
    }

    protected abstract boolean canCreate();

    /**
     * 新建资源
     * @author xiaoqianbin
     * @date 2019/12/7
     **/
    protected abstract T newResource();

    /**
     * 关闭
     * @author  xiaoqianbin
     * @date    2019/12/8
     **/
    public abstract void gracefullyShutdown();

}
