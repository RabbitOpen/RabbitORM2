package rabbit.open.test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import junit.framework.TestCase;
import rabbit.open.orm.common.exception.EmptyAliasException;
import rabbit.open.orm.common.exception.NoNamedSQLDefinedException;
import rabbit.open.orm.common.exception.RepeatedAliasException;
import rabbit.open.orm.common.exception.UnExistedNamedSQLException;
import rabbit.open.orm.common.exception.UnKnownFieldException;
import rabbit.open.orm.core.dml.NamedQuery;
import rabbit.open.test.entity.Car;
import rabbit.open.test.entity.Department;
import rabbit.open.test.entity.Organization;
import rabbit.open.test.entity.Property;
import rabbit.open.test.entity.Resources;
import rabbit.open.test.entity.Role;
import rabbit.open.test.entity.Team;
import rabbit.open.test.entity.User;
import rabbit.open.test.entity.Zone;
import rabbit.open.test.service.CarService;
import rabbit.open.test.service.DepartmentService;
import rabbit.open.test.service.OrganizationService;
import rabbit.open.test.service.PropertyService;
import rabbit.open.test.service.ResourcesService;
import rabbit.open.test.service.RoleService;
import rabbit.open.test.service.TeamService;
import rabbit.open.test.service.UserService;
import rabbit.open.test.service.ZoneService;

/**
 * <b>Description: NamedQuery测试</b><br>
 * <b>@author</b> 肖乾斌
 * 
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:applicationContext.xml" })
public class NamedQueryTest {

    @Autowired
    UserService us;

    @Autowired
    RoleService rs;

    @Autowired
    OrganizationService os;

    @Autowired
    ResourcesService resService;

    @Autowired
    CarService cs;

    @Autowired
    ZoneService zs;

    @Autowired
    PropertyService ps;

    @Autowired
    TeamService ts;

    @Autowired
    DepartmentService ds;

    /**
     * 
     * <b>Description: 命名查询测试</b><br>
     * 
     * @throws Exception
     * 
     */
    @Test
    public void namedQueryTest() {
        User user = createTestData();
        User u = us.createNamedQuery("getUserByName")
                .set("username", "%leifeng%").set("userId", user.getId())
                .unique();
        System.out.println(u);
        TestCase.assertEquals(user.getName(), u.getName());
        TestCase.assertEquals(u.getCars().size(), 3);
        TestCase.assertEquals(u.getRoles().size(), 2);
        TestCase.assertEquals(u.getOrg().getZone().getName(), user.getOrg()
                .getZone().getName());
    }

    @Test
    public void setObjectTest() {
        User user = createTestData();
        NamedQuery<User> query = us.createNamedQuery("getUserByName");
        query.set(null);
        User u = query.set(new QueryFilter(user.getName(), 
                user.getId(), 100)).unique();
        System.out.println(u);
        TestCase.assertEquals(user.getName(), u.getName());
        TestCase.assertEquals(u.getCars().size(), 3);
        TestCase.assertEquals(u.getRoles().size(), 2);
        TestCase.assertEquals(u.getOrg().getZone().getName(), user.getOrg()
                .getZone().getName());
    }

    @Test
    public void multiFetchByXmlTest() {
        Team t = addTestData();
        Department d = new Department("成都研发中心", t);
        ds.add(d);
        Department dept = ds.createNamedQuery("multiFetchByXml")
                .set("deptID", d.getId()).execute().unique();
        System.out.println(dept);

        TestCase.assertNotNull(dept.getTeam().getLeader());
        TestCase.assertNotNull(dept.getTeam().getFollower());
        TestCase.assertEquals(dept.getTeam().getLeader().getName(), t
                .getLeader().getName());
        TestCase.assertEquals(dept.getTeam().getFollower().getName(), t
                .getFollower().getName());
        TestCase.assertEquals(dept.getTeam().getName(), t.getName());
        TestCase.assertEquals(dept.getTeam().getId(), t.getId());

        List<Department> list = ds.createNamedQuery("multiFetchAll").list();
        TestCase.assertTrue(list.size() > 0);
    }

    private Team addTestData() {
        User leader = new User();
        leader.setAge(10);
        leader.setName("leader");
        us.add(leader);
        User follower = new User();
        follower.setAge(11);
        follower.setName("follower");
        us.add(follower);

        Team t = new Team("myteam", leader, follower);
        ts.add(t);
        return t;
    }

    @Test
    public void countTest() {
        User user = createTestData();
        long count = us.createNamedQuery("getUserByName")
                .set("username", "%leifeng%").set("userId", user.getId())
                .count();
        // 角色个数 * 车辆个数 * 属性个数【 笛卡尔积】
        TestCase.assertEquals(2 * 3 * 2, count);
    }

    @Test
    public void emptyAliasExceptionTest() {
        try {
            us.createNamedQuery("emptyAliasExceptionTest").set("userId", 1)
                    .execute().unique();
            throw new RuntimeException();
        } catch (Exception e) {
            TestCase.assertEquals(e.getClass(), EmptyAliasException.class);
        }
    }

    @Test
    public void repeatedAliasExceptionTest() {
        try {
            us.createNamedQuery("repeatedAliasExceptionTest").set("userId", 1)
                    .execute().unique();
            throw new RuntimeException();
        } catch (Exception e) {
            TestCase.assertEquals(e.getClass(), RepeatedAliasException.class);
        }
    }

    @Test
    public void noNamedSQLDefinedTest() {
        try {
            os.createNamedQuery("xx").execute();
            throw new RuntimeException();
        } catch (Exception e) {
            TestCase.assertEquals(NoNamedSQLDefinedException.class,
                    e.getClass());
        }
    }

    @Test
    public void unExistedNamedSQLTest() {
        try {
            us.createNamedQuery("xx").execute();
            throw new RuntimeException();
        } catch (Exception e) {
            TestCase.assertEquals(UnExistedNamedSQLException.class,
                    e.getClass());
        }
    }

    @Test
    public void unKnownFieldExceptionTest() {
        try {
            ds.createNamedQuery("multiFetchAll").set("id", 1).execute()
                    .unique();
            throw new RuntimeException();
        } catch (Exception e) {
            TestCase.assertEquals(UnKnownFieldException.class, e.getClass());
        }
    }

    /**
     * 
     * <b>Description: 生成测试数据数据</b><br>
     * 
     */
    private User createTestData() {
        Zone z = new Zone("华北");
        zs.add(z);
        User user = new User();
        // 添加组织
        Organization org = new Organization("FBI", "联邦调查局", z);
        os.add(org);

        // 添加角色
        List<Role> roles = new ArrayList<Role>();
        for (int i = 555; i < 555 + 2; i++) {
            Role r = new Role("R" + i);
            rs.add(r);
            roles.add(r);
            // 构建资源
            List<Resources> resources = new ArrayList<Resources>();
            for (int j = 0; j < 2; j++) {
                Resources rr = new Resources("baidu_" + j + i + ".com");
                resService.add(rr);
                resources.add(rr);
            }
            // 添加角色资源映射关系
            r.setResources(resources);
            rs.addJoinRecords(r);
        }

        // 添加用户
        user.setOrg(org);
        user.setBigField(new BigDecimal(1));
        user.setShortField((short) 1);
        user.setDoubleField(0.1);
        user.setFloatField(0.1f);

        user.setName("leifeng" + System.currentTimeMillis());
        user.setBirth(new Date());
        us.add(user);

        // 添加用户角色之间的映射关系
        user.setRoles(roles);
        us.addJoinRecords(user);

        // 添加车辆
        cs.add(new Car("川A110", user));
        cs.add(new Car("川A120", user));
        cs.add(new Car("川A130", user));

        ps.add(new Property(user.getOrg().getId(), "P1"));
        ps.add(new Property(user.getOrg().getId(), "P2"));

        return user;
    }
    
    public class QueryFilter {
        public String username;
        public Long userId;
        public Integer year;
        public String gender;
        public QueryFilter(String username, Long id, Integer year) {
            super();
            this.username = username;
            this.userId = id;
            this.year = year;
        }
    }
}
