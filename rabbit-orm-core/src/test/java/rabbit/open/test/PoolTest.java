package rabbit.open.test;

import junit.framework.TestCase;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import rabbit.open.orm.datasource.RabbitDataSource;
import rabbit.open.test.entity.User;
import rabbit.open.test.service.UserService;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Date;

/**
 * <b>Description 连接池相关测试</b>
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:applicationContext.xml" })
public class PoolTest {

    @Autowired
    UserService us;

    /**
     * <b>Description 测试SQLException导致连接被回收</b>
     * 
     * @throws IOException
     */
    @Test
    public void rabbitDataSourceTest() throws IOException {
        DataSource dataSource = us.getFactory().getDataSource();
        if (!(dataSource instanceof RabbitDataSource)) {
            return;
        }
        RabbitDataSource rds = (RabbitDataSource) dataSource;
        User data = new User("xxxx", 10, new Date());
        long counter = rds.getCounter();
        try {
            us.add(data);
            us.createQuery().count();
            //重复添加，主键冲突
            us.add(data);
            throw new RuntimeException();
        } catch (Exception e) {
            TestCase.assertTrue(SQLException.class
                    .isAssignableFrom(getRootCause(e).getClass()));
            TestCase.assertEquals(rds.getCounter(), counter - 1);
        }

    }
    
    public static Throwable getRootCause(Exception e) {
        Throwable cause = e;
        while(null != cause.getCause()) {
            cause = cause.getCause();
        }
        return cause;
    }

}
