package rabbit.open.dtx.client.test;

import junit.framework.TestCase;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import rabbit.open.dtx.client.datasource.proxy.TxConnection;
import rabbit.open.dtx.client.datasource.proxy.TxDataSource;
import rabbit.open.dtx.client.datasource.proxy.TxPreparedStatement;
import rabbit.open.orm.common.exception.RabbitDMLException;
import rabbit.open.orm.datasource.RabbitDataSource;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.net.URISyntaxException;
import java.net.URL;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * 代理数据源、代理连接、代理PreparedStatement 常用方法测试，验证静态代理的正确性
 * @author xiaoqianbin
 * @date 2019/12/31
 **/
@RunWith(JUnit4.class)
public class TxDataSourceTest {

    @SuppressWarnings("unchecked")
	@Test
    public void datasourceTest() throws SQLException, FileNotFoundException, NoSuchFieldException, IllegalAccessException, URISyntaxException {
        RabbitDataSource rds = new RabbitDataSource();
        rds.setUrl("jdbc:mysql://localhost:3306/cas?useUnicode=true&characterEncoding=UTF-8&useServerPrepStmts=true");
        rds.setUsername("root");
        rds.setPassword("123");
        rds.setDriverClass("com.mysql.jdbc.Driver");
        rds.init();
        TxDataSource txDataSource = new TxDataSource(rds, "test-data-source-name", null);
        testGetConnection(txDataSource);
        // 测试数据源方法
        testDatasourceMethods(rds, txDataSource);
        // 验证txConnection
        txConnectionTest(txDataSource);
        // 验证txPreparedStatement
        txPreparedStatementTest(txDataSource);
        rds.shutdown();
        Field dataSourceMap = TxDataSource.class.getDeclaredField("dataSourceMap");
        dataSourceMap.setAccessible(true);
        Map<String, TxDataSource> o = (Map<String, TxDataSource>) dataSourceMap.get(null);
        o.remove(txDataSource.getDataSourceName());
    }

    @SuppressWarnings("deprecation")
	private void txPreparedStatementTest(TxDataSource txDataSource) throws SQLException, FileNotFoundException {
        TxConnection txConn = (TxConnection) txDataSource.getConnection();
        TxPreparedStatement txStmt = (TxPreparedStatement) txConn.prepareStatement("select * from t_user where id = ?");
        txStmt.setInt(1, 1);
        txStmt.setBlob(1, new FileInputStream(new File(getClass().getClassLoader().getResource("test.properties").getFile())), 1);
        txStmt.setURL(1, getClass().getClassLoader().getResource("test.properties"));
        txStmt.setNString(1, "xx");
        txStmt.setObject(1, 1, Types.BIGINT);
        txStmt.setLong(1, 1L);
        txStmt.setByte(1, (byte)1);
        txStmt.setBytes(1, "hello".getBytes());
        txStmt.setFloat(1, 0.1f);
        txStmt.setTime(1, new Time(1000L));
        txStmt.setBigDecimal(1, new BigDecimal("10"));
        txStmt.setShort(1, (byte)1);
        txStmt.setNull(1, Types.INTEGER);
        txStmt.setBoolean(1, true);
        txStmt.setDate(1, new Date(11,2,3));
        txStmt.setFetchSize(10);
        TestCase.assertEquals(txStmt.getFetchSize(), 10);
        txStmt.setQueryTimeout(11);
        TestCase.assertEquals(txStmt.getQueryTimeout(), 11);
        txStmt.clearParameters();
        txStmt.clearBatch();
        txStmt.clearWarnings();
        TestCase.assertNotNull(txStmt.getMetaData());
        txStmt.setInt(1, 1);
        txStmt.executeQuery();
        txStmt.getWarnings();
        txStmt.cancel();
        txStmt.close();
        txConn.close();
    }

    private void txConnectionTest(TxDataSource txDataSource) throws SQLException {
        TxConnection txConn = (TxConnection) txDataSource.getConnection();
        Connection conn = txConn.getRealConn();

        txConn.setAutoCommit(true);
        TestCase.assertTrue(txConn.getAutoCommit());
        txConn.setAutoCommit(false);
        TestCase.assertTrue(!txConn.getAutoCommit());

        txConn.setReadOnly(true);
        TestCase.assertEquals(txConn.isReadOnly(), conn.isReadOnly());

        txConn.setCatalog("cas");
        TestCase.assertEquals(txConn.getCatalog(), "cas");

        txConn.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
        TestCase.assertEquals(txConn.getTransactionIsolation(), Connection.TRANSACTION_SERIALIZABLE);
        txConn.clearWarnings();
        TestCase.assertNull(txConn.getWarnings());
        txConn.unwrap(Connection.class);
        TestCase.assertEquals(txConn.isWrapperFor(Connection.class), conn.isWrapperFor(Connection.class));
        txConn.setSchema("t_user");
        TestCase.assertEquals(txConn.getSchema(), conn.getSchema());


        txConn.setHoldability(ResultSet.CLOSE_CURSORS_AT_COMMIT);
        TestCase.assertEquals(txConn.getHoldability(), conn.getHoldability());
        Properties clientInfo = new Properties();
        clientInfo.setProperty("hello", "kitty");
        txConn.setClientInfo(clientInfo);
        TestCase.assertEquals(txConn.getClientInfo(), clientInfo);
        HashMap<String, Class<?>> map = new HashMap<>();
        map.put("VARCHAR2", String.class);
        txConn.setTypeMap(map);
        TestCase.assertEquals(txConn.getTypeMap(), conn.getTypeMap());
        TestCase.assertEquals(txConn.getTypeMap().get("VARCHAR2"), String.class);

        txConn.setClientInfo("key", "tx");
        TestCase.assertEquals(txConn.getClientInfo("key"), "tx");

        TestCase.assertEquals(txConn.getNetworkTimeout(), conn.getNetworkTimeout());
        TestCase.assertEquals(txConn.isValid(1000), conn.isValid(1000));

        txConn.close();
        TestCase.assertEquals(txConn.isClosed(), conn.isClosed());
    }


    private void testDatasourceMethods(RabbitDataSource rds, TxDataSource txDataSource) throws SQLException, FileNotFoundException, URISyntaxException {
        TestCase.assertEquals(rds.isWrapperFor(TxDataSourceTest.class), txDataSource.isWrapperFor(TxDataSourceTest.class));
        TestCase.assertNull(txDataSource.unwrap(TxDataSourceTest.class));
        TestCase.assertEquals(rds.isWrapperFor(TxDataSourceTest.class), txDataSource.isWrapperFor(TxDataSourceTest.class));
        txDataSource.setLoginTimeout(100);
        TestCase.assertEquals(txDataSource.getLoginTimeout(), rds.getLoginTimeout());
        URL resource = getClass().getClassLoader().getResource("test.properties");
        PrintWriter out = new PrintWriter(new File(resource.toURI()).getAbsolutePath());
        txDataSource.setLogWriter(out);
        TestCase.assertEquals(txDataSource.getLogWriter(), rds.getLogWriter());
        out.close();
        TestCase.assertEquals(txDataSource.getParentLogger(), rds.getParentLogger());
    }

    private void testGetConnection(TxDataSource txDataSource) {
        try {
            txDataSource.getConnection("root", "123");
            throw new RuntimeException();
        } catch (Exception e) {
            TestCase.assertEquals(RabbitDMLException.class, e.getClass());
        }
    }

}
