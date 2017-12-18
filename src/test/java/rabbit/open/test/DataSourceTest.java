package rabbit.open.test;

import java.sql.Connection;
import java.util.concurrent.Semaphore;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import rabbit.open.orm.pool.jpa.RabbitDataSource;

/**
 * <b>Description: 	数据源测试</b><br>
 * <b>@author</b>	肖乾斌
 * 
 */
@RunWith(JUnit4.class)
public class DataSourceTest {

	RabbitDataSource rds;
	
	@Before
	public void setUp(){
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
	public void getConnectionTest() throws InterruptedException{
		Semaphore s = new Semaphore(0);
		int counter = 30;
		for(int i = 0; i < counter; i++){
			new Thread(new Runnable() {
				@Override
				public void run() {
					for(int j = 0; j < 100000; j++){
						try{
							Connection connection = rds.getConnection();
							connection.close();
						}catch(Exception e){
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
		//等6s，让monitor检测连接池，尝试释放空闲连接
		synchronized (this) {
            wait(6000);
        }
		rds.shutdown();
	}
	
	@Test
	public void restartTest(){
	    rds.restart();
	    rds.shutdown();
	}
	
}
