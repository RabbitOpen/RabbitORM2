package rabbit.open.dtx.client.datasource.proxy.ext;

import rabbit.open.dtx.client.datasource.parser.ColumnMeta;
import rabbit.open.dtx.client.datasource.parser.SQLMeta;
import rabbit.open.dtx.client.datasource.proxy.RollbackInfo;
import rabbit.open.dtx.client.datasource.proxy.RollbackInfoGenerator;
import rabbit.open.dtx.client.datasource.proxy.TxConnection;

import java.sql.*;
import java.util.*;
import java.util.Date;

/**
 * 更新回滚信息生成器
 * @author xiaoqianbin
 * @date 2019/12/4
 **/
public class UpdateRollbackInfoGenerator extends RollbackInfoGenerator {

    @Override
    public RollbackInfo generate(SQLMeta sqlMeta, List<Object> preparedStatementValues, TxConnection txConn) throws SQLException {
        RollbackInfo rollbackInfo = new RollbackInfo();
        rollbackInfo.setMeta(sqlMeta);
        rollbackInfo.setPreparedValues(preparedStatementValues);
        String sql = "select * from " + sqlMeta.getTargetTables() + " " + sqlMeta.getCondition();
        PreparedStatement stmt = null;
        try {
            stmt = txConn.getRealConn().prepareStatement(sql);
            setPlaceHolderValues(sqlMeta, preparedStatementValues, stmt);
            ResultSet resultSet = stmt.executeQuery();
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
            if (null != stmt) {
                stmt.close();
            }
        }
        return rollbackInfo;
    }

    /***
     * 设置占位符的值
     * @param    sqlMeta
     * @param    preparedStatementValues
     * @param    stmt
     * @author xiaoqianbin
     * @date 2019/12/4
     **/
    private void setPlaceHolderValues(SQLMeta sqlMeta, List<Object> preparedStatementValues, PreparedStatement stmt) throws SQLException {
        int index = 0;
        for (ColumnMeta column : sqlMeta.getColumns()) {
            if (column.getPlaceHolderIndex() > index) {
                index = column.getPlaceHolderIndex();
            }
        }
        for (int i = 1; i < preparedStatementValues.size() - index; i++) {
            setPreparedStatementValue(stmt, i, preparedStatementValues.get(index + i));
        }
    }

    private void setPreparedStatementValue(PreparedStatement stmt, int index, Object value) throws SQLException {
        if (value instanceof byte[]) {
            stmt.setBytes(index, (byte[])value);
        }
        if (value instanceof Date) {
            stmt.setTimestamp(index, new Timestamp(((Date) value).getTime()));
        } else if (value instanceof Float) {
            stmt.setFloat(index, (float) value);
        } else if (value instanceof Double) {
            stmt.setDouble(index, (double) value);
        } else if (value instanceof Enum) {
            stmt.setString(index, ((Enum) value).name());
        } else {
            stmt.setObject(index, value);
        }
    }

}
