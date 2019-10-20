package rabbit.open.test;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import junit.framework.TestCase;
import rabbit.open.orm.common.exception.DataSourceClosedException;
import rabbit.open.orm.common.exception.RabbitDMLException;
import rabbit.open.orm.datasource.RabbitDataSource;

/**
 * <b>Description: 数据源测试</b><br>
 * <b>@author</b> 肖乾斌
 * 
 */
@RunWith(JUnit4.class)
public class DataSourceWithDBTest {

    @Test
    public void dataSourceClosedExceptionTest() {
    	// mysql 
    	RabbitDataSource rds = getDataSource("jdbc:mysql://localhost:3306/cas?useUnicode=true&characterEncoding=UTF-8&useServerPrepStmts=true", 
    			"com.mysql.jdbc.Driver", "root", "123");
        rds.shutdown();
        try {
			rds.getConnection();
		} catch (Exception e) {
			TestCase.assertEquals(e.getClass(), DataSourceClosedException.class);
		}
        
        // oracle
        rds = getDataSource("jdbc:oracle:thin:@localhost:1521:ORCL", "oracle.jdbc.driver.OracleDriver", "root", "1234");
        rds.shutdown();
        try {
			rds.getConnection();
		} catch (Exception e) {
			TestCase.assertEquals(e.getClass(), DataSourceClosedException.class);
		}
        
        // sqlserver
        rds = getDataSource("jdbc:sqlserver://localhost:1433;DatabaseName=cas", 
        		"com.microsoft.sqlserver.jdbc.SQLServerDriver", "sa", "123");
        rds.shutdown();
        try {
        	rds.getConnection();
        } catch (Exception e) {
        	TestCase.assertEquals(e.getClass(), DataSourceClosedException.class);
        }
        
        // db2
        rds = getDataSource("jdbc:db2://localhost:50000/db2", "com.ibm.db2.jcc.DB2Driver", "admin", "wsmn9528");
        rds.shutdown();
        try {
        	rds.getConnection();
        } catch (Exception e) {
        	TestCase.assertEquals(e.getClass(), DataSourceClosedException.class);
        }
        
        // sqlite
        rds = getDataSource("jdbc:sqlite::resource:db/app.s3db", "org.sqlite.JDBC", null, null);
        rds.shutdown();
        try {
        	rds.getConnection();
        } catch (Exception e) {
        	TestCase.assertEquals(e.getClass(), DataSourceClosedException.class);
        }
        
        try {
        	rds = getDataSource("jdbc:sqlite::resource:db/app.s3db", "org.sqlite.JDBCx", null, null);
        } catch (Exception e) {
        	TestCase.assertEquals(e.getClass(), RabbitDMLException.class);
        }
    }

	private RabbitDataSource getDataSource(String url, String driverClass, String username, String password) {
		RabbitDataSource rds = new RabbitDataSource();
        rds.setDriverClass(driverClass);
        rds.setMaxCachedStmt(1000);
        rds.setMaxIdle(3);
        rds.setMinSize(3);
        rds.setMaxSize(10);
        rds.setUrl(url);
        rds.setUsername(username);
        rds.setPassword(password);
        rds.setShowSlowSql(true);
        rds.setThreshold(1);
        rds.setFetchTimeOut(100);
        rds.init();
		return rds;
	}


}
