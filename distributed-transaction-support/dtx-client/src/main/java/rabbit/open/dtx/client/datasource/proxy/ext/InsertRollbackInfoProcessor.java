package rabbit.open.dtx.client.datasource.proxy.ext;

import rabbit.open.dtx.client.datasource.parser.ColumnMeta;
import rabbit.open.dtx.client.datasource.parser.SQLMeta;
import rabbit.open.dtx.client.datasource.proxy.RollBackRecord;
import rabbit.open.dtx.client.datasource.proxy.RollbackInfo;
import rabbit.open.dtx.client.datasource.proxy.RollbackInfoProcessor;
import rabbit.open.dtx.client.datasource.proxy.TxConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
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

    /**
     * 根据回滚信息还原更新操作
     * @param record
     * @param info
     * @param conn
     * @author xiaoqianbin
     * @date 2019/12/5
     **/
    @Override
    public boolean processRollbackInfo(RollBackRecord record, RollbackInfo info, Connection conn) {
        StringBuilder sql = new StringBuilder("delete from " + info.getMeta().getTargetTables() + " where 1 = 1");
        for (ColumnMeta column : info.getMeta().getColumns()) {
            sql.append(" and " + column.getColumnName() + " = " + column.getValue());
        }
        PreparedStatement stmt = null;
        try {
            stmt = conn.prepareStatement(sql.toString());
            for (int i = 0; i < info.getPreparedValues().size(); i++) {
                setPreparedStatementValue(stmt, i + 1, info.getPreparedValues().get(i));
            }
            printRollbackLog(record, sql.toString(), info.getPreparedValues(), stmt.executeUpdate());
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return false;
        } finally {
            safeClose(stmt);
        }
        return true;
    }

}
