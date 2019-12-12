package rabbit.open.dtx.client.datasource.proxy;

import org.springframework.beans.factory.BeanCreationException;
import rabbit.open.dtx.common.nio.client.DistributedTransactionManager;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.sql.Connection;
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
        dataSourceMap.put(dataSourceName, this);
    }

    public DataSource getRealDataSource() {
        return dataSource;
    }

    /**
     * 根据应用中的数据源
     * @author  xiaoqianbin
     * @date    2019/12/5
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
