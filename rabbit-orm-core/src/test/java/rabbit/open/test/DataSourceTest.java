package rabbit.open.test;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import junit.framework.TestCase;
import rabbit.open.orm.datasource.RabbitDataSource;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

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
    public void restartTest() {
        rds.restart();
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

}
