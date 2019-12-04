package rabbit.open.dtx.client.datasource.proxy;

import rabbit.open.dtx.client.datasource.parser.SQLMeta;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;

/**
 * sql 回滚信息生成器
 * @author xiaoqianbin
 * @date 2019/12/4
 **/
public abstract class RollbackInfoGenerator {

    /**
     * @param sqlMeta                 当前预编译sql信息
     * @param preparedStatementValues 预编译sql的值信息
     * @param txConn                  数据库链接代理对象
     * @author xiaoqianbin
     * @date 2019/12/4
     **/
    public abstract RollbackInfo generate(SQLMeta sqlMeta, List<Object> preparedStatementValues, TxConnection txConn) throws SQLException;

    protected void setPreparedStatementValue(PreparedStatement stmt, int index, Object value) throws SQLException {
        if (value instanceof byte[]) {
            stmt.setBytes(index, (byte[]) value);
        } else if (value instanceof Date) {
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

    protected RollbackInfo createRollbackInfo(SQLMeta sqlMeta, List<Object> preparedStatementValues) {
        RollbackInfo rollbackInfo = new RollbackInfo();
        rollbackInfo.setMeta(sqlMeta);
        rollbackInfo.setPreparedValues(preparedStatementValues);
        return rollbackInfo;
    }
}
