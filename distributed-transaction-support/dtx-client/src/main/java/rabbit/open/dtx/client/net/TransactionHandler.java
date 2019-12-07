package rabbit.open.dtx.client.net;

/**
 * 事务消息处理接口
 * @author xiaoqianbin
 * @date 2019/12/5
 **/
public interface TransactionHandler {

    /**
     * 提交事务分支
     * @param	txGroupId
	 * @param	txBranchId
	 * @param	applicationName
     * @author  xiaoqianbin
     * @date    2019/12/5
     **/
    void doBranchCommit(Long txGroupId, Long txBranchId, String applicationName);

    /**
     * 提交整个事务
     * @param	txGroupId   事务组id
     * @param	txBranchId  当前分支id
     * @author  xiaoqianbin
     * @date    2019/12/5
     **/
    void doCommit(Long txGroupId, Long txBranchId);

    /**
     * 回滚整个事务
     * @param	txGroupId   事务组id
     * @author  xiaoqianbin
     * @date    2019/12/5
     **/
    void doRollback(Long txGroupId);

    /**
     * 获取{txGroupId}下的事务分支id
     * @param	txGroupId       事务组id
	 * @param	applicationName 应用名
     * @author  xiaoqianbin
     * @date    2019/12/5
     **/
    Long getTransactionBranchId(Long txGroupId, String applicationName);

    /**
     * 获取事务组id
     * @author  xiaoqianbin
     * @date    2019/12/7
     **/
    Long getTransactionGroupId();

}
