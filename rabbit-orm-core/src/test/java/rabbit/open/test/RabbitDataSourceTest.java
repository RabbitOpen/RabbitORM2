package rabbit.open.test;

import junit.framework.TestCase;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import rabbit.open.orm.datasource.RabbitDataSource;

import java.sql.Connection;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

@RunWith(JUnit4.class)
public class RabbitDataSourceTest {

	@Test
	public void getTest() throws Exception {
		RabbitDataSource rds = new RabbitDataSource();
        rds.setDriverClass("com.mysql.jdbc.Driver");
        rds.setMaxCachedStmt(1000);
		rds.setMaxIdle(30);
        rds.setMinSize(5);
        rds.setMaxSize(50);
        rds.setUrl("jdbc:mysql://localhost:3306/cas?useUnicode=true&characterEncoding=UTF-8&useServerPrepStmts=true");
        rds.setUsername("root");
        rds.setPassword("123");
        rds.init();
        int times = 10000;
        int threadCount = 30;
        AtomicInteger count = new AtomicInteger(0);
        Semaphore s = new Semaphore(0);
		for (int i = 0; i < threadCount; i++) {
			new Thread(new Runnable() {
				@Override
				public void run() {
					for (int k = 0; k < times; k++) {
						try {
							Connection connection = rds.getConnection();
							connection.close();
							count.addAndGet(1);
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
					s.release();
				}
			}).start();
		}
		s.acquire(threadCount);
        rds.shutdown();
        TestCase.assertEquals(count.get(), times * threadCount);
	}
}
