package rabbit.open.dtx.client.datasource.proxy.ext;

import rabbit.open.dtx.client.datasource.parser.SQLMeta;
import rabbit.open.dtx.client.datasource.proxy.RollbackInfo;
import rabbit.open.dtx.client.datasource.proxy.RollbackInfoGenerator;
import rabbit.open.dtx.client.datasource.proxy.TxConnection;

import java.util.List;

/**
 * Delete回滚信息生成器
 * @author xiaoqianbin
 * @date 2019/12/4
 **/
public class DeleteRollbackInfoGenerator extends RollbackInfoGenerator {

    @Override
    public RollbackInfo generate(SQLMeta sqlMeta, List<Object> preparedStatementValues, TxConnection txConn) {
        return null;
    }

}
