package rabbit.open.test;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import rabbit.open.orm.pool.jpa.DataSourceMonitor;
import rabbit.open.orm.pool.jpa.RabbitDataSource;

@RunWith(JUnit4.class)
public class MonitorTest {

    RabbitDataSource rds;

    @Before
    public void setUp() {
        rds = new RabbitDataSource() {
            @Override
            public void init() {
                loadDriverClass();
                initSessions();
                monitor = new DataSourceMonitor(rds) {
                    @Override
                    protected long getMaxIdle() {
                        // 调整空闲间隔时间，方便测试
                        return 1L;
                    }

                    @Override
                    protected void sleep5s() {
                        try {
                            Thread.sleep(1000L);
                        } catch (InterruptedException e) {
                            logger.info("database monitor is interrupted");
                        }
                    }

                    @Override
                    protected boolean tooManyIdleSessions() {
                        return dataSource.getCounter() > 2;
                    }
                };
                monitor.start();
            }
        };
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

    @Test
    public void monitorTest() throws InterruptedException {
        synchronized (this) {
            wait(5000);
        }
        rds.shutdown();
    }
}
