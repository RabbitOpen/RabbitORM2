package rabbit.open.dtx.client.net;

import rabbit.open.dts.common.utils.ext.KryoObjectSerializer;
import rabbit.open.dtx.client.datasource.proxy.RollBackRecord;
import rabbit.open.dtx.client.datasource.proxy.RollbackInfo;
import rabbit.open.dtx.client.datasource.proxy.RollbackInfoProcessor;
import rabbit.open.dtx.client.datasource.proxy.TxDataSource;
import rabbit.open.dtx.client.exception.DistributedTransactionException;

import javax.sql.DataSource;
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

    @Override
    public void rollback(String applicationName, Long txGroupId, Long txBranchId) {
        for (TxDataSource dataSource : TxDataSource.getDataSources()) {
            Connection conn = null;
            PreparedStatement stmt = null;
            ResultSet rs = null;
            try {
                conn = dataSource.getRealDataSource().getConnection();
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
                doRollback(records, dataSource.getRealDataSource());
            } catch (Exception e) {
                throw new DistributedTransactionException(e);
            } finally {
                safeClose(rs);
                safeClose(stmt);
                safeClose(conn);
            }
        }
    }

    private void safeClose(AutoCloseable c) {
        try {
            if (null != c) {
                c.close();
            }
        } catch (Exception e) {
            // TO DO : ignore
        }
    }

    /**
     * 逐条回滚
     * @param    records
     * @param    dataSource
     * @author xiaoqianbin
     * @date 2019/12/5
     **/
    private void doRollback(List<RollBackRecord> records, DataSource dataSource) throws SQLException {
        KryoObjectSerializer serializer = new KryoObjectSerializer();
        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            conn = dataSource.getConnection();
            for (RollBackRecord record : records) {
                RollbackInfo info = serializer.deserialize(record.getRollbackInfo(), RollbackInfo.class);
                if (RollbackInfoProcessor.getRollbackInfoProcessor(info.getMeta().getSqlType()).processRollbackInfo(record, info, conn)) {
                    stmt = conn.prepareStatement(RollBackRecord.DELETE_SQL);
                } else {
                    stmt = conn.prepareStatement(RollBackRecord.UPDATE_SQL);
                }
                stmt.setLong(1, record.getId());
                stmt.executeUpdate();
                stmt.close();
            }
        } finally {
            safeClose(stmt);
            safeClose(conn);
        }
    }

    @Override
    public void commit(String applicationName, Long txGroupId, Long txBranchId) {

    }
}
