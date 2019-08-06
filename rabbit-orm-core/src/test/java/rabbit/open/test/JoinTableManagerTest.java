package rabbit.open.test;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import junit.framework.TestCase;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import rabbit.open.common.exception.EmptyPrimaryKeyValueException;
import rabbit.open.orm.pool.jpa.Session;
import rabbit.open.test.entity.Car;
import rabbit.open.test.entity.Organization;
import rabbit.open.test.entity.Role;
import rabbit.open.test.entity.User;
import rabbit.open.test.service.OrganizationService;
import rabbit.open.test.service.RoleService;
import rabbit.open.test.service.UserService;

/**
 * <b>Description: 关联表测试</b><br>
 * <b>@author</b> 肖乾斌
 * 
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:applicationContext.xml" })
public class JoinTableManagerTest {

    @Autowired
    UserService us;

    @Autowired
    RoleService rs;

    @Autowired
    OrganizationService os;

    /**
     * 
     * <b>Description: 添加中间表记录</b><br>
     * 
     */
    @Test
    public void addJoinRecordsTest() {
        User user = addRecords(1);
        User u = query(user);
        TestCase.assertEquals(u.getRoles().size(), 1);
    }

    /**
     * 
     * <b>Description: 删除中间表记录</b><br>
     * 
     */
    @Test
    public void removeJoinRecordsTest() {
        User u = addRecords(2);
        User user = query(u);
        TestCase.assertEquals(user.getRoles().size(), 2);
        us.removeJoinRecords(user);
        user = query(u);
        TestCase.assertNull(user.getRoles());
    }

    /**
     * 
     * <b>Description: 清空中间表记录</b><br>
     * 
     */
    @Test
    public void clearJoinRecordsTest() {
        User u = addRecords(3);
        User user = query(u);
        TestCase.assertEquals(user.getRoles().size(), 3);
        us.clearJoinRecords(u, Role.class);
        user = query(u);
        TestCase.assertNull(user.getRoles());
    }

    @Test
    public void exceptionTest() {
        try {
            User user = new User();
            us.clearJoinRecords(user, Role.class);
            throw new RuntimeException();
        } catch (Exception e) {
            TestCase.assertEquals(Session.getRootCause(e).getClass(),
                    EmptyPrimaryKeyValueException.class);
        }
    }

    /**
     * 
     * <b>Description: 替换中间表记录</b><br>
     * 
     */
    @Test
    public void replaceJoinRecordsTest() {
        User user = addRecords(1);
        Role role = user.getRoles().get(0);
        user = query(user);
        TestCase.assertEquals(user.getRoles().size(), 1);
        List<Role> roles = new ArrayList<Role>();
        for (int i = 3; i < 5; i++) {
            Role r = new Role("R" + i);
            rs.add(r);
            roles.add(r);
        }
        user.setRoles(roles);
        us.replaceJoinRecords(user);
        user = query(user);
        TestCase.assertEquals(user.getRoles().size(), 3);
        TestCase.assertEquals(getRoleByID(roles.get(0).getId(), user.getRoles()).getRoleName(), roles.get(0).getRoleName());
        TestCase.assertEquals(getRoleByID(roles.get(1).getId(), user.getRoles()).getRoleName(), roles.get(1).getRoleName());
        TestCase.assertEquals(getRoleByID(role.getId(), user.getRoles()).getRoleName(), role.getRoleName());
        
    }
    
    private Role getRoleByID(Integer id, List<Role> roles) {
        for (Role r : roles) {
            if (id.equals(r.getId())) {
                return r;
            }
        }
        return null;
    }

    private User addRecords(int roleSize) {
        User user = new User();
        user.setName("zhangsan" + System.currentTimeMillis());
        user.setBirth(new Date());
        us.add(user);
        List<Role> roles = new ArrayList<Role>();
        for (int i = 0; i < roleSize; i++) {
            Role r = new Role("R" + i);
            rs.add(r);
            roles.add(r);
        }
        user.setRoles(roles);
        List<Car> cars = new ArrayList<>();
        Car c = new Car();
        c.setId(1);
        cars.add(c);
        user.setCars(cars);
        us.addJoinRecords(user);
        return user;
    }

    private User query(User user) {
        return us.createQuery(user).joinFetch(Role.class)
                .fetch(Organization.class).execute().unique();
    }

}
