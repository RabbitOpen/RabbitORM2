package rabbit.open.dtx.client.datasource.proxy.ext;

import org.springframework.util.StringUtils;
import rabbit.open.dtx.client.datasource.parser.ColumnMeta;
import rabbit.open.dtx.client.datasource.parser.SQLMeta;
import rabbit.open.dtx.client.datasource.proxy.RollBackRecord;
import rabbit.open.dtx.client.datasource.proxy.RollbackInfo;
import rabbit.open.dtx.client.datasource.proxy.RollbackInfoProcessor;
import rabbit.open.dtx.client.datasource.proxy.TxConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 更新回滚信息处理器
 * @author xiaoqianbin
 * @date 2019/12/4
 **/
public class UpdateRollbackInfoProcessor extends RollbackInfoProcessor {

    @Override
    public RollbackInfo generateRollbackInfo(SQLMeta sqlMeta, List<Object> preparedStatementValues, TxConnection txConn) throws SQLException {
        RollbackInfo rollbackInfo = createRollbackInfo(sqlMeta, preparedStatementValues);
        String sql = "select * from " + sqlMeta.getTargetTables() + " " + sqlMeta.getCondition();
        PreparedStatement stmt = null;
        ResultSet resultSet = null;
        try {
            stmt = txConn.getRealConn().prepareStatement(sql);
            setPlaceHolderValues(sqlMeta, preparedStatementValues, stmt);
            resultSet = stmt.executeQuery();
            ResultSetMetaData metaData = resultSet.getMetaData();
            List<Map<String, Object>> list = new ArrayList<>();
            while (resultSet.next()) {
                Map<String, Object> row = new HashMap<>();
                for (int i = 1; i <= metaData.getColumnCount(); i++) {
                    String columnName = metaData.getColumnName(i);
                    Object value = resultSet.getObject(i);
                    row.put(columnName, value);
                }
                list.add(row);
            }
            rollbackInfo.setOriginalData(list);
            resultSet.close();
        } finally {
            safeClose(resultSet);
            safeClose(stmt);
        }
        return rollbackInfo;
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
        if (info.getOriginalData().isEmpty()) {
            return true;
        }
        String sql = createUpdateSql(info);
        PreparedStatement stmt = null;
        List<Object> preparedValues = new ArrayList<>();
        try {
            for (Map<String, Object> data : info.getOriginalData()) {
                stmt = conn.prepareStatement(sql);
                // 给set字段设值
                preparedValues.addAll(getValueFields(data));
                preparedValues.addAll(getConditionPlaceHolderValues(info));
                // 获取原始sql更新字段转换成过滤条件后的值
                preparedValues.addAll(getOriginalFieldValues(info));
                for (int i = 0; i < preparedValues.size(); i++) {
                    setPreparedStatementValue(stmt, i + 1, preparedValues.get(i));
                }
                int effectDataSize = stmt.executeUpdate();
                if (0 == effectDataSize) {
                    logger.error("transaction[txGroupId --> {}, txBranchId --> {}, dataId -->{}] roll back failed: {}, \n preparedValues: {}", record.getTxGroupId(), record.getTxBranchId(), record.getId(), sql, preparedValues);
                } else {
                    logger.info("transaction[txGroupId --> {}, txBranchId --> {}, dataId -->{}] roll back success: {}, \n preparedValues: {}", record.getTxGroupId(), record.getTxBranchId(), record.getId(), sql, preparedValues);
                }
                stmt.close();
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return false;
        } finally {
            safeClose(stmt);
        }
        return true;
    }

    // 获取原始sql更新字段转换成过滤条件后的值
    private List<Object> getOriginalFieldValues(RollbackInfo info) {
        List<Object> list = new ArrayList<>();
        for (ColumnMeta column : info.getMeta().getColumns()) {
            if ("?".equals(column.getValue())) {
                list.add(info.getPreparedValues().get(column.getPlaceHolderIndex()));
            }
        }
        return list;
    }

    // 读取原始过滤条件的占位符(?)上的值
    private List<Object> getConditionPlaceHolderValues(RollbackInfo info) {
        List<Object> list = new ArrayList<>();
        int lastIndex = getConditionPlaceholderStartIndex(info.getMeta());
        // 添加原始过滤条件的值
        for (int i = lastIndex + 1; i < info.getPreparedValues().size(); i++) {
            list.add(info.getPreparedValues().get(i));
        }
        return list;
    }

    //原始sql中条件字段中的'?'在所有'?'中的位置
    private int getConditionPlaceholderStartIndex(SQLMeta meta) {
        int lastIndex = 0;
        for (ColumnMeta column : meta.getColumns()) {
            if (column.getPlaceHolderIndex() > lastIndex) {
                lastIndex = column.getPlaceHolderIndex();
            }
        }
        return lastIndex;
    }

    private List<Object> getValueFields(Map<String, Object> data) {
        List<Object> list = new ArrayList<>();
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            list.add(entry.getValue());
        }
        return list;
    }

    private String createUpdateSql(RollbackInfo info) {
        SQLMeta meta = info.getMeta();
        List<Map<String, Object>> originalData = info.getOriginalData();
        StringBuilder sql = new StringBuilder("update " + meta.getTargetTables() + " set ");
        if (!originalData.isEmpty()) {
            for (String c : originalData.get(0).keySet()) {
                sql.append(c + " = ?, ");
            }
            sql.deleteCharAt(sql.lastIndexOf(","));
            if (StringUtils.isEmpty(meta.getCondition())) {
                sql.append(" where 1 = 1 ");
            } else {
                sql.append(meta.getCondition());
            }
            for (ColumnMeta column : meta.getColumns()) {
                sql.append(" and " + column.getColumnName() + " = " + column.getValue());
            }
        }
        return sql.toString();
    }

    /***
     * 设置占位符的值
     * @param    sqlMeta
     * @param    preparedStatementValues
     * @param    stmt
     * @author xiaoqianbin
     * @date 2019/12/4
     **/
    protected void setPlaceHolderValues(SQLMeta sqlMeta, List<Object> preparedStatementValues, PreparedStatement stmt) throws SQLException {
        int index = getConditionPlaceholderStartIndex(sqlMeta);
        for (int i = 1; i < preparedStatementValues.size() - index; i++) {
            setPreparedStatementValue(stmt, i, preparedStatementValues.get(index + i));
        }
    }

}
