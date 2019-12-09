package rabbit.open.dts.common.rpc.nio.client;

/**
 * 可池化的资源
 * @author xiaoqianbin
 * @date 2019/12/8
 **/
public interface PooledResource {

    /**
     * 销毁资源
     * @author  xiaoqianbin
     * @date    2019/12/8
     **/
    void destroy();

    /**
     * 释放资源
     * @author  xiaoqianbin
     * @date    2019/12/8
     **/
    void release();
}
