package rabbit.open.dtx.client.datasource.proxy;

import rabbit.open.dtx.client.context.DistributedTransactionContext;
import rabbit.open.dtx.client.datasource.parser.SQLStructure;
import rabbit.open.dtx.client.datasource.parser.SimpleSQLParser;

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.*;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * prepared statement proxy
 * @author xiaoqianbin
 * @date 2019/12/2
 **/
public class TxPreparedStatement implements PreparedStatement {

    private PreparedStatement realStmt;

    private TxConnection txConn;

    private List<Object> values = new ArrayList<>();

    private String preparedSql;

    public TxPreparedStatement(PreparedStatement realStmt, TxConnection txConn, String preparedSql) {
        this.realStmt = realStmt;
        this.txConn = txConn;
        this.preparedSql = preparedSql;
    }

    public TxConnection getTxConn() {
        return txConn;
    }

    @Override
    public ResultSet executeQuery() throws SQLException {
        return realStmt.executeQuery();
    }

    @Override
    public int executeUpdate() throws SQLException {
        int i = realStmt.executeUpdate();
        if (null != DistributedTransactionContext.getTransactionContext()) {
            SQLStructure parse = SimpleSQLParser.parse(preparedSql);

        }
        return i;
    }

    @Override
    public void setNull(int parameterIndex, int sqlType) throws SQLException {
        realStmt.setNull(parameterIndex, sqlType);
    }

    @Override
    public void setBoolean(int parameterIndex, boolean x) throws SQLException {
        realStmt.setBoolean(parameterIndex, x);
        values.add(x);
    }

    @Override
    public void setByte(int parameterIndex, byte x) throws SQLException {
        realStmt.setByte(parameterIndex, x);
        values.add(x);
    }

    @Override
    public void setShort(int parameterIndex, short x) throws SQLException {
        realStmt.setShort(parameterIndex, x);
        values.add(x);
    }

    @Override
    public void setInt(int parameterIndex, int x) throws SQLException {
        realStmt.setInt(parameterIndex, x);
        values.add(x);
    }

    @Override
    public void setLong(int parameterIndex, long x) throws SQLException {
        realStmt.setLong(parameterIndex, x);
        values.add(x);
    }

    @Override
    public void setFloat(int parameterIndex, float x) throws SQLException {
        realStmt.setFloat(parameterIndex, x);
        values.add(x);
    }

    @Override
    public void setDouble(int parameterIndex, double x) throws SQLException {
        realStmt.setDouble(parameterIndex, x);
        values.add(x);
    }

    @Override
    public void setBigDecimal(int parameterIndex, BigDecimal x) throws SQLException {
        realStmt.setBigDecimal(parameterIndex, x);
        values.add(x);
    }

    @Override
    public void setString(int parameterIndex, String x) throws SQLException {
        realStmt.setString(parameterIndex, x);
        values.add(x);
    }

    @Override
    public void setBytes(int parameterIndex, byte[] x) throws SQLException {
        realStmt.setBytes(parameterIndex, x);
        values.add(x);
    }

    @Override
    public void setDate(int parameterIndex, Date x) throws SQLException {
        realStmt.setDate(parameterIndex, x);
        values.add(x);
    }

    @Override
    public void setTime(int parameterIndex, Time x) throws SQLException {
        realStmt.setTime(parameterIndex, x);
        values.add(x);
    }

    @Override
    public void setTimestamp(int parameterIndex, Timestamp x) throws SQLException {
        realStmt.setTimestamp(parameterIndex, x);
        values.add(x);
    }

    @Override
    public void setAsciiStream(int parameterIndex, InputStream x, int length) throws SQLException {
        realStmt.setAsciiStream(parameterIndex, x, length);
    }

    /**
     * @deprecated (may be someday)
     */
    @Override
    @Deprecated
    public void setUnicodeStream(int parameterIndex, InputStream x, int length) throws SQLException {
        realStmt.setUnicodeStream(parameterIndex, x, length);
    }

    @Override
    public void setBinaryStream(int parameterIndex, InputStream x, int length) throws SQLException {
        realStmt.setBinaryStream(parameterIndex, x, length);
    }

    @Override
    public void clearParameters() throws SQLException {
        realStmt.clearParameters();
    }

    @Override
    public void setObject(int parameterIndex, Object x, int targetSqlType) throws SQLException {
        realStmt.setObject(parameterIndex, x, targetSqlType);
        values.add(x);
    }

    @Override
    public void setObject(int parameterIndex, Object x) throws SQLException {
        realStmt.setObject(parameterIndex, x);
        values.add(x);
    }

    @Override
    public boolean execute() throws SQLException {
        return realStmt.execute();
    }

    @Override
    public void addBatch() throws SQLException {
        realStmt.addBatch();
    }

    @Override
    public void setCharacterStream(int parameterIndex, Reader reader, int length) throws SQLException {
        realStmt.setCharacterStream(parameterIndex, reader, length);
    }

    @Override
    public void setRef(int parameterIndex, Ref x) throws SQLException {
        realStmt.setRef(parameterIndex, x);
        values.add(x);
    }

    @Override
    public void setBlob(int parameterIndex, Blob x) throws SQLException {
        realStmt.setBlob(parameterIndex, x);
        values.add(x);
    }

    @Override
    public void setClob(int parameterIndex, Clob x) throws SQLException {
        realStmt.setClob(parameterIndex, x);
        values.add(x);
    }

    @Override
    public void setArray(int parameterIndex, Array x) throws SQLException {
        realStmt.setArray(parameterIndex, x);
        values.add(x);
    }

    @Override
    public ResultSetMetaData getMetaData() throws SQLException {
        return realStmt.getMetaData();
    }

    @Override
    public void setDate(int parameterIndex, Date x, Calendar cal) throws SQLException {
        realStmt.setDate(parameterIndex, x);
        values.add(x);
    }

    @Override
    public void setTime(int parameterIndex, Time x, Calendar cal) throws SQLException {
        realStmt.setTime(parameterIndex, x);
        values.add(x);
    }

    @Override
    public void setTimestamp(int parameterIndex, Timestamp x, Calendar cal) throws SQLException {
        realStmt.setTimestamp(parameterIndex, x);
        values.add(x);
    }

    @Override
    public void setNull(int parameterIndex, int sqlType, String typeName) throws SQLException {
        realStmt.setNull(parameterIndex, sqlType, typeName);
    }

    @Override
    public void setURL(int parameterIndex, URL x) throws SQLException {
        realStmt.setURL(parameterIndex, x);
        values.add(x);
    }

    @Override
    public ParameterMetaData getParameterMetaData() throws SQLException {
        return realStmt.getParameterMetaData();
    }

    @Override
    public void setRowId(int parameterIndex, RowId x) throws SQLException {
        realStmt.setRowId(parameterIndex, x);
        values.add(x);
    }

    @Override
    public void setNString(int parameterIndex, String x) throws SQLException {
        realStmt.setNString(parameterIndex, x);
        values.add(x);
    }

    @Override
    public void setNCharacterStream(int parameterIndex, Reader value, long length) throws SQLException {
        realStmt.setNCharacterStream(parameterIndex, value, length);
    }

    @Override
    public void setNClob(int parameterIndex, NClob value) throws SQLException {
        realStmt.setNClob(parameterIndex, value);
        values.add(value);
    }

    @Override
    public void setClob(int parameterIndex, Reader reader, long length) throws SQLException {
        realStmt.setClob(parameterIndex, reader, length);
    }

    @Override
    public void setBlob(int parameterIndex, InputStream inputStream, long length) throws SQLException {
        realStmt.setBlob(parameterIndex, inputStream, length);
    }

    @Override
    public void setNClob(int parameterIndex, Reader reader, long length) throws SQLException {
        realStmt.setNClob(parameterIndex, reader, length);
    }

    @Override
    public void setSQLXML(int parameterIndex, SQLXML xmlObject) throws SQLException {
        realStmt.setSQLXML(parameterIndex, xmlObject);
        values.add(xmlObject);
    }

    @Override
    public void setObject(int parameterIndex, Object x, int targetSqlType, int scaleOrLength) throws SQLException {
        realStmt.setObject(parameterIndex, x, targetSqlType, scaleOrLength);
    }

    @Override
    public void setAsciiStream(int parameterIndex, InputStream x, long length) throws SQLException {
        realStmt.setAsciiStream(parameterIndex, x, length);
    }

    @Override
    public void setBinaryStream(int parameterIndex, InputStream x, long length) throws SQLException {
        realStmt.setBinaryStream(parameterIndex, x, length);
    }

    @Override
    public void setCharacterStream(int parameterIndex, Reader reader, long length) throws SQLException {
        realStmt.setCharacterStream(parameterIndex, reader, length);
    }

    @Override
    public void setAsciiStream(int parameterIndex, InputStream x) throws SQLException {
        realStmt.setAsciiStream(parameterIndex, x);
    }

    @Override
    public void setBinaryStream(int parameterIndex, InputStream x) throws SQLException {
        realStmt.setBinaryStream(parameterIndex, x);
    }

    @Override
    public void setCharacterStream(int parameterIndex, Reader reader) throws SQLException {
        realStmt.setCharacterStream(parameterIndex, reader);
    }

    @Override
    public void setNCharacterStream(int parameterIndex, Reader value) throws SQLException {
        realStmt.setNCharacterStream(parameterIndex, value);
    }

    @Override
    public void setClob(int parameterIndex, Reader reader) throws SQLException {
        realStmt.setClob(parameterIndex, reader);
    }

    @Override
    public void setBlob(int parameterIndex, InputStream inputStream) throws SQLException {
        realStmt.setBlob(parameterIndex, inputStream);
    }

    @Override
    public void setNClob(int parameterIndex, Reader reader) throws SQLException {
        realStmt.setNClob(parameterIndex, reader);
    }

    @Override
    public ResultSet executeQuery(String sql) throws SQLException {
        return realStmt.executeQuery(sql);
    }

    @Override
    public int executeUpdate(String sql) throws SQLException {
        return realStmt.executeUpdate(sql);
    }

    @Override
    public void close() throws SQLException {
        realStmt.close();
    }

    @Override
    public int getMaxFieldSize() throws SQLException {
        return realStmt.getMaxFieldSize();
    }

    @Override
    public void setMaxFieldSize(int max) throws SQLException {
        realStmt.setMaxFieldSize(max);
    }

    @Override
    public int getMaxRows() throws SQLException {
        return realStmt.getMaxRows();
    }

    @Override
    public void setMaxRows(int max) throws SQLException {
        realStmt.setMaxRows(max);
    }

    @Override
    public void setEscapeProcessing(boolean enable) throws SQLException {
        realStmt.setEscapeProcessing(enable);
    }

    @Override
    public int getQueryTimeout() throws SQLException {
        return realStmt.getQueryTimeout();
    }

    @Override
    public void setQueryTimeout(int seconds) throws SQLException {
        realStmt.setQueryTimeout(seconds);
    }

    @Override
    public void cancel() throws SQLException {
        realStmt.cancel();
    }

    @Override
    public SQLWarning getWarnings() throws SQLException {
        return realStmt.getWarnings();
    }

    @Override
    public void clearWarnings() throws SQLException {
        realStmt.clearWarnings();
    }

    @Override
    public void setCursorName(String name) throws SQLException {
        realStmt.setCursorName(name);
    }

    @Override
    public boolean execute(String sql) throws SQLException {
        return realStmt.execute(sql);
    }

    @Override
    public ResultSet getResultSet() throws SQLException {
        return realStmt.getResultSet();
    }

    @Override
    public int getUpdateCount() throws SQLException {
        return realStmt.getUpdateCount();
    }

    @Override
    public boolean getMoreResults() throws SQLException {
        return realStmt.getMoreResults();
    }

    @Override
    public void setFetchDirection(int direction) throws SQLException {
        realStmt.setFetchDirection(direction);
    }

    @Override
    public int getFetchDirection() throws SQLException {
        return realStmt.getFetchDirection();
    }

    @Override
    public void setFetchSize(int rows) throws SQLException {
        realStmt.setFetchSize(rows);
    }

    @Override
    public int getFetchSize() throws SQLException {
        return realStmt.getFetchSize();
    }

    @Override
    public int getResultSetConcurrency() throws SQLException {
        return realStmt.getResultSetConcurrency();
    }

    @Override
    public int getResultSetType() throws SQLException {
        return realStmt.getResultSetType();
    }

    @Override
    public void addBatch(String sql) throws SQLException {
        realStmt.addBatch(sql);
    }

    @Override
    public void clearBatch() throws SQLException {
        realStmt.clearBatch();
    }

    @Override
    public int[] executeBatch() throws SQLException {
        return realStmt.executeBatch();
    }

    @Override
    public Connection getConnection() throws SQLException {
        return realStmt.getConnection();
    }

    @Override
    public boolean getMoreResults(int current) throws SQLException {
        return realStmt.getMoreResults(current);
    }

    @Override
    public ResultSet getGeneratedKeys() throws SQLException {
        return realStmt.getGeneratedKeys();
    }

    @Override
    public int executeUpdate(String sql, int autoGeneratedKeys) throws SQLException {
        return realStmt.executeUpdate(sql, autoGeneratedKeys);
    }

    @Override
    public int executeUpdate(String sql, int[] columnIndexes) throws SQLException {
        return realStmt.executeUpdate(sql, columnIndexes);
    }

    @Override
    public int executeUpdate(String sql, String[] columnNames) throws SQLException {
        return realStmt.executeUpdate(sql, columnNames);
    }

    @Override
    public boolean execute(String sql, int autoGeneratedKeys) throws SQLException {
        return realStmt.execute(sql, autoGeneratedKeys);
    }

    @Override
    public boolean execute(String sql, int[] columnIndexes) throws SQLException {
        return realStmt.execute(sql, columnIndexes);
    }

    @Override
    public boolean execute(String sql, String[] columnNames) throws SQLException {
        return realStmt.execute(sql, columnNames);
    }

    @Override
    public int getResultSetHoldability() throws SQLException {
        return realStmt.getResultSetHoldability();
    }

    @Override
    public boolean isClosed() throws SQLException {
        return realStmt.isClosed();
    }

    @Override
    public void setPoolable(boolean poolable) throws SQLException {
        realStmt.setPoolable(poolable);
    }

    @Override
    public boolean isPoolable() throws SQLException {
        return realStmt.isPoolable();
    }

    @Override
    public void closeOnCompletion() throws SQLException {
        realStmt.closeOnCompletion();
    }

    @Override
    public boolean isCloseOnCompletion() throws SQLException {
        return realStmt.isCloseOnCompletion();
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        return realStmt.unwrap(iface);
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return realStmt.isWrapperFor(iface);
    }
}
