package rabbit.open.dtx.common.nio.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rabbit.open.dtx.common.nio.exception.NetworkException;
import rabbit.open.dtx.common.nio.exception.RpcException;

import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 抽象资源池
 * @author xiaoqianbin
 * @date 2019/12/7
 **/
public abstract class AbstractResourcePool<T extends PooledResource> {

    protected Logger logger = LoggerFactory.getLogger(getClass());

    protected LinkedBlockingDeque<T> queue;

    private ReentrantLock createLock = new ReentrantLock();

    // 当前个数
    protected int count;

    // 最大并发数
    protected final int maxConcurrenceSize;

    public AbstractResourcePool(int maxConcurrenceSize) {
        queue = new LinkedBlockingDeque<>(maxConcurrenceSize);
        this.maxConcurrenceSize = maxConcurrenceSize;
    }

    // 获取当前连接数
    public int getCount() {
        return count;
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
        if (!isRunning()) {
            throw new RpcException("resource pool is closed");
        }
        T resource = null;
        try {
            if (0 != timeoutMilliSeconds) {
                resource = queue.pollFirst(timeoutMilliSeconds, TimeUnit.MILLISECONDS);
            } else {
                resource = queue.pollFirst();
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        if (null != resource) {
            return resource;
        } else {
            tryCreateResource();
            return getResource(5);
        }
    }

    /**
     * 释放连接
     * @param    resource
     * @author xiaoqianbin
     * @date 2019/12/7
     **/
    public void release(T resource) {
        queue.addFirst(resource);
        onNetWorkRecovered();
    }

    /**
     * 销毁连接
     * @author xiaoqianbin
     * @date 2019/12/7
     **/
    public void destroyResource() {
        try {
            createLock.lock();
            count--;
        } finally {
            createLock.unlock();
        }
    }

    /**
     * 尝试新建一个资源
     * @author xiaoqianbin
     * @date 2019/12/7
     **/
    private void tryCreateResource() {
        if (!canCreate()) {
            return;
        }
        try {
            createLock.lock();
            if (canCreate()) {
                T resource = newResource();
                release(resource);
                count++;
            }
        } catch (NetworkException e) {
            onNetWorkException(e);
        } finally {
            createLock.unlock();
        }
    }

    private boolean canCreate() {
        return count < maxConcurrenceSize;
    }

    /**
     * 网络异常处理
     * @param	e
     * @author  xiaoqianbin
     * @date    2019/12/8
     **/
    protected abstract void onNetWorkException(NetworkException e);

    /**
     * 网络恢复了
     * @author  xiaoqianbin
     * @date    2019/12/8
     **/
    protected abstract void onNetWorkRecovered();

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

    /**
     * 判断pool是否处于run的状态
     * @author  xiaoqianbin
     * @date    2019/12/8
     **/
    protected abstract boolean isRunning();

}
