package rabbit.open.dtx.client.datasource.proxy.ext;

import rabbit.open.dtx.client.datasource.parser.ColumnMeta;
import rabbit.open.dtx.client.datasource.parser.SQLMeta;
import rabbit.open.dtx.client.datasource.proxy.RollbackInfo;
import rabbit.open.dtx.client.datasource.proxy.RollbackInfoGenerator;
import rabbit.open.dtx.client.datasource.proxy.TxConnection;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 更新回滚信息生成器
 * @author xiaoqianbin
 * @date 2019/12/4
 **/
public class UpdateRollbackInfoGenerator extends RollbackInfoGenerator {

    @Override
    public RollbackInfo generate(SQLMeta sqlMeta, List<Object> preparedStatementValues, TxConnection txConn) throws SQLException {
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
            if (null != resultSet) {
                resultSet.close();
            }
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
    protected void setPlaceHolderValues(SQLMeta sqlMeta, List<Object> preparedStatementValues, PreparedStatement stmt) throws SQLException {
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



}
