package rabbit.open.dtx.client.datasource.proxy;

import rabbit.open.dtx.client.context.DistributedTransactionObject;
import rabbit.open.dtx.client.datasource.parser.SQLStructure;

import java.util.List;

/**
 * sql 回滚信息生成器
 * @author xiaoqianbin
 * @date 2019/12/4
 **/
public abstract class RollbackInfoGenerator {

    /**
     * @param    context 分布式事务上下文信息
     * @param    sqlStructure 当前预编译sql信息
     * @param    preparedStatementValues 预编译sql的值信息
     * @author xiaoqianbin
     * @date 2019/12/4
     **/
    public abstract RollbackInfo generate(DistributedTransactionObject context, SQLStructure sqlStructure, List<Object> preparedStatementValues);

}
