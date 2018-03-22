package rabbit.open.test;

import junit.framework.TestCase;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import rabbit.open.orm.exception.RabbitDMLException;
import rabbit.open.test.entity.Organization;
import rabbit.open.test.entity.UUIDPolicyEntity;
import rabbit.open.test.entity.User;
import rabbit.open.test.service.UUIDEntityService;
import rabbit.open.test.service.UserService;

/**
 * <b>Description: 新增测试</b><br>
 * <b>@author</b> 肖乾斌
 * 
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:applicationContext.xml" })
public class InsertTest {

    @Autowired
    UserService us;

    @Autowired
    UUIDEntityService uus;

    /**
     * 
     * <b>Description: 新增数据测试</b><br>
     * 
     */
    @Test
    public void addDataTest() {
        User user = new User();
        user.setName("zhangsan");
        us.add(user);
        System.out.println(user);
        user = new User();
        user.setName("zhangsan1");
        user.setOrg(new Organization("MY_ORG", "MY_ORG_NAME"));
        us.add(user);
        System.out.println(user);

        // uuid策略测试
        UUIDPolicyEntity data = new UUIDPolicyEntity("lisi");
        uus.add(data);
        System.out.println(data);
    }
    
    @Test
    public void exceptionTest() {
        try {
            us.add(null);
            throw new RuntimeException();
        } catch (Exception e) {
            TestCase.assertSame(RabbitDMLException.class, e.getClass());
        }
    }
}
