package rabbit.open.dtx.client.datasource.proxy.ext;

import org.springframework.util.StringUtils;
import rabbit.open.dtx.client.datasource.parser.ColumnMeta;
import rabbit.open.dtx.client.datasource.parser.SQLMeta;
import rabbit.open.dtx.client.datasource.proxy.RollBackRecord;
import rabbit.open.dtx.client.datasource.proxy.RollbackInfo;
import rabbit.open.dtx.client.datasource.proxy.RollbackInfoProcessor;
import rabbit.open.dtx.client.datasource.proxy.TxConnection;
import rabbit.open.dtx.common.annotation.Isolation;
import rabbit.open.dtx.common.context.DistributedTransactionContext;
import rabbit.open.dtx.common.exception.IsolationException;
import rabbit.open.dtx.common.nio.client.DistributedTransactionObject;
import rabbit.open.dtx.common.nio.client.ext.AbstractTransactionManager;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 更新回滚信息处理器
 * @author xiaoqianbin
 * @date 2019/12/4
 **/
public class UpdateRollbackInfoProcessor extends RollbackInfoProcessor {

    @Override
    public RollbackInfo generateRollbackInfo(SQLMeta sqlMeta, List<Object> preparedStatementValues, TxConnection txConn) throws SQLException {
        RollbackInfo rollbackInfo = createRollbackInfo(sqlMeta, preparedStatementValues);
        PreparedStatement stmt = null;
        ResultSet resultSet = null;
        try {
            stmt = txConn.getRealConn().prepareStatement(getPreImageSql(sqlMeta, txConn));
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
            txConn.getTxDataSource().getDataSourceName();
            lockDataByIsolation(txConn, getLocks(sqlMeta, txConn, list));
        } finally {
            safeClose(resultSet);
            safeClose(stmt);
        }
        return rollbackInfo;
    }

    /**
     * 生成锁id     {dataSourceName}-{tableName}-{rowId}
     * @param    sqlMeta
     * @param    txConn
     * @param    list
     * @author xiaoqianbin
     * @date 2019/12/23
     **/
    private List<String> getLocks(SQLMeta sqlMeta, TxConnection txConn, List<Map<String, Object>> list) {
        return list.stream().map(m -> {
            String primaryKeyName = txConn.getTxDataSource().getPrimaryKey(sqlMeta.getTargetTables(), txConn.getRealConn());
            return new StringBuilder(sqlMeta.getTargetTables()).append("-")
                    .append(m.get(primaryKeyName)).toString();
        }).collect(Collectors.toList());
    }

    /**
     * 根据隔离级别进行锁数据
     * @param txConn 当前连接
     * @param ids    主键
     * @author xiaoqianbin
     * @date 2019/12/23
     **/
    private void lockDataByIsolation(TxConnection txConn, List<String> ids) {
        if (!isStrictIsolation()) {
            return;
        }
        AbstractTransactionManager transactionManager = (AbstractTransactionManager) txConn.getTxDataSource().getTransactionManger();
        DistributedTransactionObject tranObj = DistributedTransactionContext.getDistributedTransactionObject();
        Long txGroupId = tranObj.getTxGroupId();
        Long branchId = transactionManager.getTransactionBranchId();
        logger.debug("{} try to lock data {} in dtx transaction [{}-{}]", transactionManager.getApplicationName(), ids, txGroupId, branchId);
        // 网络io锁数据
        transactionManager.getTransactionHandler().lockData(transactionManager.getApplicationName(), txGroupId, branchId, ids);
        logger.debug("{} locked data {} in dtx transaction [{}-{}]", transactionManager.getApplicationName(), ids, txGroupId, branchId);
    }

    /**
     * 生成查询前镜像的sql语句
     * @param sqlMeta
     * @param txConn
     * @author xiaoqianbin
     * @date 2019/12/23
     **/
    protected String getPreImageSql(SQLMeta sqlMeta, TxConnection txConn) throws SQLException {
        if (isStrictIsolation()) {
            if (txConn.getAutoCommit()) {
                throw new IsolationException();
            } else {
                return createPreImageSql(sqlMeta).append(" for update ").toString();
            }
        } else {
            return createPreImageSql(sqlMeta).toString();
        }
    }

    /**
     * 判断是不是严格的隔离级别设置(是否需要隔离数据，是否需要加锁)
     * @author xiaoqianbin
     * @date 2019/12/23
     **/
    private boolean isStrictIsolation() {
        return Isolation.LOCK == DistributedTransactionContext.getDistributedTransactionObject().getIsolation();
    }

    protected StringBuilder createPreImageSql(SQLMeta sqlMeta) {
        return new StringBuilder("select * from ").append(sqlMeta.getTargetTables()).append(" ").append(sqlMeta.getCondition());
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
            logger.warn("distributed transaction[txGroupId --> {}, txBranchId --> {}, dataId -->{}] roll back success, no data needs to rollback", record.getTxGroupId(), record.getTxBranchId(), record.getId());
            return true;
        }
        String sql = createRollbackUpdateSql(info);
        PreparedStatement stmt = null;
        List<Object> preparedValues = new ArrayList<>();
        int effectDataSize = 0;
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
                effectDataSize = stmt.executeUpdate();
                printRollbackLog(record, sql, preparedValues, effectDataSize);
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

    /**
     * 创建回滚时的update sql信息
     * @param info
     * @author xiaoqianbin
     * @date 2019/12/6
     **/
    private String createRollbackUpdateSql(RollbackInfo info) {
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
