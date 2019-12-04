package rabbit.open.dtx.client.datasource.proxy;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.logging.Logger;

/**
 * datasource proxy
 * @author xiaoqianbin
 * @date 2019/12/2
 **/
public class TxDataSource implements DataSource {

    // 真实数据源
    private DataSource dataSource;

    // 数据源名
    private String dataSourceName;

    public TxDataSource(DataSource dataSource, String dataSourceName) {
        this.dataSource = dataSource;
        this.dataSourceName = dataSourceName;
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
