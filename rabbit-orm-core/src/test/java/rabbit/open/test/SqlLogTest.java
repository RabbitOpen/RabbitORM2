package rabbit.open.test;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import rabbit.open.orm.core.dml.Query;
import rabbit.open.test.entity.Role;
import rabbit.open.test.service.RoleService;

/**
 * <b>Description: 查询测试</b><br>
 * <b>@author</b> 肖乾斌
 * 
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:applicationContext.xml" })
public class SqlLogTest {
    
    @Autowired
    RoleService rs;
    
    @Test
    public void showLog() {
        Query<Role> query = rs.createQuery();
        query.showMaskedPreparedSql();
        query.showUnMaskedSql();
        query.addFilter("roleName", "r1").list();
        query.showMaskedPreparedSql();
        query.showUnMaskedSql();
    }
}
