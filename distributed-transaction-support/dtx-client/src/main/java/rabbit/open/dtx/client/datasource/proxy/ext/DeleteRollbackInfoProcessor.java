package rabbit.open.dtx.client.datasource.proxy.ext;

import rabbit.open.dtx.client.datasource.parser.SQLMeta;
import rabbit.open.dtx.client.datasource.proxy.RollBackRecord;
import rabbit.open.dtx.client.datasource.proxy.RollbackInfo;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * Delete回滚信息处理器
 * @author xiaoqianbin
 * @date 2019/12/4
 **/
public class DeleteRollbackInfoProcessor extends UpdateRollbackInfoProcessor {

    @Override
    protected void setPlaceHolderValues(SQLMeta sqlMeta, List<Object> preparedStatementValues, PreparedStatement stmt) throws SQLException {
        for (int i = 1; i <= preparedStatementValues.size(); i++) {
            setPreparedStatementValue(stmt, i, preparedStatementValues.get(i - 1));
        }
    }

    /**
     * 根据回滚信息还原删除操作
     * @param record
     * @param info
     * @param conn
     * @author xiaoqianbin
     * @date 2019/12/5
     **/
    @Override
    public boolean processRollbackInfo(RollBackRecord record, RollbackInfo info, Connection conn) {
        if (info.getOriginalData().isEmpty()) {
            logger.warn("distributed transaction[txGroupId --> {}, txBranchId --> {}, dataId -->{}] roll back success, no data needs to rollback", record.getTxGroupId(), record.getTxBranchId(), record.getId());
            return true;
        }
        StringBuilder sql = new StringBuilder("insert into " + info.getMeta().getTargetTables() + "(");
        for (Map.Entry<String, Object> entry : info.getOriginalData().get(0).entrySet()) {
            sql.append(entry.getKey() + ", ");
        }
        sql.deleteCharAt(sql.lastIndexOf(","));
        sql.append(") values (");
        for (int i = 0; i < info.getOriginalData().get(0).size(); i++) {
            sql.append("?, ");
        }
        sql.deleteCharAt(sql.lastIndexOf(","));
        sql.append(")");
        PreparedStatement stmt = null;
        int effectDataSize = 0;
        try {
            for (Map<String, Object> data : info.getOriginalData()) {
                stmt = conn.prepareStatement(sql.toString());
                int index = 1;
                for (Map.Entry<String, Object> entry : data.entrySet()) {
                    setPreparedStatementValue(stmt, index, entry.getValue());
                    index++;
                }
                effectDataSize = stmt.executeUpdate();
                printRollbackLog(record, sql.toString(), data.values(), effectDataSize);
                stmt.close();
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return false;
        } finally {
            safeClose(stmt);
        }
        return 0 != effectDataSize;
    }
}
