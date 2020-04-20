package rabbit.open.test;

import junit.framework.TestCase;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import rabbit.open.orm.core.dml.Delete;
import rabbit.open.test.entity.Organization;
import rabbit.open.test.entity.User;
import rabbit.open.test.service.OrganizationService;
import rabbit.open.test.service.UserService;

import java.util.Date;

/**
 * <b>Description: delete测试</b><br>
 * <b>@author</b> 肖乾斌
 * 
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:applicationContext.xml" })
public class DeleteTest {

    @Autowired
    UserService us;

    @Autowired
    OrganizationService os;

    public User addInitUser() {
        User user = new User();
        user.setName("wangwu");
        user.setBirth(new Date());
        us.add(user);
        return user;
    }

    /**
     * 
     * <b>Description: 清除表中所有数据</b><br>
     * 
     */
    @Test
    public void clear() {
    	long before = us.createQuery().count();
        addInitUser();
        TestCase.assertEquals(before + 1, us.createQuery().count());
        us.clearAll();
        TestCase.assertEquals(0, us.createQuery().count());
    }

    @Test
    public void deleteByID() {
        User user = addInitUser();
        TestCase.assertEquals(1, us.createQuery().addFilter("id", user.getId())
                .count());
        us.deleteByID(user.getId());
        TestCase.assertEquals(0, us.createQuery().addFilter("id", user.getId())
                .count());
    }

    @Test
    public void delete() {
        User user = addInitUser();
        TestCase.assertNotNull(us.createQuery(user).execute().unique());
        us.delete(user);
        TestCase.assertNull(us.createQuery(user).execute().unique());
    }

    @Test
    public void deleteFilterTest() {
        User user = addInitUser();
        TestCase.assertEquals(1, us.createQuery().addFilter("id", user.getId())
                .count());
        us.createDelete().addFilter("id", user.getId()).execute();
        TestCase.assertEquals(0, us.createQuery().addFilter("id", user.getId())
                .count());
    }

    @Test
    public void joinDelete() {
        Organization o = new Organization("deleteOrg", "deleteOrg");
        os.add(o);
        User user = new User();
        user.setName("wangwu");
        user.setOrg(o);
        us.add(user);
        Delete<User> delete = us
                .createDelete(user)
                .addNullFilter("birth")
                .addNullFilter("birth", true)
                .addFilter("orgCode", o.getOrgCode(), Organization.class,
                        User.class);
		long result = delete.execute();
        TestCase.assertEquals(1, result);
    }

    /**
     * 
     * <b>Description: addNotNullFilterTest</b><br>
     * 
     */
    @Test
    public void addNotNullFilterTest() {
    	Organization o = new Organization("deleteOrg", "deleteOrg");
    	os.add(o);
    	User user = new User();
    	user.setName("wangwu");
    	user.setOrg(o);
    	us.add(user);
    	long result = us.createDelete()
    			.addNotNullFilter("name")
    			.addFilter("orgCode", o.getOrgCode(), Organization.class,
    					User.class).execute();
    	TestCase.assertTrue(result >= 1);
    	
    }
    
    @Test
    public void namedDeleteTest() {
    	User user = new User();
    	user.setName("wangwuxxx");
    	us.add(user);
    	TestCase.assertNotNull(us.getByID(user.getId()));
    	us.createNamedDelete("namedDelete").set("userId", user.getId(), null, null).execute();
    	TestCase.assertNull(us.getByID(user.getId()));
    }

}
