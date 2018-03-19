package rabbit.open.test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import junit.framework.TestCase;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import rabbit.open.orm.annotation.FilterType;
import rabbit.open.orm.dml.Query;
import rabbit.open.orm.dml.meta.JoinFilterBuilder;
import rabbit.open.orm.exception.CycleDependencyException;
import rabbit.open.orm.exception.InvalidFetchOperationException;
import rabbit.open.orm.exception.InvalidJoinFetchOperationException;
import rabbit.open.orm.exception.InvalidJoinFilterException;
import rabbit.open.orm.exception.InvalidQueryPathException;
import rabbit.open.orm.exception.OrderAssociationException;
import rabbit.open.orm.exception.RepeatedFetchOperationException;
import rabbit.open.test.entity.Car;
import rabbit.open.test.entity.Leader;
import rabbit.open.test.entity.Organization;
import rabbit.open.test.entity.Property;
import rabbit.open.test.entity.Resources;
import rabbit.open.test.entity.Role;
import rabbit.open.test.entity.Team;
import rabbit.open.test.entity.UUIDPolicyEntity;
import rabbit.open.test.entity.User;
import rabbit.open.test.entity.ZProperty;
import rabbit.open.test.entity.Zone;
import rabbit.open.test.service.CarService;
import rabbit.open.test.service.LeaderService;
import rabbit.open.test.service.OrganizationService;
import rabbit.open.test.service.PropertyService;
import rabbit.open.test.service.ResourcesService;
import rabbit.open.test.service.RoleService;
import rabbit.open.test.service.UserService;
import rabbit.open.test.service.ZPropertyService;
import rabbit.open.test.service.ZoneService;

/**
 * <b>Description: 查询测试</b><br>
 * <b>@author</b> 肖乾斌
 * 
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:applicationContext.xml" })
public class QueryTest {

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
    PropertyService ps;

    @Autowired
    ZPropertyService zps;

    @Autowired
    ZoneService zs;

    @Autowired
    LeaderService ls;

    /**
     * 
     * <b>Description: 关联(多对一、多对多)查询 + distinct </b><br>
     * .
     * 
     */
    @Test
    public void simpleQueryTest() {
        User user = addInitData(100);
        List<User> list = us.createQuery(user).joinFetch(Role.class)
                .fetch(Organization.class).distinct().execute().list();
        TestCase.assertTrue(list.size() > 0);
        TestCase.assertEquals(user.getBigField(), list.get(0).getBigField());
        TestCase.assertEquals(user.getDoubleField(), list.get(0).getDoubleField());
        TestCase.assertEquals(user.getFloatField(), list.get(0).getFloatField());
    }

    @Test
    public void invalidFetchOperationExceptionTest() {
        User u = new User();
        u.setId(10L);
        try {
            us.createQuery(u).fetch(Role.class).execute().list();
        } catch (Exception e) {
            TestCase.assertSame(e.getClass(),
                    InvalidFetchOperationException.class);
        }
    }

    @Test
    public void queryByID() {
        User user = addInitData(100);
        User u = us.getByID(user.getId());
        TestCase.assertEquals(user.getId(), u.getId());
        TestCase.assertEquals(user.getName(), u.getName());
    }

    @Test
    public void addFilterQueryTest() {
        User user = addInitData(110);
        User u = us
                .createQuery()
                .joinFetch(Role.class)
                .fetch(Organization.class)
                .addFilter("id", user.getOrg().getId(), Organization.class,
                        User.class)
                .addFilter("${id}", new Long[] { user.getId() }, FilterType.IN)
                .addFilter("birth", user.getBirth(), FilterType.LTE)
                .addFilter("name", new String[] { user.getName() },
                        FilterType.IN)
                .addFilter("orgCode", user.getOrg().getOrgCode(),
                        Organization.class, User.class)
                .addFilter("org", new Long[] { user.getOrg().getId() },
                        FilterType.IN).alias(User.class, "U").execute()
                .unique();
        TestCase.assertEquals(user.getName(), u.getName());
        TestCase.assertEquals(user.getOrg().getName(), u.getOrg().getName());
        TestCase.assertEquals(user.getOrg().getOrgCode(), u.getOrg()
                .getOrgCode());
    }

    @Test
    public void invalidQueryPathTest() {
        try {
            us.createQuery()
                    .joinFetch(Role.class)
                    .fetch(Organization.class)
                    .addFilter("id", 1, Organization.class, User.class)
                    .addFilter("id", 1, Organization.class, User.class)
                    .addFilter("id", 1L, Organization.class, Team.class,
                            Organization.class, User.class).execute().unique();
            throw new RuntimeException();
        } catch (Exception e) {
            TestCase.assertEquals(e.getClass(), InvalidQueryPathException.class);
        }

        try {
            us.createQuery()
                    .joinFetch(Role.class)
                    .fetch(Organization.class)
                    .addFilter("id", 1L, Organization.class, Team.class,
                            Organization.class, User.class).execute().unique();
            throw new RuntimeException();
        } catch (Exception e) {
            TestCase.assertEquals(e.getClass(), CycleDependencyException.class);
        }
    }

    @Test
    public void queryOrderTest() {
        addInitData(220);
        List<User> list = us.createQuery().page(0, 10)
                .fetch(Organization.class).desc("id").asc("name").execute()
                .list();
        TestCase.assertTrue(list.size() >= 1);
        TestCase.assertTrue(list.size() <= 10);
    }

    /**
     * 
     * <b>Description: 新增内链接条件测试</b><br>
     * .
     * 
     */
    @Test
    public void innerJoinQueryTest() {
        User user = addInitData(120);
        User u = us
                .createQuery()
                .joinFetch(Role.class)
                .fetch(Organization.class)
                .addFilter("id", user.getId())
                .addInnerJoinFilter(
                        "id",
                        FilterType.IN,
                        new Integer[] { user.getRoles().get(0).getId(),
                                user.getRoles().get(1).getId() }, Role.class)
                .addInnerJoinFilter("roleName",
                        user.getRoles().get(0).getRoleName(), Role.class)
                .execute().unique();
        TestCase.assertEquals(u.getOrg().getOrgCode(), user.getOrg()
                .getOrgCode());
        TestCase.assertEquals(u.getRoles().size(), 1);
        TestCase.assertEquals(u.getRoles().get(0).getRoleName(), user
                .getRoles().get(0).getRoleName());
        System.out.println(u);
    }

    /**
     * 
     * <b>Description: 新增自定义内链接条件测试</b><br>
     * . ManyToMany
     * 
     */
    @Test
    public void joinFilterBuilderTest() {
        User user = addInitData(120);
        Query<User> query = us.createQuery();
        User u = query
                .joinFetch(Role.class)
                .fetch(Organization.class)
                .addFilter("id", user.getId())
                .addInnerJoinFilter(
                        JoinFilterBuilder
                                .prepare(query)
                                .join(Role.class)
                                .on("id", user.getRoles().get(0).getId())
                                .on("roleName",
                                        user.getRoles().get(0).getRoleName())
                                .join(Resources.class)
                                .on("${id}",
                                        user.getRoles().get(0).getResources()
                                                .get(0).getId()).build())
                .execute().unique();
        TestCase.assertEquals(u.getOrg().getOrgCode(), user.getOrg()
                .getOrgCode());
        TestCase.assertEquals(u.getRoles().size(), 1);
        TestCase.assertEquals(u.getRoles().get(0).getRoleName(), user
                .getRoles().get(0).getRoleName());
        System.out.println(u);
    }

    /**
     * <b>Description 测试添加非法join filter</b>
     */
    @Test
    public void invalidJoinFilterExceptionTest() {
        Query<User> query = us.createQuery();
        try {
            query.joinFetch(Role.class)
                    .addInnerJoinFilter(
                            JoinFilterBuilder.prepare(query)
                                    .join(Organization.class).on("id", 1)
                                    .build()).execute().unique();
        } catch (Exception e) {
            TestCase.assertEquals(e.getClass(),
                    InvalidJoinFilterException.class);
        }
    }

    /**
     * 
     * <b>Description: 新增自定义内链接条件测试</b><br>
     * . OneToMany
     * 
     */
    @Test
    public void joinFilterBuilderTest2() {
        User user = addInitData(125);
        Query<User> query = us.createQuery();
        User u = query
                .joinFetch(Role.class)
                .distinct()
                .addFilter("id", user.getId())
                .alias(Resources.class, "RESOURCES")
                .fetch(Organization.class)
                .joinFetch(Car.class)
                .addInnerJoinFilter(
                        JoinFilterBuilder
                                .prepare(query)
                                .join(Role.class)
                                .on("id", user.getRoles().get(0).getId())
                                .on("roleName",
                                        user.getRoles().get(0).getRoleName())
                                .join(Resources.class)
                                .on("${id}",
                                        user.getRoles().get(0).getResources()
                                                .get(0).getId()).build())
                .addInnerJoinFilter(
                        JoinFilterBuilder.prepare(query).join(Car.class)
                                .on("${id}", user.getCars().get(0).getId())
                                .build()).execute().unique();
        TestCase.assertEquals(u.getOrg().getOrgCode(), user.getOrg()
                .getOrgCode());
        TestCase.assertEquals(u.getRoles().size(), 1);
        TestCase.assertEquals(u.getRoles().get(0).getRoleName(), user
                .getRoles().get(0).getRoleName());
        TestCase.assertEquals(u.getCars().size(), 1);
        TestCase.assertEquals(u.getCars().get(0).getCarNo(), user.getCars()
                .get(0).getCarNo());
        System.out.println(u);
    }

    /**
     * 
     * <b>Description: 非法排序测试 </b><br>
     * .
     * 
     */
    @Test
    public void wrongOrderTest() {
        try {
            us.createQuery().joinFetch(Role.class).fetch(Organization.class)
                    .asc("id", Organization.class).desc("id")
                    .asc("id", Role.class).desc("id", UUIDPolicyEntity.class)
                    .execute();
            throw new RuntimeException();
        } catch (Exception e) {
            TestCase.assertSame(e.getClass(), OrderAssociationException.class);
        }
        Query<User> query = us.createQuery();
        query.addInnerJoinFilter(
                JoinFilterBuilder.prepare(query).join(Role.class).on("id", 1)
                        .build()).fetch(Organization.class)
                .asc("id", Organization.class).desc("id").asc("id", Role.class)
                .execute();
    }

    /**
     * 
     * <b>Description: 条数统计测试</b><br>
     * .
     * 
     */
    @Test
    public void countTest() {
        User u = addInitData(130);
        Query<User> query = us.createQuery();
        query.joinFetch(Role.class).fetch(Organization.class)
            .joinFetch(Car.class)
            .addInnerJoinFilter(
                JoinFilterBuilder.prepare(query).join(Role.class)
                        .on("id", u.getRoles().get(0).getId()).on("roleName", u.getRoles().get(0).getRoleName())
                        .join(Resources.class).on("${id}", 
                                u.getRoles().get(0).getResources().get(0).getId()).build())
            .addInnerJoinFilter(
                JoinFilterBuilder.prepare(query).join(Car.class)
                        .on("${id}", u.getCars().get(1).getId()).build());
        long count = query.count();
        TestCase.assertEquals(1, count);

        TestCase.assertEquals(1, query.list().size());
        
        User user = query.unique();
        TestCase.assertEquals(user.getRoles().size(), 1);
        TestCase.assertEquals(user.getCars().size(), 1);
        TestCase.assertEquals(user.getCars().get(0).getCarNo(), u.getCars().get(1).getCarNo());
        TestCase.assertEquals(user.getRoles().get(0).getRoleName(), u.getRoles().get(0).getRoleName());
    }

    /**
     * 
     * <b>Description: addJoinFilterTest</b><br>
     * .
     * 
     */
    @Test
    public void addJoinFilterTest() {
        User user = addInitData(150);
        Car c = new Car();
        String carNo = "川A110";
        c.setCarNo(carNo);
        String roleName = "R150";
        User u = us
                .createQuery()
                .addFilter("id", user.getId())
                .joinFetch(Role.class, user.getRoles().get(0))
                .joinFetch(Car.class, c)
                .addJoinFilter("id", user.getRoles().get(0).getId(), Role.class)
                .addJoinFilter("roleName", roleName, Role.class)
                .addJoinFilter("carNo", carNo, Car.class).execute().unique();
        TestCase.assertEquals(u.getCars().size(), 1);
        TestCase.assertEquals(u.getCars().get(0).getCarNo(), carNo);
        TestCase.assertEquals(u.getRoles().size(), 1);
        TestCase.assertEquals(u.getRoles().get(0).getRoleName(), roleName);
    }

    /**
     * 
     * <b>Description: addJoinFilterTest</b><br>
     * .
     * 
     */
    @Test
    public void invalidJoinFilterTest() {
        addInitData(150);
        try {
            us.createQuery().page(0, 10).joinFetch(Organization.class)
                    .execute().list();
            throw new RuntimeException();
        } catch (Exception e) {
            TestCase.assertSame(e.getClass(),
                    InvalidJoinFetchOperationException.class);
        }
        try {
            us.createQuery().page(0, 10)
                    .addJoinFilter("name", "name", Organization.class)
                    .execute().list();
            throw new RuntimeException();
        } catch (Exception e) {
            TestCase.assertSame(e.getClass(),
                    InvalidJoinFetchOperationException.class);
        }
    }

    /**
     * 
     * <b>Description: addNullFilterTest</b><br>
     * .
     * 
     */
    @Test
    public void addNullFilterTest() {
        addInitData(150);
        List<User> list = us.createQuery().addNullFilter("id", false)
                .joinFetch(Role.class).joinFetch(Car.class)
                .addJoinFilter("id", 1, Role.class)
                .addJoinFilter("roleName", "R150", Role.class)
                .addJoinFilter("id", 2, Car.class).execute().list();
        TestCase.assertTrue(list.size() > 0);

        User u = us.createQuery().addNullFilter("id").joinFetch(Role.class)
                .joinFetch(Car.class).addJoinFilter("id", 1, Role.class)
                .addJoinFilter("roleName", "R150", Role.class)
                .addJoinFilter("id", 2, Car.class).execute().unique();
        TestCase.assertNull(u);
    }

    public User addInitData2() {
        Zone z = new Zone("华北");
        zs.add(z);
        User user = new User();
        // 添加组织
        Organization org = new Organization("FBI", "联邦调查局", z);
        os.add(org);

        // 添加角色
        List<Role> roles = new ArrayList<Role>();
        for (int i = 1000; i < 1002; i++) {
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

        user.setName("zhangsan" + System.currentTimeMillis());
        user.setBirth(new Date());
        us.add(user);

        // 添加用户角色之间的映射关系
        user.setRoles(roles);
        us.addJoinRecords(user);

        // 添加车辆
        cs.add(new Car("川A110", user));
        cs.add(new Car("川A120", user));
        cs.add(new Car("川A130", user));
        return user;
    }

    @Test
    public void buildFetchTest() {
        User user = addInitData2();
        user.getOrg();
        ps.add(new Property(user.getOrg().getId(), "P1"));
        ps.add(new Property(user.getOrg().getId(), "P2"));

        zps.add(new ZProperty(user.getOrg().getZone().getId(), "zP4"));
        zps.add(new ZProperty(user.getOrg().getZone().getId(), "zP3"));

        try {
            // 验证非法的joinFetch操作
            us.createQuery().buildFetch().joinFetch(Property.class).build()
                    .execute().list();
            throw new RuntimeException();
        } catch (Exception e) {
            TestCase.assertSame(e.getClass(),
                    InvalidJoinFetchOperationException.class);
        }

        // 主表joinFetch
        User u = us.createQuery().addFilter("id", user.getId()).buildFetch()
                .joinFetch(Role.class).on("id", user.getRoles().get(0).getId())
                .build().execute().unique();
        TestCase.assertNotNull(u.getRoles());
        TestCase.assertEquals(u.getRoles().size(), 1);

        // 从表joinFetch
        u = us.createQuery()
                .addFilter("id", user.getId())
                .buildFetch()
                .joinFetch(Role.class)
                .fetch(Organization.class)
                .joinFetch(Property.class)
                .build()
                .addJoinFilter("id", user.getRoles().get(0).getId(), Role.class)
                .execute().unique();
        TestCase.assertEquals(u.getRoles().size(), 1);
        TestCase.assertEquals(u.getOrg().getOrgCode(), user.getOrg()
                .getOrgCode());
        TestCase.assertEquals(u.getOrg().getName(), user.getOrg().getName());
        TestCase.assertEquals(u.getOrg().getProps().size(), 2);

        // 从表joinFetch
        u = us.createQuery().addFilter("id", user.getId()).buildFetch()
                .joinFetch(Role.class).fetch(Organization.class)
                .joinFetch(Property.class).fetch(Zone.class)
                .joinFetch(ZProperty.class).build().execute().unique();
        TestCase.assertEquals(u.getRoles().size(), 2);
        TestCase.assertEquals(u.getOrg().getOrgCode(), user.getOrg()
                .getOrgCode());
        TestCase.assertEquals(u.getOrg().getName(), user.getOrg().getName());
        TestCase.assertEquals(u.getOrg().getProps().size(), 2);
        TestCase.assertEquals(u.getOrg().getZone().getProps().size(), 2);

    }

    @Test
    public void deepFetchTest() {

        String zoneName = "华中地区";
        Zone z = new Zone(zoneName);
        zs.add(z);
        // 添加组织
        Organization org = new Organization("FBI", "联邦调查局", z);
        os.add(org);
        User user = new User();
        user.setAge(10);
        user.setName("wangwu");
        user.setOrg(org);
        us.add(user);

        User u = us.createQuery().addFilter("id", user.getId())
                .fetch(Zone.class, Organization.class)
                .fetch(Organization.class).execute().unique();

        TestCase.assertEquals(u.getName(), user.getName());
        TestCase.assertEquals(u.getOrg().getOrgCode(), "FBI");
        TestCase.assertEquals(u.getOrg().getZone().getName(), zoneName);

    }

    @Test
    public void buildFetchTest2() {
        Zone z = new Zone("华强北");
        zs.add(z);
        Leader l = new Leader("LEADER");
        ls.add(l);
        User user = new User();
        // 添加组织
        Organization org = new Organization("FBI", "联邦调查局", z, l);
        os.add(org);
        user.setOrg(org);
        us.add(user);
        User u = us.createQuery().addFilter("id", user.getId()).buildFetch()
                .fetch(Organization.class).fetch(Zone.class).build()
                .fetch(Leader.class, Organization.class).execute().unique();

        TestCase.assertEquals(u.getOrg().getOrgCode(), "FBI");
        TestCase.assertEquals(u.getOrg().getZone().getName(), "华强北");
        TestCase.assertEquals(u.getOrg().getLeader().getName(), "LEADER");

    }

    /**
     * <b>Description fetch异常测试.</b>
     */
    @Test
    public void fetchExceptionTest() {
        try {
            us.createQuery().buildFetch().fetch(Organization.class)
                    .fetch(Zone.class).build().fetch(Zone.class).execute()
                    .list();
            throw new RuntimeException();
        } catch (Exception e) {
            TestCase.assertSame(e.getClass(),
                    RepeatedFetchOperationException.class);
        }
        try {
            us.createQuery().fetch(Zone.class).buildFetch()
                    .fetch(Organization.class).fetch(Zone.class).build()
                    .execute().list();
            throw new RuntimeException();
        } catch (Exception e) {
            TestCase.assertSame(e.getClass(),
                    RepeatedFetchOperationException.class);
        }
    }

    /**
     * 
     * <b>Description: 添加测试数据</b><br>
     * @param start
     * 
     */
    public User addInitData(int start) {
        User user = new User();
        // 添加组织
        Organization org = new Organization("FBI", "联邦调查局");
        os.add(org);

        // 添加角色
        List<Role> roles = new ArrayList<Role>();
        for (int i = start; i < start + 2; i++) {
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
        user.setDoubleField(0.2);
        user.setFloatField(0.1f);

        user.setName("zhangsan" + System.currentTimeMillis());
        user.setBirth(new Date());
        us.add(user);

        // 添加用户角色之间的映射关系
        user.setRoles(roles);
        us.addJoinRecords(user);
        List<Car> cars = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            // 添加车辆
            Car car = new Car("川A11" + i, user);
            cs.add(car);
            cars.add(car);
        }
        user.setCars(cars);
        return user;

    }

}
