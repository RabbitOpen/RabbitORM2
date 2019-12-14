package rabbit.open.test;

import java.sql.Connection;
import java.sql.SQLException;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import junit.framework.TestCase;
import rabbit.open.orm.datasource.RabbitDataSource;
import rabbit.open.orm.datasource.Session;

/**
 * <b>Description: Session测试</b><br>
 * <b>@author</b> 肖乾斌
 * 
 */
@RunWith(JUnit4.class)
public class SessionTest {

    RabbitDataSource rds;

    @Test
    public void sessionTest() throws SQLException {
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
        Session session = (Session) rds.getConnection();
        Connection conn = session.getConnector();
        
        TestCase.assertEquals(conn.getClientInfo(), session.getClientInfo());
        TestCase.assertEquals(conn.getClientInfo("xx"), session.getClientInfo("xx"));
        TestCase.assertEquals(conn.getCatalog(), session.getCatalog());
        TestCase.assertEquals(conn.getHoldability(), session.getHoldability());
        session.setNetworkTimeout(command -> {

        }, 1000);
        TestCase.assertEquals(conn.getNetworkTimeout(), session.getNetworkTimeout());
        TestCase.assertEquals(conn.getTypeMap(), session.getTypeMap());
        TestCase.assertEquals(conn.getSchema(), session.getSchema());
        TestCase.assertEquals(conn.getWarnings(), session.getWarnings());
        
        session.destroy();
        rds.shutdown();
    }

   
}
