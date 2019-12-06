package rabbit.open.dtx.client.datasource.proxy.ext;

import rabbit.open.dtx.client.datasource.parser.SQLMeta;
import rabbit.open.dtx.client.datasource.proxy.RollBackRecord;
import rabbit.open.dtx.client.datasource.proxy.RollbackInfo;
import rabbit.open.dtx.client.datasource.proxy.RollbackInfoProcessor;
import rabbit.open.dtx.client.datasource.proxy.TxConnection;

import java.sql.Connection;
import java.util.List;

/**
 * Insert回滚信息生成器
 * @author xiaoqianbin
 * @date 2019/12/4
 **/
public class InsertRollbackInfoProcessor extends RollbackInfoProcessor {

    @Override
    public RollbackInfo generateRollbackInfo(SQLMeta sqlMeta, List<Object> preparedStatementValues, TxConnection txConn) {
        return new RollbackInfo(sqlMeta, preparedStatementValues);
    }

    @Override
    public boolean processRollbackInfo(RollBackRecord record, RollbackInfo info, Connection connection) {
        return true;
    }

}
