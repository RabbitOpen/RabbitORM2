package rabbit.open.dtx.client.datasource.proxy;

import org.springframework.beans.factory.BeanCreationException;
import rabbit.open.dtx.common.exception.DistributedTransactionException;
import rabbit.open.dtx.common.nio.client.DistributedTransactionManager;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * datasource proxy
 * @author xiaoqianbin
 * @date 2019/12/2
 **/
public class TxDataSource implements DataSource {

    private static Map<String, TxDataSource> dataSourceMap = new ConcurrentHashMap<>();

    // 缓存表的自增状态
    private Map<String, Boolean> keyGenCache = new ConcurrentHashMap<>();

    // 自增字段名
    private Map<String, String> autoIncrementColumnNames = new ConcurrentHashMap<>();

    // 主键字段名
    private Map<String, String> primaryKeyNames = new ConcurrentHashMap<>();

    // 真实数据源
    private DataSource dataSource;

    // 数据源名
    private String dataSourceName;

    // 事务管理器
    private DistributedTransactionManager transactionManger;

    public TxDataSource(DataSource dataSource, String dataSourceName, DistributedTransactionManager transactionManger) {
        this.dataSource = dataSource;
        this.dataSourceName = dataSourceName;
        this.transactionManger = transactionManger;
        if (dataSourceMap.containsKey(dataSourceName)) {
            throw new BeanCreationException(String.format("repeated datasource name '%s'", dataSourceName));
        }
        loadKeyGenInfo(dataSource);
        dataSourceMap.put(dataSourceName, this);
    }

    /**
     * 加载自增字段信息
     * @param    dataSource
     * @author xiaoqianbin
     * @date 2019/12/23
     **/
    private void loadKeyGenInfo(DataSource dataSource) {
        Connection connection = null;
        ResultSet tables = null;
        try {
            connection = dataSource.getConnection();
            tables = connection.getMetaData().getTables(null, null, null, null);
            while (tables.next()) {
                if (!"TABLE".equalsIgnoreCase(tables.getString("TABLE_TYPE"))) {
                    continue;
                }
                String tableName = tables.getString("TABLE_NAME");
                loadKeyGenInfoByTableName(connection, tableName);
                loadPrimaryKeys(connection, tableName);
            }
        } catch (Exception e) {
            throw new DistributedTransactionException(e);
        } finally {
            closeQuietly(tables);
            closeQuietly(connection);
        }
    }

    private void loadPrimaryKeys(Connection connection, String tableName) {
        ResultSet rs = null;
        try {
            rs = connection.getMetaData().getPrimaryKeys(null, null, tableName);
            while (rs.next()) {
                primaryKeyNames.put(tableName.toUpperCase(), rs.getString("COLUMN_NAME"));
            }
        } catch (Exception e) {
            throw new DistributedTransactionException(e);
        } finally {
            closeQuietly(rs);
        }
    }

    public String getPrimaryKey(String tableName, Connection conn) {
        if (!primaryKeyNames.containsKey(tableName.toUpperCase())) {
            loadPrimaryKeys(conn, tableName);
        }
        return primaryKeyNames.get(tableName.toUpperCase());
    }

    public boolean isAutoIncrement(String table, Connection conn) {
        if (!keyGenCache.containsKey(table.toUpperCase())) {
            loadKeyGenInfoByTableName(conn, table);
        }
        return keyGenCache.get(table.toUpperCase());
    }

    public String getAutoIncrementColumn(String table) {
        return autoIncrementColumnNames.get(table.toUpperCase());
    }

    private void loadKeyGenInfoByTableName(Connection connection, String tableName) {
        ResultSet columns = null;
        try {
            columns = connection.getMetaData().getColumns(null, null, tableName, null);
            while (columns.next()) {
                keyGenCache.put(tableName.toUpperCase(), false);
                if ("YES".equalsIgnoreCase(columns.getString("IS_AUTOINCREMENT"))) {
                    keyGenCache.put(tableName.toUpperCase(), true);
                    autoIncrementColumnNames.put(tableName.toUpperCase(), columns.getString("COLUMN_NAME"));
                    break;
                }
            }
        } catch (Exception e) {
            // TO DO: ignore ORACLE 没这个字段, 会抛异常
        } finally {
            closeQuietly(columns);
        }
    }

    public void closeQuietly(AutoCloseable c) {
        try {
            if (null != c) {
                c.close();
            }
        } catch (Exception e) {
            // TO DO :ignore
        }
    }

    public DataSource getRealDataSource() {
        return dataSource;
    }

    /**
     * 根据应用中的数据源
     * @author xiaoqianbin
     * @date 2019/12/5
     **/
    public static Collection<TxDataSource> getDataSources() {
        return dataSourceMap.values();
    }

    public DistributedTransactionManager getTransactionManger() {
        return transactionManger;
    }

    @Override
    public Connection getConnection() throws SQLException {
        return new TxConnection(dataSource.getConnection(), this);
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        return new TxConnection(dataSource.getConnection(username, password), this);
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        return dataSource.unwrap(iface);
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return dataSource.isWrapperFor(iface);
    }

    @Override
    public PrintWriter getLogWriter() throws SQLException {
        return dataSource.getLogWriter();
    }

    @Override
    public void setLogWriter(PrintWriter out) throws SQLException {
        dataSource.setLogWriter(out);
    }

    @Override
    public void setLoginTimeout(int seconds) throws SQLException {
        dataSource.setLoginTimeout(seconds);
    }

    @Override
    public int getLoginTimeout() throws SQLException {
        return dataSource.getLoginTimeout();
    }

    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        return dataSource.getParentLogger();
    }

    public String getDataSourceName() {
        return dataSourceName;
    }
}
