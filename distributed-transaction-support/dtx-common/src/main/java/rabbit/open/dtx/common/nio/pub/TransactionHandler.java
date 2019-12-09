package rabbit.open.dtx.common.nio.pub;

import rabbit.open.dtx.common.spring.anno.Namespace;

/**
 * 事务消息处理接口
 * @author xiaoqianbin
 * @date 2019/12/5
 **/
@Namespace("dtxTransactionHandler")
public interface TransactionHandler {

    /**
     * 提交事务分支
     * @param	txGroupId
	 * @param	txBranchId
	 * @param	applicationName
     * @author  xiaoqianbin
     * @date    2019/12/5
     **/
    default void doBranchCommit(Long txGroupId, Long txBranchId, String applicationName) {}

    /**
     * 提交整个事务
     * @param	txGroupId   事务组id
     * @param	txBranchId  当前分支id
     * @author  xiaoqianbin
     * @date    2019/12/5
     **/
    default void doCommit(Long txGroupId, Long txBranchId) {}

    /**
     * 回滚整个事务
     * @param	txGroupId   事务组id
     * @author  xiaoqianbin
     * @date    2019/12/5
     **/
    default void doRollback(Long txGroupId) {}

    /**
     * 获取{txGroupId}下的事务分支id
     * @param	txGroupId       事务组id
	 * @param	applicationName 应用名
     * @author  xiaoqianbin
     * @date    2019/12/5
     **/
    default Long getTransactionBranchId(Long txGroupId, String applicationName) {
        return 0L;
    }

    /**
     * 获取事务组id
     * @author  xiaoqianbin
     * @date    2019/12/7
     **/
    default Long getTransactionGroupId() {
        return 0L;
    }

}
