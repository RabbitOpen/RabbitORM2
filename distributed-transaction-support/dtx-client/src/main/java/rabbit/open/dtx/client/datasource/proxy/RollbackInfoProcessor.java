package rabbit.open.dtx.client.datasource.proxy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rabbit.open.dtx.client.datasource.parser.SQLMeta;
import rabbit.open.dtx.client.datasource.parser.SQLType;
import rabbit.open.dtx.client.datasource.proxy.ext.DeleteRollbackInfoProcessor;
import rabbit.open.dtx.client.datasource.proxy.ext.InsertRollbackInfoProcessor;
import rabbit.open.dtx.client.datasource.proxy.ext.UpdateRollbackInfoProcessor;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * sql 回滚信息处理器
 * @author xiaoqianbin
 * @date 2019/12/4
 **/
public abstract class RollbackInfoProcessor {

    protected Logger logger = LoggerFactory.getLogger(getClass());

    private static Map<SQLType, RollbackInfoProcessor> processorMap = new EnumMap<>(SQLType.class);

    /**
     * @param sqlMeta                 当前预编译sql信息
     * @param preparedStatementValues 预编译sql的值信息
     * @param txConn                  数据库链接代理对象
     * @author xiaoqianbin
     * @date 2019/12/4
     **/
    public abstract RollbackInfo generateRollbackInfo(SQLMeta sqlMeta, List<Object> preparedStatementValues, TxConnection txConn) throws SQLException;

    /**
     * 处理回滚消息
     * @param	record
     * @param	info
     * @param	connection
     * @author  xiaoqianbin
     * @date    2019/12/5
     * @return  true:成功
     **/

    public abstract boolean processRollbackInfo(RollBackRecord record, RollbackInfo info, Connection connection);

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

    public static RollbackInfoProcessor getRollbackInfoProcessor(SQLType type) {
        if (!processorMap.containsKey(type)) {
            synchronized (RollbackInfoProcessor.class) {
                if (processorMap.containsKey(type)) {
                    return processorMap.get(type);
                }
                processorMap.put(SQLType.DELETE, new DeleteRollbackInfoProcessor());
                processorMap.put(SQLType.INSERT, new InsertRollbackInfoProcessor());
                processorMap.put(SQLType.UPDATE, new UpdateRollbackInfoProcessor());
            }
        }
        return processorMap.get(type);
    }

    protected void safeClose(AutoCloseable c) {
        try {
            if (null != c) {
                c.close();
            }
        } catch (Exception e) {
            // TO DO : ignore
        }
    }
}
