package rabbit.open.test.datasource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.concurrent.Semaphore;

import javax.sql.DataSource;

import org.apache.commons.dbcp2.BasicDataSource;

import rabbit.open.orm.pool.jpa.RabbitDataSource;

import com.alibaba.druid.pool.DruidDataSource;
import com.mchange.v2.c3p0.ComboPooledDataSource;
import com.zaxxer.hikari.HikariDataSource;

/**
 * <b>Description:   单独测试各种数据源的性能</b>.
 *                   
 *             测试方法：      
 *                   模拟30个线程，每个线程调用1000次
 *                   查看耗时
 *             
 *             分两种模式测试      
 *             PreparedStatement
 *             Statement        
 *                   
 * <b>@author</b>    肖乾斌
 * 
 */
public class TestSingleDB {

    private static final int MAX = 10;
    static String url = "jdbc:mysql://localhost:3306/cas?useUnicode=true&characterEncoding=UTF-8&useServerPrepStmts=true";
    static String username = "root";
    static String password = "123";
    static String sql = "SELECT A.*, B.* FROM T_USER A LEFT JOIN T_ORG B ON A.ORG_ID = B.ID WHERE A.ID = 1";

    public static void main(String[] args) {
        DataSource ds = getHikariDatasource();
        Semaphore s = new Semaphore(0);
        int threadCount = 100;
        long start = System.currentTimeMillis();
        for (int i = 0; i < threadCount; i++) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    for (int i = 0; i < 500; i++) {
                        Connection connection = null;
                        PreparedStatement stmt = null;
                        try {
                            connection = ds.getConnection();
                            stmt = connection.prepareStatement(sql);
                            stmt.execute();
                        } catch (Exception e) {
                            e.printStackTrace();
                        } finally {
                            try {
                                stmt.close();
                                connection.close();
                            } catch (SQLException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    s.release();
                }
            }).start();
        }
        try {
            s.acquire(threadCount);
            System.out.println("cost:\t "
                    + (System.currentTimeMillis() - start));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * 
     * <b>Description: dbcp数据源</b><br>
     * .
     * 
     * @return
     * 
     */
    public static DataSource getDbcpDatasource() {
        BasicDataSource ds = new BasicDataSource();
        ds.setUrl(url);
        ds.setUsername(username);
        ds.setPassword(password);
        ds.setMaxIdle(3);
        ds.setMaxConnLifetimeMillis(60 * 1000);
        ds.setCacheState(true);
        ds.setPoolPreparedStatements(true);
        ds.setMaxOpenPreparedStatements(1000);
        ds.setMaxWaitMillis(30 * 1000);
        ds.setMinIdle(3);
        ds.setMaxTotal(MAX);
        return ds;
    }

    /**
     * 
     * <b>Description: 获取c3p0数据源</b><br>
     * .
     * 
     * @return
     * 
     */
    public static DataSource getC3p0Datasource() {
        ComboPooledDataSource ds = new ComboPooledDataSource();
        ds.setJdbcUrl(url);
        ds.setUser(username);
        ds.setPassword(password);
        ds.setMaxStatements(1000);
        ds.setMaxStatementsPerConnection(1000);
        ds.setMinPoolSize(3);
        ds.setMaxIdleTime(60 * 1000);
        ds.setInitialPoolSize(3);
        ds.setMaxPoolSize(MAX);
        return ds;
    }

    /**
     * 
     * <b>Description: 获取hikari数据源</b><br>
     * .
     * 
     * @return
     * 
     */
    public static DataSource getHikariDatasource() {
        HikariDataSource ds = new HikariDataSource();
        ds.setJdbcUrl(url);
        ds.setUsername(username);
        ds.setPassword(password);
        ds.setMinimumIdle(3);
        ds.setIdleTimeout(60 * 1000);
        ds.setMaxLifetime(5 * 60 * 1000);
        ds.setMaximumPoolSize(MAX);
        return ds;
    }

    /**
     * 
     * <b>Description: 获取rabbit数据源</b><br>
     * .
     * 
     * @return
     * 
     */
    public static DataSource getRabbitDatasource() {
        RabbitDataSource ds = new RabbitDataSource();
        ds.setUrl(url);
        ds.setUsername(username);
        ds.setPassword(password);
        ds.setMaxIdle(3);
        ds.setMaxSize(MAX);
        ds.setMinSize(3);
        return ds;
    }

    /**
     * 
     * <b>Description: 获取rabbit数据源</b><br>
     * .
     * 
     * @return
     * 
     */
    public static DataSource getDruidDataSource() {
        DruidDataSource ds = new DruidDataSource();
        ds.setUrl(url);
        ds.setUsername(username);
        ds.setPassword(password);
        ds.setMaxActive(MAX);
        ds.setInitialSize(3);
        ds.setMinIdle(3);
        ds.setPoolPreparedStatements(true);
        ds.setMaxOpenPreparedStatements(1000);
        return ds;
    }
}
