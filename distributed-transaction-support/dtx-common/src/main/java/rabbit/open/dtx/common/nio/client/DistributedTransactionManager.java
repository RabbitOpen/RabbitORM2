package rabbit.open.dtx.common.nio.client;

import java.io.Serializable;
import java.lang.reflect.Method;

/**
 * 分布式事务管理器
 * @author xiaoqianbin
 * @date 2019/12/4
 **/
public interface DistributedTransactionManager extends Serializable {

    /**
     * 开启事务
     * @param method 开启事务的方法
     * @author xiaoqianbin
     * @date 2019/12/4
     **/
    void beginTransaction(Method method);

    /**
     * 回滚事务
     * @param method         开启事务的方法
     * @param timeoutSeconds 回滚超时时间
     * @author xiaoqianbin
     * @date 2019/12/4
     **/
    void rollback(Method method, long timeoutSeconds);

    /**
     * 提交事务
     * @param method 开启事务的方法
     * @author xiaoqianbin
     * @date 2019/12/4
     **/
    void commit(Method method);

    /**
     * 判断当前业务是否处于事务开启状态
     * @param method 开启事务的方法
     * @author xiaoqianbin
     * @date 2019/12/5
     **/
    boolean isTransactionOpen(Method method);

    /**
     * 获取事务对象
     * @author xiaoqianbin
     * @date 2019/12/5
     **/
    DistributedTransactionObject getCurrentTransactionObject();

    /**
     * 获取一个事务分支id
     * @author xiaoqianbin
     * @date 2019/12/5
     **/
    Long getTransactionBranchId();

    /**
     * 获取事务组id
     * @author xiaoqianbin
     * @date 2019/12/7
     **/
    Long getTransactionGroupId();

    /**
     * 获取app名字
     * @author xiaoqianbin
     * @date 2019/12/5
     **/
    String getApplicationName();

}
