package rabbit.open.test;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import rabbit.open.orm.dml.SQLCallBack;
import rabbit.open.test.service.UserService;

/**
 * <b>Description: 	SQLQuery测试</b><br>
 * <b>@author</b>	肖乾斌
 * 
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations={"classpath:applicationContext.xml"})
public class SQLQueryTest {

	@Autowired
	UserService us;
	
	/**
	 * 
	 * <b>Description:	命名查询测试</b><br>	
	 * @throws Exception 
	 * 
	 */
	@Test
	public void sqlQueryTest() throws Exception{
		Long count = us.createSQLQuery("countUser", new SQLCallBack<Long>() {
			@Override
			public Long execute(PreparedStatement stmt) {
				ResultSet rs = null;
				try {
					rs = stmt.executeQuery();
					rs.next();
					return rs.getLong(1);
				} catch (SQLException e) {
					e.printStackTrace();
					return 0L;
				} finally {
				    closeResultSet(rs);
				}
			}

            private void closeResultSet(ResultSet rs) {
                if(null == rs){
                    return;
                }
                try {
                    rs.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
		}).execute();
		System.out.println(count);
	}

}
