package rabbit.open.test;

import junit.framework.TestCase;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import rabbit.open.common.exception.NoField2InsertException;
import rabbit.open.common.exception.RabbitDMLException;
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
    public void addDataTest2() {
    	String name = "testUserName";
    	int count = 100;
		for (int i = 0; i < count; i++) {
    		User user = new User();
			user.setName(name);
        	us.add(user);
    	}
    	TestCase.assertEquals(count, us.createQuery().addFilter("name", name).count());
    	
    }

    @Test
    public void exceptionTest() {
        uus.add(new UUIDPolicyEntity());
        try {
            us.add(null);
            throw new RuntimeException();
        } catch (Exception e) {
            TestCase.assertSame(RabbitDMLException.class, e.getClass());
        }
        try {
            us.add(new User());
            throw new RuntimeException();
        } catch (Exception e) {
            TestCase.assertSame(NoField2InsertException.class, e.getClass());
        }
        
    }
}
