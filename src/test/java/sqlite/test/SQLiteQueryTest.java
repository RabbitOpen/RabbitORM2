package sqlite.test;

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
import sqlite.test.entity.SQLiteCar;
import sqlite.test.entity.SQLiteLeader;
import sqlite.test.entity.SQLiteOrganization;
import sqlite.test.entity.SQLiteProperty;
import sqlite.test.entity.SQLiteResources;
import sqlite.test.entity.SQLiteRole;
import sqlite.test.entity.SQLiteTeam;
import sqlite.test.entity.SQLiteUUIDPolicyEntity;
import sqlite.test.entity.SQLiteUser;
import sqlite.test.entity.SQLiteZProperty;
import sqlite.test.entity.SQLiteZone;
import sqlite.test.service.SQLiteCarService;
import sqlite.test.service.SQLiteLeaderService;
import sqlite.test.service.SQLiteOrganizationService;
import sqlite.test.service.SQLitePropertyService;
import sqlite.test.service.SQLiteResourcesService;
import sqlite.test.service.SQLiteRoleService;
import sqlite.test.service.SQLiteUserService;
import sqlite.test.service.SQLiteZPropertyService;
import sqlite.test.service.SQLiteZoneService;

/**
 * <b>Description: 查询测试</b><br>
 * <b>@author</b> 肖乾斌
 * 
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:applicationContext-sqlite.xml" })
public class SQLiteQueryTest {

    @Autowired
    SQLiteUserService us;

    @Autowired
    SQLiteRoleService rs;

    @Autowired
    SQLiteOrganizationService os;

    @Autowired
    SQLiteResourcesService resService;

    @Autowired
    SQLiteCarService cs;

    @Autowired
    SQLitePropertyService ps;

    @Autowired
    SQLiteZPropertyService zps;

    @Autowired
    SQLiteZoneService zs;

    @Autowired
    SQLiteLeaderService ls;

    /**
     * 
     * <b>Description: 关联(多对一、多对多)查询 + distinct </b><br>
     * .
     * 
     */
    @Test
    public void simpleQueryTest() {
        SQLiteUser user = addInitData(100);
        List<SQLiteUser> list = us.createQuery(user).joinFetch(SQLiteRole.class)
                .fetch(SQLiteOrganization.class).distinct().execute().list();
        TestCase.assertTrue(list.size() > 0);
        TestCase.assertEquals(user.getBigField(), list.get(0).getBigField());
        TestCase.assertEquals(user.getDoubleField(), list.get(0).getDoubleField());
        TestCase.assertEquals(user.getFloatField(), list.get(0).getFloatField());
    }

    @Test
    public void invalidFetchOperationExceptionTest() {
        SQLiteUser u = new SQLiteUser();
        u.setId(10L);
        try {
            us.createQuery(u).fetch(SQLiteRole.class).execute().list();
        } catch (Exception e) {
            TestCase.assertSame(e.getClass(),
                    InvalidFetchOperationException.class);
        }
    }

    @Test
    public void queryByID() {
        SQLiteUser user = addInitData(100);
        SQLiteUser u = us.getByID(user.getId());
        TestCase.assertEquals(user.getId(), u.getId());
        TestCase.assertEquals(user.getName(), u.getName());
    }

    @Test
    public void addFilterQueryTest() {
        SQLiteUser user = addInitData(110);
        SQLiteUser u = us
                .createQuery()
                .joinFetch(SQLiteRole.class)
                .fetch(SQLiteOrganization.class)
                .addFilter("id", user.getOrg().getId(), SQLiteOrganization.class,
                        SQLiteUser.class)
                .addFilter("${id}", new Long[] { user.getId() }, FilterType.IN)
                .addFilter("birth", user.getBirth(), FilterType.LTE)
                .addFilter("name", new String[] { user.getName() },
                        FilterType.IN)
                .addFilter("orgCode", user.getOrg().getOrgCode(),
                        SQLiteOrganization.class, SQLiteUser.class)
                .addFilter("org", new Long[] { user.getOrg().getId() },
                        FilterType.IN).alias(SQLiteUser.class, "U").execute()
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
                    .joinFetch(SQLiteRole.class)
                    .fetch(SQLiteOrganization.class)
                    .addFilter("id", 1, SQLiteOrganization.class, SQLiteUser.class)
                    .addFilter("id", 1, SQLiteOrganization.class, SQLiteUser.class)
                    .addFilter("id", 1L, SQLiteOrganization.class, SQLiteTeam.class,
                            SQLiteOrganization.class, SQLiteUser.class).execute().unique();
            throw new RuntimeException();
        } catch (Exception e) {
            TestCase.assertEquals(e.getClass(), InvalidQueryPathException.class);
        }

        try {
            us.createQuery()
                    .joinFetch(SQLiteRole.class)
                    .fetch(SQLiteOrganization.class)
                    .addFilter("id", 1L, SQLiteOrganization.class, SQLiteTeam.class,
                            SQLiteOrganization.class, SQLiteUser.class).execute().unique();
            throw new RuntimeException();
        } catch (Exception e) {
            TestCase.assertEquals(e.getClass(), CycleDependencyException.class);
        }
    }

    @Test
    public void queryOrderTest() {
        addInitData(220);
        List<SQLiteUser> list = us.createQuery().page(0, 10)
                .fetch(SQLiteOrganization.class).desc("id").asc("name")
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
        SQLiteUser user = addInitData(120);
        SQLiteUser u = us
                .createQuery()
                .joinFetch(SQLiteRole.class)
                .fetch(SQLiteOrganization.class)
                .addFilter("id", user.getId())
                .addInnerJoinFilter(
                        "id",
                        FilterType.IN,
                        new Integer[] { user.getRoles().get(0).getId(),
                                user.getRoles().get(1).getId() }, SQLiteRole.class)
                .addInnerJoinFilter("roleName",
                        user.getRoles().get(0).getRoleName(), SQLiteRole.class)
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
        SQLiteUser user = addInitData(120);
        Query<SQLiteUser> query = us.createQuery();
        SQLiteUser u = query
                .joinFetch(SQLiteRole.class)
                .fetch(SQLiteOrganization.class)
                .addFilter("id", user.getId())
                .addInnerJoinFilter(
                        JoinFilterBuilder
                                .prepare(query)
                                .join(SQLiteRole.class)
                                .on("id", user.getRoles().get(0).getId())
                                .on("roleName",
                                        user.getRoles().get(0).getRoleName())
                                .join(SQLiteResources.class)
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
        Query<SQLiteUser> query = us.createQuery();
        try {
            query.joinFetch(SQLiteRole.class)
                    .addInnerJoinFilter(
                            JoinFilterBuilder.prepare(query)
                                    .join(SQLiteOrganization.class).on("id", 1)
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
        SQLiteUser user = addInitData(125);
        Query<SQLiteUser> query = us.createQuery();
        SQLiteUser u = query
                .joinFetch(SQLiteRole.class)
                .distinct()
                .addFilter("id", user.getId())
                .alias(SQLiteResources.class, "RESOURCES")
                .fetch(SQLiteOrganization.class)
                .joinFetch(SQLiteCar.class)
                .addInnerJoinFilter(
                        JoinFilterBuilder
                                .prepare(query)
                                .join(SQLiteRole.class)
                                .on("id", user.getRoles().get(0).getId())
                                .on("roleName",
                                        user.getRoles().get(0).getRoleName())
                                .join(SQLiteResources.class)
                                .on("${id}",
                                        user.getRoles().get(0).getResources()
                                                .get(0).getId()).build())
                .addInnerJoinFilter(
                        JoinFilterBuilder.prepare(query).join(SQLiteCar.class)
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
            us.createQuery().joinFetch(SQLiteRole.class).fetch(SQLiteOrganization.class)
                    .asc("id", SQLiteOrganization.class).desc("id")
                    .asc("id", SQLiteRole.class).desc("id", SQLiteUUIDPolicyEntity.class)
                    .execute();
            throw new RuntimeException();
        } catch (Exception e) {
            TestCase.assertSame(e.getClass(), OrderAssociationException.class);
        }
        Query<SQLiteUser> query = us.createQuery();
        query.addInnerJoinFilter(
                JoinFilterBuilder.prepare(query).join(SQLiteRole.class).on("id", 1)
                        .build()).fetch(SQLiteOrganization.class)
                .asc("id", SQLiteOrganization.class).desc("id").asc("id", SQLiteRole.class)
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
        SQLiteUser u = addInitData(130);
        Query<SQLiteUser> query = us.createQuery();
        query.joinFetch(SQLiteRole.class).fetch(SQLiteOrganization.class)
            .joinFetch(SQLiteCar.class)
            .addInnerJoinFilter(
                JoinFilterBuilder.prepare(query).join(SQLiteRole.class)
                        .on("id", u.getRoles().get(0).getId()).on("roleName", u.getRoles().get(0).getRoleName())
                        .join(SQLiteResources.class).on("${id}", 
                                u.getRoles().get(0).getResources().get(0).getId()).build())
            .addInnerJoinFilter(
                JoinFilterBuilder.prepare(query).join(SQLiteCar.class)
                        .on("${id}", u.getCars().get(1).getId()).build());
        long count = query.count();
        TestCase.assertEquals(1, count);
        
        TestCase.assertEquals(1, query.list().size());
        
        SQLiteUser user = query.unique();
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
        SQLiteUser user = addInitData(150);
        SQLiteCar c = new SQLiteCar();
        String carNo = "川A110";
        c.setCarNo(carNo);
        String roleName = "R150";
        SQLiteUser u = us
                .createQuery()
                .addFilter("id", user.getId())
                .joinFetch(SQLiteRole.class, user.getRoles().get(0))
                .joinFetch(SQLiteCar.class, c)
                .addJoinFilter("id", user.getRoles().get(0).getId(), SQLiteRole.class)
                .addJoinFilter("roleName", roleName, SQLiteRole.class)
                .addJoinFilter("carNo", carNo, SQLiteCar.class).execute().unique();
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
            us.createQuery().page(0, 10).joinFetch(SQLiteOrganization.class)
                    .execute().list();
            throw new RuntimeException();
        } catch (Exception e) {
            TestCase.assertSame(e.getClass(),
                    InvalidJoinFetchOperationException.class);
        }
        try {
            us.createQuery().page(0, 10)
                    .addJoinFilter("name", "name", SQLiteOrganization.class)
                    .list();
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
        List<SQLiteUser> list = us.createQuery().addNullFilter("id", false)
                .joinFetch(SQLiteRole.class).joinFetch(SQLiteCar.class)
                .addJoinFilter("id", 1, SQLiteRole.class)
                .addJoinFilter("roleName", "R150", SQLiteRole.class)
                .addJoinFilter("id", 2, SQLiteCar.class).execute().list();
        TestCase.assertTrue(list.size() > 0);

        SQLiteUser u = us.createQuery().addNullFilter("id").joinFetch(SQLiteRole.class)
                .joinFetch(SQLiteCar.class).addJoinFilter("id", 1, SQLiteRole.class)
                .addJoinFilter("roleName", "R150", SQLiteRole.class)
                .addJoinFilter("id", 2, SQLiteCar.class).execute().unique();
        TestCase.assertNull(u);
    }

    public SQLiteUser addInitData2() {
        SQLiteZone z = new SQLiteZone("华北");
        zs.add(z);
        SQLiteUser user = new SQLiteUser();
        // 添加组织
        SQLiteOrganization org = new SQLiteOrganization("FBI", "联邦调查局", z);
        os.add(org);

        // 添加角色
        List<SQLiteRole> roles = new ArrayList<>();
        for (int i = 1000; i < 1002; i++) {
            SQLiteRole r = new SQLiteRole("R" + i);
            rs.add(r);
            roles.add(r);
            // 构建资源
            List<SQLiteResources> resources = new ArrayList<>();
            for (int j = 0; j < 2; j++) {
                SQLiteResources rr = new SQLiteResources("baidu_" + j + i + ".com");
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
        cs.add(new SQLiteCar("川A110", user));
        cs.add(new SQLiteCar("川A120", user));
        cs.add(new SQLiteCar("川A130", user));
        return user;
    }

    @Test
    public void buildFetchTest() {
        SQLiteUser user = addInitData2();
        user.getOrg();
        ps.add(new SQLiteProperty(user.getOrg().getId(), "P1"));
        ps.add(new SQLiteProperty(user.getOrg().getId(), "P2"));

        zps.add(new SQLiteZProperty(user.getOrg().getZone().getId(), "zP4"));
        zps.add(new SQLiteZProperty(user.getOrg().getZone().getId(), "zP3"));

        try {
            // 验证非法的joinFetch操作
            us.createQuery().buildFetch().joinFetch(SQLiteProperty.class).build()
                    .execute().list();
            throw new RuntimeException();
        } catch (Exception e) {
            TestCase.assertSame(e.getClass(),
                    InvalidJoinFetchOperationException.class);
        }

        // 主表joinFetch
        SQLiteUser u = us.createQuery().addFilter("id", user.getId()).buildFetch()
                .joinFetch(SQLiteRole.class).on("id", user.getRoles().get(0).getId())
                .build().execute().unique();
        TestCase.assertNotNull(u.getRoles());
        TestCase.assertEquals(u.getRoles().size(), 1);

        // 从表joinFetch
        u = us.createQuery()
                .addFilter("id", user.getId())
                .buildFetch()
                .joinFetch(SQLiteRole.class)
                .fetch(SQLiteOrganization.class)
                .joinFetch(SQLiteProperty.class)
                .build()
                .addJoinFilter("id", user.getRoles().get(0).getId(), SQLiteRole.class)
                .execute().unique();
        TestCase.assertEquals(u.getRoles().size(), 1);
        TestCase.assertEquals(u.getOrg().getOrgCode(), user.getOrg()
                .getOrgCode());
        TestCase.assertEquals(u.getOrg().getName(), user.getOrg().getName());
        TestCase.assertEquals(u.getOrg().getProps().size(), 2);

        // 从表joinFetch
        u = us.createQuery().addFilter("id", user.getId()).buildFetch()
                .joinFetch(SQLiteRole.class).fetch(SQLiteOrganization.class)
                .joinFetch(SQLiteProperty.class).fetch(SQLiteZone.class)
                .joinFetch(SQLiteZProperty.class).build().execute().unique();
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
        SQLiteZone z = new SQLiteZone(zoneName);
        zs.add(z);
        // 添加组织
        SQLiteOrganization org = new SQLiteOrganization("FBI", "联邦调查局", z);
        os.add(org);
        SQLiteUser user = new SQLiteUser();
        user.setAge(10);
        user.setName("wangwu");
        user.setOrg(org);
        us.add(user);

        SQLiteUser u = us.createQuery().addFilter("id", user.getId())
                .fetch(SQLiteZone.class, SQLiteOrganization.class)
                .fetch(SQLiteOrganization.class).execute().unique();

        TestCase.assertEquals(u.getName(), user.getName());
        TestCase.assertEquals(u.getOrg().getOrgCode(), "FBI");
        TestCase.assertEquals(u.getOrg().getZone().getName(), zoneName);

    }

    @Test
    public void buildFetchTest2() {
        SQLiteZone z = new SQLiteZone("华强北");
        zs.add(z);
        SQLiteLeader l = new SQLiteLeader("LEADER");
        ls.add(l);
        SQLiteUser user = new SQLiteUser();
        // 添加组织
        SQLiteOrganization org = new SQLiteOrganization("FBI", "联邦调查局", z, l);
        os.add(org);
        user.setOrg(org);
        us.add(user);
        SQLiteUser u = us.createQuery().addFilter("id", user.getId()).buildFetch()
                .fetch(SQLiteOrganization.class).fetch(SQLiteZone.class).build()
                .fetch(SQLiteLeader.class, SQLiteOrganization.class).execute().unique();

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
            us.createQuery().buildFetch().fetch(SQLiteOrganization.class)
                    .fetch(SQLiteZone.class).build().fetch(SQLiteZone.class).execute()
                    .list();
            throw new RuntimeException();
        } catch (Exception e) {
            TestCase.assertSame(e.getClass(),
                    RepeatedFetchOperationException.class);
        }
        try {
            us.createQuery().fetch(SQLiteZone.class).buildFetch()
                    .fetch(SQLiteOrganization.class).fetch(SQLiteZone.class).build()
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
    public SQLiteUser addInitData(int start) {
        SQLiteUser user = new SQLiteUser();
        // 添加组织
        SQLiteOrganization org = new SQLiteOrganization("FBI", "联邦调查局");
        os.add(org);

        // 添加角色
        List<SQLiteRole> roles = new ArrayList<>();
        for (int i = start; i < start + 2; i++) {
            SQLiteRole r = new SQLiteRole("R" + i);
            rs.add(r);
            roles.add(r);
            // 构建资源
            List<SQLiteResources> resources = new ArrayList<>();
            for (int j = 0; j < 2; j++) {
                SQLiteResources rr = new SQLiteResources("baidu_" + j + i + ".com");
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
        List<SQLiteCar> cars = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            // 添加车辆
            SQLiteCar car = new SQLiteCar("川A11" + i, user);
            cs.add(car);
            cars.add(car);
        }
        user.setCars(cars);
        return user;

    }

}
