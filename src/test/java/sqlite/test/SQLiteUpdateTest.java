package sqlite.test;

import java.text.SimpleDateFormat;
import java.util.Date;

import junit.framework.TestCase;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import rabbit.open.orm.exception.UnKnownFieldException;
import sqlite.test.entity.SQLiteOrganization;
import sqlite.test.entity.SQLiteUser;
import sqlite.test.service.SQLiteOrganizationService;
import sqlite.test.service.SQLiteUserService;

/**
 * <b>Description: update测试</b><br>
 * <b>@author</b> 肖乾斌
 * 
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:applicationContext-sqlite.xml" })
public class SQLiteUpdateTest {

    @Autowired
    SQLiteUserService us;

    @Autowired
    SQLiteOrganizationService os;

    /**
     * 
     * <b>Description: update测试</b><br>
     * 
     */
    @Test
    public void updateTest() {
        SQLiteOrganization org = new SQLiteOrganization();
        org.setId(510L);
        SQLiteUser user = new SQLiteUser("lili", 510, new Date(), org);
        us.add(user);
        us.createUpdate().addFilter("id", user.getId()).set("org", 12)
                .setValue(user).setNull("birth").set("name", "lisi").execute();
        user = us.createQuery().addFilter("id", user.getId())
                .fetch(SQLiteOrganization.class).execute().unique();
        TestCase.assertEquals(user.getName(), "lisi");

    }

    /**
     * 
     * <b>Description: update测试</b><br>
     * 
     */
    @Test
    public void updateFilter() {
        SQLiteOrganization org = new SQLiteOrganization();
        org.setId(110L);
        SQLiteUser user = new SQLiteUser("lili", 10, null, org);
        us.add(user);
        us.createUpdate().addFilter("id", user.getId()).addFilter("org", 110L)
                .set("name", "lisi").execute();
        SQLiteUser u = us.createQuery().addFilter("id", user.getId()).execute()
                .unique();
        TestCase.assertEquals(u.getName(), "lisi");
    }

    /**
     * <b>Description 测试添加非法属性作为过滤条件</b>
     */
    @Test
    public void invalidFilterTest() {
        try {
            us.createUpdate().set("id", 10).addFilter("idx", 1).execute();
        } catch (Exception e) {
            TestCase.assertSame(UnKnownFieldException.class, e.getClass());
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
        SQLiteUser user = new SQLiteUser("lili", 10, null, null);
        us.add(user);
        us.createUpdate().addNullFilter("birth").set("name", "lisi").execute();
        SQLiteUser u = us.createQuery().addFilter("id", user.getId()).execute()
                .unique();
        TestCase.assertEquals(u.getName(), "lisi");

    }

    /**
     * 
     * <b>Description: update测试</b><br>
     * 
     */
    @Test
    public void update2Null() {
        SQLiteUser user = new SQLiteUser("lili", 10, null, null);
        us.add(user);
        us.createUpdate(user).addFilter("id", user.getId()).set("name", null)
                .execute();
        SQLiteUser u = us.createQuery().addFilter("id", user.getId()).execute()
                .unique();
        TestCase.assertNull(u.getName());
    }

    /**
     * 
     * <b>Description: 多表联合更新</b><br>
     * @throws Exception
     * 
     */
    @Test
    public void joinUpdate() throws Exception {
        SQLiteOrganization org = new SQLiteOrganization();
        org.setName("myorg");
        os.add(org);
        SQLiteUser user = new SQLiteUser("lili", 10, null, org);
        us.add(user);
        String newName = "lisi44";
        Date birth = new Date();
        us.createUpdate().addFilter("id", user.getId()).set("name", newName)
                .set("birth", birth)
                .addFilter("id", org.getId(), SQLiteOrganization.class, SQLiteUser.class)
                .execute();
        SQLiteUser u = us.createQuery().addFilter("id", user.getId()).execute()
                .unique();
        TestCase.assertEquals(newName, u.getName());
        TestCase.assertEquals(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(birth), 
                new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(u.getBirth()));
    }

    /**
     * 
     * <b>Description: updateByIDTest测试</b><br>
     * 
     */
    @Test
    public void updateByIDTest() {
        SQLiteOrganization org = new SQLiteOrganization("code", "name");
        os.add(org);
        org.setName("newname");
        os.updateByID(org);
        TestCase.assertEquals(org.getName(), "newname");
    }
}
