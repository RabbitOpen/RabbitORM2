package rabbit.open.test;

import junit.framework.TestCase;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import rabbit.open.orm.common.exception.AmbiguousDependencyException;
import rabbit.open.orm.common.exception.CycleFetchException;
import rabbit.open.orm.common.exception.RepeatedFetchOperationException;
import rabbit.open.test.entity.*;
import rabbit.open.test.service.*;

import java.util.ArrayList;
import java.util.List;

/**
 * <b>Description 多属性外键fetch</b>
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:applicationContext.xml" })
public class MultiFetchTest {

    @Autowired
    TeamService ts;

    @Autowired
    UserService us;

    @Autowired
    RoleService rs;

    @Autowired
    OrganizationService os;

    @Autowired
    ZoneService zs;

    @Autowired
    CarService cs;

    @Autowired
    DepartmentService ds;

    /**
     * <b>Description 测试查询关联多个同样类型的数据</b>
     * 
     * Team 对象中包含了两个User的场景
     */
    @Test
    public void fetchTest() {
        Team t = addTestData();
        Team team = ts.createQuery().addFilter("id", t.getId())
                .fetch(User.class).execute().unique();

        System.out.println(team);
        TestCase.assertEquals(team.getLeader().getName(), t.getLeader()
                .getName());
        TestCase.assertEquals(team.getFollower().getName(), t.getFollower()
                .getName());
        TestCase.assertEquals(team.getId(), t.getId());
    }

    @Test
    public void multiFetchTest() {
        User leader = new User();
        leader.setDesc("mydesc");
        leader.setAge(10);
        leader.setName("leader");
        us.add(leader);
        User follower = new User();
        follower.setAge(11);
        follower.setName("follower");
        us.add(follower);
        // 添加组织
        Organization org = new Organization("FBI", "联邦调查局");
        os.add(org);
        Team t = new Team("myteam", leader, follower);
        t.setOrg(org);
        ts.add(t);

        // 添加角色
        List<Role> roles = new ArrayList<Role>();
        for (int i = 0; i < 3; i++) {
            Role r = new Role("R" + i);
            rs.add(r);
            roles.add(r);
        }

        t.setRoles(roles);
        ts.addJoinRecords(t);

        Team team = ts.createQuery().addFilter("id", t.getId())
        		.queryFields("org", "leader", "follower")
        		.queryFields(Role.class, "id")
                .fetch(User.class).fetch(Organization.class)
                .joinFetch(Role.class).execute().unique();

        // 验证关联取出了Organization对象
        TestCase.assertEquals(team.getOrg().getName(), org.getName());
        TestCase.assertEquals(team.getOrg().getOrgCode(), org.getOrgCode());

        // 验证取出了角色信息
        TestCase.assertEquals(team.getRoles().size(), roles.size());

        // 验证同时取出了两个User对象
        TestCase.assertEquals(team.getLeader().getName(), t.getLeader()
                .getName());
        TestCase.assertEquals(team.getLeader().getDesc(), "mydesc");
        TestCase.assertEquals(team.getFollower().getName(), t.getFollower()
                .getName());
        TestCase.assertEquals(team.getId(), t.getId());
    }

    @Test
    public void cycleFetchExceptionTest() {
        try {
            ts.createQuery().fetch(Team.class, Organization.class).execute();
            throw new RuntimeException();
        } catch (Exception e) {
            TestCase.assertEquals(e.getClass(), CycleFetchException.class);
        }
    }

    /**
     * <b>Description 在不同的类中取相同类型的实体</b>
     */
    @Test
    public void repeatedFetchExceptionTest() {
        try {
            ts.createQuery().fetch(Organization.class, User.class)
                    .fetch(Organization.class).execute();
            throw new RuntimeException();
        } catch (Exception e) {
            TestCase.assertEquals(e.getClass(),
                    RepeatedFetchOperationException.class);
        }
    }

    /**
     * <b>Description joinFetch时带过滤条件</b>
     */
    @Test
    public void multiFetchWithFilterTest() {
        User leader = new User();
        leader.setAge(10);
        leader.setName("leader");
        us.add(leader);
        User follower = new User();
        follower.setAge(11);
        follower.setName("follower");
        us.add(follower);
        // 添加组织
        Organization org = new Organization("FBI", "联邦调查局");
        os.add(org);
        Team t = new Team("myteam", leader, follower);
        t.setOrg(org);
        ts.add(t);

        Zone zone = new Zone("海南");
        zs.add(zone);

        // 添加角色
        List<Role> roles = new ArrayList<Role>();
        for (int i = 0; i < 3; i++) {
            Role r = new Role("R" + i);
            r.setZone(zone);
            rs.add(r);
            roles.add(r);
        }

        List<Car> cars = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            // 添加车辆
            Car car = new Car("川A11" + i, t);
            car.setZone(zone);
            cs.add(car);
            cars.add(car);
        }
        t.setCars(cars);
        t.setRoles(roles);
        ts.addJoinRecords(t);

        Team team = ts.createQuery().addFilter("id", t.getId())
                .fetch(User.class).fetch(Organization.class)
                .joinFetch(Role.class, roles.get(0))
                .joinFetch(Car.class, cars.get(0)).execute().unique();

        // 验证关联取出了Organization对象
        TestCase.assertEquals(team.getOrg().getName(), org.getName());
        TestCase.assertEquals(team.getOrg().getOrgCode(), org.getOrgCode());

        // 验证取出了角色信息
        TestCase.assertEquals(1, team.getRoles().size());

        // 验证取出了车辆信息
        TestCase.assertEquals(1, team.getCars().size());

        TestCase.assertEquals(zone.getId(), team.getCars().get(0).getZone()
                .getId());

        // 验证同时取出了两个User对象
        TestCase.assertEquals(team.getLeader().getName(), t.getLeader()
                .getName());
        TestCase.assertEquals(team.getFollower().getName(), t.getFollower()
                .getName());
        TestCase.assertEquals(team.getId(), t.getId());
    }

    /**
     * <b>Description 异常测试 多依赖的实体不支持继续深度关联查询！ </b>
     */
    @Test
    public void ambiguousDependencyExceptionTest() {
        Team t = addTestData();

        try {
            ts.createQuery().addFilter("id", t.getId())
                    .fetch(Organization.class, User.class).execute().unique();
            throw new RuntimeException();
        } catch (Exception e) {
            TestCase.assertEquals(e.getClass(),
                    AmbiguousDependencyException.class);
        }

        try {
            ts.createQuery().addFilter("id", t.getId()).fetch(User.class)
                    .addFilter("id", 10, User.class).execute().unique();
            throw new RuntimeException();
        } catch (Exception e) {
            TestCase.assertEquals(e.getClass(),
                    AmbiguousDependencyException.class);
        }

        try {
            ts.createQuery().addFilter("id", t.getId())
                    .fetch(Organization.class, User.class).fetch(User.class)
                    .execute().unique();
            throw new RuntimeException();
        } catch (Exception e) {
            TestCase.assertEquals(e.getClass(),
                    AmbiguousDependencyException.class);
        }

        try {
            ts.createQuery().addFilter("id", t.getId()).buildFetch()
                    .fetch(User.class).fetch(Organization.class).build()
                    .execute().unique();
            throw new RuntimeException();
        } catch (Exception e) {
            TestCase.assertEquals(e.getClass(),
                    AmbiguousDependencyException.class);
        }

    }

    @Test
    public void ambiguousDependencyExceptionTest2() {
        Team t = addTestData();
        Department d = new Department("成都研发中心", t);
        ds.add(d);
        Department dept = ds.createQuery().addFilter("id", d.getId())
                .fetch(User.class, Team.class, Department.class).execute()
                .unique();
        TestCase.assertNotNull(dept.getTeam().getLeader());
        TestCase.assertNotNull(dept.getTeam().getFollower());
        TestCase.assertEquals(dept.getTeam().getLeader().getName(), t
                .getLeader().getName());
        TestCase.assertEquals(dept.getTeam().getFollower().getName(), t
                .getFollower().getName());
        TestCase.assertEquals(dept.getTeam().getName(), t.getName());
        TestCase.assertEquals(dept.getTeam().getId(), t.getId());

        try {
            ds.createQuery().addFilter("id", d.getId())
                    .fetch(User.class, Team.class)
                    // 因为涉及循环引用，所以上面的fetch不一定一直正确
                    // .fetch(User.class, Team.class, Department.class)
                    .execute().unique();
            throw new RuntimeException();
        } catch (Exception e) {
            TestCase.assertEquals(e.getClass(),
                    AmbiguousDependencyException.class);
        }
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

}
