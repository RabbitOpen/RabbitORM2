package rabbit.open.dtx.client.datasource.proxy.ext;

import rabbit.open.dtx.client.datasource.parser.SQLMeta;
import rabbit.open.dtx.client.datasource.proxy.RollbackInfo;
import rabbit.open.dtx.client.datasource.proxy.RollbackInfoGenerator;

import java.util.List;

/**
 * 新增操作回滚数据生成器
 * @author xiaoqianbin
 * @date 2019/12/4
 **/
public class InsertRollbackInfoGenerator extends RollbackInfoGenerator {

    @Override
    public RollbackInfo generate(SQLMeta sqlMeta, List<Object> preparedStatementValues) {
        RollbackInfo rollbackInfo = new RollbackInfo();

        return rollbackInfo;
    }
}
