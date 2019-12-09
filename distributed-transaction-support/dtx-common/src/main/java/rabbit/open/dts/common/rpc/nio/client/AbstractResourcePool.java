package rabbit.open.dts.common.rpc.nio.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rabbit.open.dts.common.rpc.nio.exception.NetworkException;
import rabbit.open.dts.common.rpc.nio.exception.RpcException;

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

    protected int maxSize;

    public AbstractResourcePool(int maxSize) {
        queue = new LinkedBlockingDeque<>(maxSize);
        this.maxSize = maxSize;
    }

    /**
     * 获取一个资源
     * @param    timeoutMills 超时时间
     * @author xiaoqianbin
     * @date 2019/12/7
     **/
    public T getResource(int timeoutMills) {
        if (!isRunning()) {
            throw new RpcException("resource pool is closed");
        }
        T resource = null;
        try {
            if (0 != timeoutMills) {
                resource = queue.pollFirst(timeoutMills, TimeUnit.MILLISECONDS);
            } else {
                resource = queue.pollFirst();
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        if (null != resource) {
            return resource;
        } else {
            return tryNewResource(50);
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
     * @param    timeout
     * @author xiaoqianbin
     * @date 2019/12/7
     **/
    private T tryNewResource(int timeout) {
        try {
            createLock.lock();
            if (count >= maxSize) {
                return getResource(timeout);
            }
            T resource = createResource();
            onNetWorkRecovered();
            count++;
            return resource;
        } catch (NetworkException e) {
            onNetWorkException(e);
            return getResource(timeout);
        } finally {
            createLock.unlock();
        }
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
    protected abstract T createResource();

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
