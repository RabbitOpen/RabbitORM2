package rabbit.open.test;

import java.text.ParseException;
import java.text.SimpleDateFormat;
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
import rabbit.open.test.entity.Zone;
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
        TestCase.assertNull(user.getBirth());

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
            User u = new User();
            u.setId(1L);
            us.createUpdate().updateByID(u);
            throw new RuntimeException();
        } catch (Exception e) {
            TestCase.assertSame(RabbitDMLException.class, e.getClass());
        }
        try {
            us.createUpdate().execute();
            throw new RuntimeException();
        } catch (Exception e) {
            TestCase.assertSame(RabbitDMLException.class, e.getClass());
        }
        try {
            us.createUpdate().set("id", 10).addFilter("idx", 1).execute();
            throw new RuntimeException();
        } catch (Exception e) {
            TestCase.assertSame(UnKnownFieldException.class, e.getClass());
        }

        try {
            User u = new User();
            u.setName("xxx");
            us.createUpdate().updateByID(u);
            throw new RuntimeException();
        } catch (Exception e) {
            TestCase.assertSame(RabbitDMLException.class, e.getClass());
        }

        try {
            us.createUpdate().updateByID(new User());
            throw new RuntimeException();
        } catch (Exception e) {
            TestCase.assertSame(RabbitDMLException.class, e.getClass());
        }
    }
    
    @Test
    public void excludeTest() throws ParseException {
    	SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		User user = new User("lili", 10, sdf.parse("2018-10-10"), null);
    	us.add(user);
    	
    	User update = new User("lilix", 12, sdf.parse("2019-10-10"), null);
    	update.setId(user.getId());
    	us.createUpdate().exclude("age").updateByID(update);
    	User get = us.getByID(user.getId());
    	TestCase.assertEquals(update.getName(), get.getName());
    	TestCase.assertEquals(update.getBirth(), get.getBirth());
    	TestCase.assertEquals(user.getAge(), get.getAge());
    	
    	User replace = new User("replace", 13, sdf.parse("2011-10-10"), null);
    	replace.setId(user.getId());
    	us.createUpdate().exclude("age", "name").replaceByID(replace);
    	get = us.getByID(user.getId());
    	TestCase.assertEquals(update.getName(), get.getName());
    	TestCase.assertEquals(replace.getBirth(), get.getBirth());
    	TestCase.assertEquals(user.getAge(), get.getAge());
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
        Zone z = new Zone();
        z.setId(1993L);
        org.setName("newname");
        org.setZone(z);
        TestCase.assertEquals(1, os.updateByID(org));
        Organization oq = os.createQuery().addFilter("id", org.getId()).fetch(Zone.class).unique();
        TestCase.assertEquals(oq.getName(), "newname");
        TestCase.assertEquals(oq.getZone().getId(), z.getId());
    }

    @Test
    public void replaceByIDTest() {
    	User u = new User();
    	String name = "lisi";
		u.setName(name);
    	u.setAge(10);
    	us.add(u);
    	u = us.getByID(u.getId());
    	TestCase.assertEquals(u.getName(), name);
    	TestCase.assertEquals(u.getAge().intValue(), 10);
    	
    	User r = new User();
    	name = "wangwu";
    	r.setId(u.getId());
    	r.setName(name);
    	TestCase.assertEquals(1, us.replaceByID(r));
    	u = us.getByID(u.getId());
    	TestCase.assertEquals(u.getName(), name);
    	TestCase.assertNull(u.getAge());
    	
    	
    }
    
    /**
     * 
     * <b>Description: addNotNullFilterTest</b><br>
     * 
     */
    @Test
    public void addNotNullFilterTest() {
    	User user = new User();
    	user.setName("wangwu");
    	us.add(user);
    	long result = us.createUpdate().addNotNullFilter("name")
    			.addFilter("id", user.getId()).set("name", "lisi").execute();
    	TestCase.assertTrue(result >= 1);
    	
    }
    
}
