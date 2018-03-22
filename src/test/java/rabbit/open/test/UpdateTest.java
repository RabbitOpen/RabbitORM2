package rabbit.open.test;

import java.util.Date;

import junit.framework.TestCase;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import rabbit.open.orm.dml.Update;
import rabbit.open.orm.exception.RabbitDMLException;
import rabbit.open.orm.exception.UnKnownFieldException;
import rabbit.open.test.entity.Organization;
import rabbit.open.test.entity.User;
import rabbit.open.test.service.OrganizationService;
import rabbit.open.test.service.UserService;

/**
 * <b>Description: update测试</b><br>
 * <b>@author</b> 肖乾斌
 * 
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:applicationContext.xml" })
public class UpdateTest {

    @Autowired
    UserService us;

    @Autowired
    OrganizationService os;

    /**
     * 
     * <b>Description: update测试</b><br>
     * 
     */
    @Test
    public void updateTest() {
        Organization org = new Organization();
        org.setId(510L);
        User user = new User("lili", 510, new Date(), org);
        us.add(user);
        us.createUpdate().addFilter("id", user.getId()).set("org", 12)
                .setValue(user).setNull("birth").set("name", "lisi").execute();
        user = us.createQuery().addFilter("id", user.getId())
                .fetch(Organization.class).execute().unique();
        TestCase.assertEquals(user.getName(), user.getName());

    }
    
    @Test
    public void updaterTest() {
        User user = new User("lilixs", 510, new Date(), null);
        us.add(user);
        Update<User> update = us.createUpdate().addFilter("id", user.getId());
        update.getUpdater().setName("newName");
        update.getUpdater().setAge(100);
        update.execute();
        User u = us.createQuery().addFilter("id", user.getId()).unique();
        TestCase.assertEquals(u.getName(), update.getUpdater().getName());
        TestCase.assertEquals(u.getAge(), update.getUpdater().getAge());
        
    }

    /**
     * 
     * <b>Description: update测试</b><br>
     * 
     */
    @Test
    public void updateFilter() {
        Organization org = new Organization();
        org.setId(110L);
        User user = new User("lili", 10, null, org);
        us.add(user);
        us.createUpdate().addFilter("id", user.getId()).addFilter("org", 110L)
                .set("name", "lisi").execute();
        User u = us.createQuery().addFilter("id", user.getId()).execute()
                .unique();
        TestCase.assertEquals(u.getName(), "lisi");
    }

    /**
     * <b>Description 异常测试</b>
     */
    @Test
    public void exceptionTest() {
        try {
            us.createUpdate().set("id", 10).addFilter("idx", 1).execute();
            throw new RuntimeException();
        } catch (Exception e) {
            TestCase.assertSame(UnKnownFieldException.class, e.getClass());
        }
        try {
            us.createUpdate().execute();
            throw new RuntimeException();
        } catch (Exception e) {
            TestCase.assertSame(RabbitDMLException.class, e.getClass());
        }

        try {
            User u = new User();
            u.setName("xxx");
            us.createUpdate().updateByID(u);
            throw new RuntimeException();
        } catch (Exception e) {
            TestCase.assertSame(RabbitDMLException.class, e.getClass());
        }
    }

    @Test
    public void invalidFieldTest() {
        try {
            us.createUpdate().set("idxs", 10).addFilter("id", 1).execute();
        } catch (Exception e) {
            TestCase.assertSame(UnKnownFieldException.class, e.getClass());
        }
    }

    /**
     * 
     * <b>Description: update测试</b><br>
     * 
     */
    @Test
    public void updateNullFilter() {
        User user = new User("lili", 10, null, null);
        us.add(user);
        us.createUpdate().addNullFilter("birth").set("name", "lisi").execute();
        User u = us.createQuery().addFilter("id", user.getId()).unique();
        TestCase.assertEquals(u.getName(), "lisi");

    }

    /**
     * 
     * <b>Description: update测试</b><br>
     * 
     */
    @Test
    public void update2Null() {
        User user = new User("lili", 10, null, null);
        us.add(user);
        us.createUpdate(user).addFilter("id", user.getId()).set("name", null)
                .execute();
        User u = us.createQuery().addFilter("id", user.getId()).execute()
                .unique();
        TestCase.assertNull(u.getName());
    }

    /**
     * 
     * <b>Description: 多表联合更新</b><br>
     * 
     * @throws Exception
     * 
     */
    @Test
    public void joinUpdate() throws Exception {
        Organization org = new Organization();
        org.setName("myorg");
        os.add(org);
        User user = new User("lili", 10, null, org);
        us.add(user);
        String newName = "lisi44";
        us.createUpdate().addFilter("id", user.getId()).set("name", newName)
                .addFilter("id", org.getId(), Organization.class, User.class)
                .execute();
        User u = us.createQuery().addFilter("id", user.getId()).execute()
                .unique();
        TestCase.assertEquals(newName, u.getName());
    }

    /**
     * 
     * <b>Description: updateByIDTest测试</b><br>
     * 
     */
    @Test
    public void updateByIDTest() {
        Organization org = new Organization("code", "name");
        os.add(org);
        org.setName("newname");
        os.updateByID(org);
        TestCase.assertEquals(org.getName(), "newname");
    }
}
