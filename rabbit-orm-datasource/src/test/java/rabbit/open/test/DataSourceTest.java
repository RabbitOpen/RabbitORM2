package rabbit.open.test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import com.mysql.jdbc.Statement;

import junit.framework.TestCase;
import rabbit.open.orm.datasource.RabbitDataSource;

/**
 * <b>Description: 数据源测试</b><br>
 * <b>@author</b> 肖乾斌
 * 
 */
@RunWith(JUnit4.class)
public class DataSourceTest {

    RabbitDataSource rds;

    @Before
    public void setUp() {
        rds = new RabbitDataSource();
        rds.setDriverClass("com.mysql.jdbc.Driver");
        rds.setMaxCachedStmt(1000);
        rds.setMaxIdle(3);
        rds.setMinSize(3);
        rds.setMaxSize(10);
        rds.setUrl("jdbc:mysql://localhost:3306/cas?useUnicode=true&characterEncoding=UTF-8&useServerPrepStmts=true");
        rds.setUsername("root");
        rds.setPassword("123");
        rds.setShowSlowSql(true);
        rds.setThreshold(1);
        rds.setFetchTimeOut(100);
        rds.init();
    }

    /**
     * 
     * <b>Description: 获取300万次连接测试 </b><br>
     * @throws InterruptedException
     * 
     */
    @Test
    public void getConnectionTest() throws InterruptedException {
        Semaphore s = new Semaphore(0);
        int counter = 30;
        for (int i = 0; i < counter; i++) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    for (int j = 0; j < 100000; j++) {
                        try {
                            Connection connection = rds.getConnection();
                            connection.close();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    s.release();
                }
            }).start();
        }
        try {
            s.acquire(counter);
        } catch (Exception e) {
            e.printStackTrace();
        }
        rds.shutdown();
        TestCase.assertEquals(0, rds.getConnectors().size());
    }

    @Test
    public void restartTest() throws SQLException {
        rds.restart();
        Connection connection = rds.getConnection();
        connection.getMetaData();
        connection.setReadOnly(true);
        TestCase.assertEquals(true, connection.isReadOnly());
        connection.setTransactionIsolation(connection.getTransactionIsolation());
        TestCase.assertEquals(false, connection.isClosed());
        connection.close();
        rds.shutdown();
        TestCase.assertEquals(0, rds.getConnectors().size());
    }

    @Test
    public void sessionHoldTimeoutTest() throws InterruptedException, SQLException {
    	rds.setMaxSessionHoldingSeconds(1);
    	rds.setDumpSuspectedFetch(true);
    	Connection conn = rds.getConnection();
    	new Semaphore(0).tryAcquire(7, TimeUnit.SECONDS);
    	conn.close();
    	rds.shutdown();
    	TestCase.assertEquals(0, rds.getConnectors().size());
    }
    
    @Test
    public void testPreparedStatement() throws SQLException {
    	Connection conn = rds.getConnection();
    	conn.setAutoCommit(true);
    	PreparedStatement stmt = conn.prepareStatement("insert into morg(NAME) values (?)", Statement.RETURN_GENERATED_KEYS);
    	stmt.setString(1, "abc");
    	stmt.executeUpdate();
    	//无效的rollback
    	conn.rollback();
        ResultSet rs = stmt.getGeneratedKeys();
        int id = 0;
        if (rs.next()) {
        	id = rs.getBigDecimal(1).intValue();
        }
        TestCase.assertEquals(true, 0 != id);
    	conn.close();
    	rds.shutdown();
    	
    }

    @Test
    public void testTransaction() throws SQLException {
    	Connection conn = rds.getConnection();
    	int count = getcount(conn);
    	conn.setAutoCommit(false);
    	conn.setAutoCommit(false);
    	TestCase.assertEquals(false, conn.getAutoCommit());
    	java.sql.Statement stmt = conn.createStatement();
    	stmt.execute("insert into morg(NAME) values ('abcd')");
    	conn.commit();
    	stmt.close();
    	TestCase.assertEquals(count + 1, getcount(conn));
    	
    	stmt = conn.createStatement();
    	stmt.execute("insert into morg(NAME) values ('abcd')");
    	conn.rollback();
    	
    	TestCase.assertEquals(count + 1, getcount(conn));
    	conn.close();
    	rds.shutdown();
    	
    }

	protected int getcount(Connection conn) throws SQLException {
		java.sql.Statement stmt = conn.createStatement();
    	ResultSet rs = stmt.executeQuery("select count(1) from morg");
    	rs.next();
    	int count = rs.getInt(1);
    	rs.close();
    	stmt.close();
    	return count;
	}

}
