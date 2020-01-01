package rabbit.open.dtx.common.nio.server;

/**
 * RedisTransactionHandler 所有使用到的key都在这里
 * @author xiaoqianbin
 * @date 2020/1/1
 **/
enum RedisKeyNames {

    DTX_GROUP_ID_("group id key前缀"),
    GROUP_INFO("组信息字段"),
    BRANCH_INFO("分支信息字段"),
    ROLLBACK_CONTEXT("回滚时的上下文信息"),
    DTX_GLOBAL_ID("全局id键");

    RedisKeyNames(String desc) {

    }
}
