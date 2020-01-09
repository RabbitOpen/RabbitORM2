package rabbit.open.dtx.client.net;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rabbit.open.dtx.client.datasource.proxy.RollBackRecord;
import rabbit.open.dtx.client.datasource.proxy.RollbackInfo;
import rabbit.open.dtx.client.datasource.proxy.RollbackInfoProcessor;
import rabbit.open.dtx.client.datasource.proxy.TxDataSource;
import rabbit.open.dtx.common.exception.DistributedTransactionException;
import rabbit.open.dtx.common.utils.ext.KryoObjectSerializer;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * 事务消息回调处理类
 * @author xiaoqianbin
 * @date 2019/12/5
 **/
public class TransactionMessageHandler implements MessageHandler {

    private Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * 处理提交
     * @param applicationName
     * @param txGroupId
     * @param txBranchId
     * @author xiaoqianbin
     * @date 2019/12/6
     **/
    @Override
    public boolean rollback(String applicationName, Long txGroupId, Long txBranchId) {
        return doTransaction(applicationName, txGroupId, txBranchId, this::doRollback);
    }

    private boolean doTransaction(String applicationName, Long txGroupId, Long txBranchId, CallBack callBack) {
        boolean result = true;
        for (TxDataSource dataSource : TxDataSource.getDataSources()) {
            Connection conn = null;
            PreparedStatement stmt = null;
            ResultSet rs = null;
            try {
                conn = dataSource.getRealDataSource().getConnection();
                if (conn.getAutoCommit()) {
                    conn.setAutoCommit(false);
                }
                stmt = conn.prepareStatement(RollBackRecord.QUERY_SQL);
                stmt.setLong(1, txGroupId);
                stmt.setLong(2, txBranchId);
                stmt.setString(3, applicationName);
                stmt.setString(4, dataSource.getDataSourceName());
                rs = stmt.executeQuery();
                List<RollBackRecord> records = new ArrayList<>();
                while (rs.next()) {
                    RollBackRecord info = new RollBackRecord();
                    info.setId(rs.getLong(RollBackRecord.PRIMARY_KEY));
                    info.setRollbackInfo(rs.getBytes(RollBackRecord.ROLLBACK_INFO));
                    info.setTxGroupId(rs.getLong(RollBackRecord.TX_GROUP_ID));
                    info.setTxBranchId(rs.getLong(RollBackRecord.TX_BRANCH_ID));
                    records.add(info);
                }
                result = callBack.call(records, conn);
                conn.commit();
            } catch (Exception e) {
                rollback(conn);
                throw new DistributedTransactionException(e);
            } finally {
                safeClose(rs);
                safeClose(stmt);
                safeClose(conn);
            }
        }
        return result;
    }

    private void rollback(Connection conn) {
        if (null == conn) {
            return;
        }
        try {
            conn.rollback();
        } catch (SQLException e) {
            logger.error(e.getMessage(), e);
        }
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
     * 逐条回滚
     * @param records
     * @param conn
     * @author xiaoqianbin
     * @date 2019/12/5
     **/
    private boolean doRollback(List<RollBackRecord> records, Connection conn) throws SQLException {
        KryoObjectSerializer serializer = new KryoObjectSerializer();
        PreparedStatement stmt = null;
        try {
            boolean result = true;
            for (RollBackRecord record : records) {
                RollbackInfo info = serializer.deserialize(record.getRollbackInfo(), RollbackInfo.class);
                if (RollbackInfoProcessor.getRollbackInfoProcessor(info.getMeta().getSqlType()).processRollbackInfo(record, info, conn)) {
                    stmt = conn.prepareStatement(RollBackRecord.DELETE_SQL);
                    result = false;
                } else {
                    stmt = conn.prepareStatement(RollBackRecord.UPDATE_SQL);
                }
                stmt.setLong(1, record.getId());
                stmt.executeUpdate();
                stmt.close();
            }
            return result;
        } finally {
            safeClose(stmt);
        }
    }

    /**
     * 提交事务，直接删除回滚信息
     * @param applicationName
     * @param txGroupId
     * @param txBranchId
     * @author xiaoqianbin
     * @date 2019/12/6
     **/
    @Override
    public boolean commit(String applicationName, Long txGroupId, Long txBranchId) {
        return doTransaction(applicationName, txGroupId, txBranchId, (records, conn) -> {
            PreparedStatement stmt = null;
            try {
                for (RollBackRecord record : records) {
                    stmt = conn.prepareStatement(RollBackRecord.DELETE_SQL);
                    stmt.setLong(1, record.getId());
                    stmt.executeUpdate();
                    stmt.close();
                }
            } finally {
                safeClose(stmt);
            }
            return true;
        });
    }

    // 回调接口
    @FunctionalInterface
    interface CallBack {
        boolean call(List<RollBackRecord> records, Connection conn) throws SQLException;
    }
}
