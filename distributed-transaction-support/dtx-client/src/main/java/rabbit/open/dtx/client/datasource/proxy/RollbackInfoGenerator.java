package rabbit.open.dtx.client.datasource.proxy;

import rabbit.open.dtx.client.datasource.parser.SQLMeta;

import java.util.List;

/**
 * sql 回滚信息生成器
 * @author xiaoqianbin
 * @date 2019/12/4
 **/
public abstract class RollbackInfoGenerator {

    /**
     * @param    sqlMeta 当前预编译sql信息
     * @param    preparedStatementValues 预编译sql的值信息
     * @author xiaoqianbin
     * @date 2019/12/4
     **/
    public abstract RollbackInfo generate(SQLMeta sqlMeta, List<Object> preparedStatementValues);

}
