package rabbit.open.test;

import java.sql.Connection;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import rabbit.open.orm.pool.jpa.RabbitDataSource;

public class RabbitDataSourceTest {

	static Logger logger = Logger.getLogger(RabbitDataSourceTest.class);
	static boolean stop = false;
	public static void main(String[] args) throws Exception {
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
       
        for (int i = 0; i < 30; i++) {
        	new Thread(new Runnable() {
				
				@Override
				public void run() {
					while (!stop) {
						try {
							Connection connection = rds.getConnection();
							new Semaphore(0).tryAcquire(10, TimeUnit.MILLISECONDS);
							connection.close();
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
					logger.info("exit " + Thread.currentThread().getId());
				}
			}).start();
        }
        
        System.in.read();
        stop = true;
        logger.info("stopped");
        System.in.read();
        rds.shutdown();
	}
}
