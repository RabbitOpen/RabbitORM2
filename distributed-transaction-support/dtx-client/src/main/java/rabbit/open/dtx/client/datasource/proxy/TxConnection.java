package rabbit.open.dtx.client.datasource.proxy;

import java.sql.*;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executor;

/**
 * connection proxy
 * @author xiaoqianbin
 * @date 2019/12/2
 **/
public class TxConnection implements Connection {

    private Connection realConn;

    private TxDataSource txDataSource;

    public TxConnection(Connection realConn, TxDataSource txDataSource) {
        this.realConn = realConn;
        this.txDataSource = txDataSource;
    }

    public TxDataSource getTxDataSource() {
        return txDataSource;
    }

    @Override
    public Statement createStatement() throws SQLException {
        return realConn.createStatement();
    }

    @Override
    public PreparedStatement prepareStatement(String sql) throws SQLException {
        PreparedStatement realStmt = realConn.prepareStatement(sql);
        return getTxPreparedStatement(realStmt, sql);
    }

    /**
     * 创建一个代理的PreparedStatement对象
     * @param	realStmt
     * @author  xiaoqianbin
     * @date    2019/12/3
     **/
    private TxPreparedStatement getTxPreparedStatement(PreparedStatement realStmt, String preparedSql) {
        return new TxPreparedStatement(realStmt, this, preparedSql);
    }

    @Override
    public CallableStatement prepareCall(String sql) throws SQLException {
        return realConn.prepareCall(sql);
    }

    @Override
    public String nativeSQL(String sql) throws SQLException {
        return realConn.nativeSQL(sql);
    }

    @Override
    public void setAutoCommit(boolean autoCommit) throws SQLException {
        realConn.setAutoCommit(autoCommit);
    }

    @Override
    public boolean getAutoCommit() throws SQLException {
        return realConn.getAutoCommit();
    }

    @Override
    public void commit() throws SQLException {
        realConn.commit();
    }

    @Override
    public void rollback() throws SQLException {
        realConn.rollback();
    }

    @Override
    public void close() throws SQLException {
        realConn.close();
    }

    @Override
    public boolean isClosed() throws SQLException {
        return realConn.isClosed();
    }

    @Override
    public DatabaseMetaData getMetaData() throws SQLException {
        return realConn.getMetaData();
    }

    @Override
    public void setReadOnly(boolean readOnly) throws SQLException {
        realConn.setReadOnly(readOnly);
    }

    @Override
    public boolean isReadOnly() throws SQLException {
        return realConn.isReadOnly();
    }

    @Override
    public void setCatalog(String catalog) throws SQLException {
        realConn.setCatalog(catalog);
    }

    @Override
    public String getCatalog() throws SQLException {
        return realConn.getCatalog();
    }

    @Override
    public void setTransactionIsolation(int level) throws SQLException {
        realConn.setTransactionIsolation(level);
    }

    @Override
    public int getTransactionIsolation() throws SQLException {
        return realConn.getTransactionIsolation();
    }

    @Override
    public SQLWarning getWarnings() throws SQLException {
        return realConn.getWarnings();
    }

    @Override
    public void clearWarnings() throws SQLException {
        realConn.close();
    }

    @Override
    public Statement createStatement(int resultSetType, int resultSetConcurrency) throws SQLException {
        return realConn.createStatement(resultSetType, resultSetConcurrency);
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
        PreparedStatement statement = realConn.prepareStatement(sql, resultSetType, resultSetConcurrency);
        return getTxPreparedStatement(statement, sql);
    }

    @Override
    public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
        return realConn.prepareCall(sql, resultSetType, resultSetConcurrency);
    }

    @Override
    public Map<String, Class<?>> getTypeMap() throws SQLException {
        return realConn.getTypeMap();
    }

    @Override
    public void setTypeMap(Map<String, Class<?>> map) throws SQLException {
        realConn.setTypeMap(map);
    }

    @Override
    public void setHoldability(int holdability) throws SQLException {
        realConn.setHoldability(holdability);
    }

    @Override
    public int getHoldability() throws SQLException {
        return realConn.getHoldability();
    }

    @Override
    public Savepoint setSavepoint() throws SQLException {
        return realConn.setSavepoint();
    }

    @Override
    public Savepoint setSavepoint(String name) throws SQLException {
        return realConn.setSavepoint(name);
    }

    @Override
    public void rollback(Savepoint savepoint) throws SQLException {
        realConn.rollback(savepoint);
    }

    @Override
    public void releaseSavepoint(Savepoint savepoint) throws SQLException {
        realConn.releaseSavepoint(savepoint);
    }

    @Override
    public Statement createStatement(int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
        return realConn.createStatement(resultSetType, resultSetConcurrency, resultSetHoldability);
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
        PreparedStatement statement = realConn.prepareStatement(sql, resultSetType, resultSetConcurrency, resultSetHoldability);
        return getTxPreparedStatement(statement, sql);
    }

    @Override
    public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
        return realConn.prepareCall(sql, resultSetType, resultSetConcurrency, resultSetHoldability);
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys) throws SQLException {
        PreparedStatement statement = realConn.prepareStatement(sql, autoGeneratedKeys);
        return getTxPreparedStatement(statement, sql);
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int[] columnIndexes) throws SQLException {
        PreparedStatement statement = realConn.prepareStatement(sql, columnIndexes);
        return getTxPreparedStatement(statement, sql);
    }

    @Override
    public PreparedStatement prepareStatement(String sql, String[] columnNames) throws SQLException {
        PreparedStatement statement = realConn.prepareStatement(sql, columnNames);
        return getTxPreparedStatement(statement, sql);
    }

    @Override
    public Clob createClob() throws SQLException {
        return realConn.createClob();
    }

    @Override
    public Blob createBlob() throws SQLException {
        return realConn.createBlob();
    }

    @Override
    public NClob createNClob() throws SQLException {
        return realConn.createNClob();
    }

    @Override
    public SQLXML createSQLXML() throws SQLException {
        return realConn.createSQLXML();
    }

    @Override
    public boolean isValid(int timeout) throws SQLException {
        return realConn.isValid(timeout);
    }

    @Override
    public void setClientInfo(String name, String value) throws SQLClientInfoException {
        realConn.setClientInfo(name, value);
    }

    @Override
    public void setClientInfo(Properties properties) throws SQLClientInfoException {
        realConn.setClientInfo(properties);
    }

    @Override
    public String getClientInfo(String name) throws SQLException {
        return realConn.getClientInfo(name);
    }

    @Override
    public Properties getClientInfo() throws SQLException {
        return realConn.getClientInfo();
    }

    @Override
    public Array createArrayOf(String typeName, Object[] elements) throws SQLException {
        return realConn.createArrayOf(typeName, elements);
    }

    @Override
    public Struct createStruct(String typeName, Object[] attributes) throws SQLException {
        return realConn.createStruct(typeName, attributes);
    }

    @Override
    public void setSchema(String schema) throws SQLException {
        realConn.setSchema(schema);
    }

    @Override
    public String getSchema() throws SQLException {
        return realConn.getSchema();
    }

    @Override
    public void abort(Executor executor) throws SQLException {
        realConn.abort(executor);
    }

    @Override
    public void setNetworkTimeout(Executor executor, int milliseconds) throws SQLException {
        realConn.setNetworkTimeout(executor, milliseconds);
    }

    @Override
    public int getNetworkTimeout() throws SQLException {
        return realConn.getNetworkTimeout();
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        return realConn.unwrap(iface);
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return realConn.isWrapperFor(iface);
    }
}
