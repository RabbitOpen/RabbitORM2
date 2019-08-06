package sqlite.test;

import java.util.Date;

import junit.framework.TestCase;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import sqlite.test.entity.SQLiteOrganization;
import sqlite.test.entity.SQLiteUser;
import sqlite.test.service.SQLiteOrganizationService;
import sqlite.test.service.SQLiteUserService;

/**
 * <b>Description: delete测试</b><br>
 * <b>@author</b> 肖乾斌
 * 
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:applicationContext-sqlite.xml" })
public class SQLiteDeleteTest {

    @Autowired
    SQLiteUserService us;

    @Autowired
    SQLiteOrganizationService os;

    public SQLiteUser addInitUser() {
        SQLiteUser user = new SQLiteUser();
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
        addInitUser();
        TestCase.assertEquals(1, us.createQuery().count());
        us.clearAll();
        TestCase.assertEquals(0, us.createQuery().count());
    }

    @Test
    public void deleteByID() {
        SQLiteUser user = addInitUser();
        TestCase.assertEquals(1, us.createQuery().addFilter("id", user.getId())
                .count());
        us.deleteByID(user.getId());
        TestCase.assertEquals(0, us.createQuery().addFilter("id", user.getId())
                .count());
    }

    @Test
    public void delete() {
        SQLiteUser user = addInitUser();
        TestCase.assertNotNull(us.createQuery(user).execute().unique());
        us.delete(user);
        TestCase.assertNull(us.createQuery(user).execute().unique());
    }

    @Test
    public void deleteFilterTest() {
        SQLiteUser user = addInitUser();
        TestCase.assertEquals(1, us.createQuery().addFilter("id", user.getId())
                .count());
        us.createDelete().addFilter("id", user.getId()).execute();
        TestCase.assertEquals(0, us.createQuery().addFilter("id", user.getId())
                .count());
    }

    @Test
    public void joinDelete() {
        SQLiteOrganization o = new SQLiteOrganization("deleteOrg", "deleteOrg");
        os.add(o);
        SQLiteUser user = new SQLiteUser();
        user.setName("wangwu");
        user.setOrg(o);
        us.add(user);
        TestCase.assertNotNull(us.getByID(user.getId()));
        long result = us
                .createDelete()
                .addNullFilter("birth")
                .addNullFilter("birth", true)
                .addFilter("orgCode", o.getOrgCode(), SQLiteOrganization.class,
                        SQLiteUser.class).execute();
        TestCase.assertEquals(1, result);
        TestCase.assertNull(us.getByID(user.getId()));
    }

}
