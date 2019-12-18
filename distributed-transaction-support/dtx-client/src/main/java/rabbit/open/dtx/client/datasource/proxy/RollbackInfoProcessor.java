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
import java.util.*;

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
     * @param record
     * @param info
     * @param connection
     * @return true:成功
     * @author xiaoqianbin
     * @date 2019/12/5
     **/

    public abstract boolean processRollbackInfo(RollBackRecord record, RollbackInfo info, Connection connection);

    @SuppressWarnings("rawtypes")
	protected void setPreparedStatementValue(PreparedStatement stmt, int index, Object value) throws SQLException {
        if (value instanceof byte[]) {
            stmt.setBytes(index, (byte[]) value);
        } else if (value instanceof Timestamp) {
            stmt.setTimestamp(index, (Timestamp) value);
        } else if (value instanceof Date) {
            stmt.setTimestamp(index, new Timestamp(((Date) value).getTime()));
        } else if (value instanceof Float) {
            stmt.setFloat(index, (float) value);
        } else if (value instanceof Double) {
            stmt.setDouble(index, (double) value);
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
            logger.error(e.getMessage(), e);
        }
    }

    /**
     * 打印回滚日志
     * @param record         回滚记录
     * @param sql            sql信息
     * @param preparedValues sql的值
     * @param effectDataSize 回滚操作影响的数据条数
     * @author xiaoqianbin
     * @date 2019/12/6
     **/
    protected void printRollbackLog(RollBackRecord record, String sql, Collection<Object> preparedValues, int effectDataSize) {
        if (0 == effectDataSize) {
            logger.error("distributed transaction[txGroupId --> {}, txBranchId --> {}, dataId -->{}] roll back failed: {}, \n preparedValues: {}", record.getTxGroupId(), record.getTxBranchId(), record.getId(), sql, preparedValues);
        } else {
            logger.info("distributed transaction[txGroupId --> {}, txBranchId --> {}, dataId -->{}] roll back success: {}, \n preparedValues: {}", record.getTxGroupId(), record.getTxBranchId(), record.getId(), sql, preparedValues);
        }
    }
}
