package rabbit.open.dtx.common.nio.server;

/**
 * RedisTransactionHandler 所有使用到的key都在这里
 * @author xiaoqianbin
 * @date 2020/1/1
 **/
enum RedisKeyNames {

    DTX_GLOBAL_ID("全局id键");

    RedisKeyNames(String desc) {

    }
}
