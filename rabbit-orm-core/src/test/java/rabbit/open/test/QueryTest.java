package rabbit.open.test;

import junit.framework.TestCase;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import rabbit.open.orm.common.dml.FilterType;
import rabbit.open.orm.common.exception.*;
import rabbit.open.orm.core.annotation.Column;
import rabbit.open.orm.core.dml.Query;
import rabbit.open.orm.core.dml.filter.ext.ManyToManyFilter;
import rabbit.open.orm.core.dml.filter.ext.ManyToOneFilter;
import rabbit.open.orm.core.dml.filter.ext.OneToManyFilter;
import rabbit.open.orm.core.dml.meta.FieldMetaData;
import rabbit.open.orm.core.dml.meta.MetaData;
import rabbit.open.test.entity.*;
import rabbit.open.test.entity.EnumComponent.ComponentCodeEnum;
import rabbit.open.test.entity.EnumRole.EnumRoleEnum;
import rabbit.open.test.entity.dmlfilter.*;
import rabbit.open.test.service.*;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

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

    @Autowired
    MyUserSerivce mus;
    
    @Autowired
    DMLUserService dmlUs;
    
    @Autowired
    DMLRoleService dmlRs;
    
    @Autowired
    DMLTeamService dmlTs;
    
    @Autowired
    DMLResourceService dmlResourceService;
    
    @Autowired
	DMLURIService dmlUris;

    @Autowired
    AutoSpeculateService ass;
    
    @Test
    public void multiExtend() {
    	mus.batchAdd();
    }

    /**
     * 
     * <b>Description: 关联(多对一、多对多)查询 + distinct </b><br>
     * 
     */
    @Test
    public void simpleQueryTest() {
        User user = addInitData(1202);
        List<User> list = us.createQuery(user).joinFetch(Role.class)
                .fetch(Organization.class)
                .addFilter("id", user.getOrg().getId(), Organization.class, User.class)
                .execute().list();
        TestCase.assertTrue(list.size() > 0);
        TestCase.assertEquals(user.getBigField(), list.get(0).getBigField());
        TestCase.assertEquals(user.getDoubleField(), list.get(0).getDoubleField());
        MetaData.getMetaByClass(User.class);
        TestCase.assertEquals(user.getFloatField(), list.get(0).getFloatField());
    }
    
    /**
     * 
     * <b>Description: 关联(多对一、多对多)查询 + distinct </b><br>
     * 
     */
    @Test
    public void innerJoinFetchTest() {
        User user = addInitData(1207);
        List<User> list = us.createQuery()
        		.addFilter("id", user.getId())
        		.innerJoinFetch(Role.class)
                .execute().list();
        TestCase.assertEquals(1, list.size());
        TestCase.assertEquals(list.get(0).getRoles().size(), 2);
        
        User result = us.createQuery()
				.addFilter("id", user.getId())
				.innerJoinFetch(Role.class, user.getRoles().get(0)).unique();
        TestCase.assertEquals(result.getRoles().size(), 1);
        
        // 验证innerJoinFetch后导致整个数据都查不到
        Role r = new Role();
        r.setId(user.getRoles().get(0).getId() + 10);
        result = us.createQuery()
				.addFilter("id", user.getId())
				.innerJoinFetch(Role.class, r).unique();
        TestCase.assertNull(result);
        
    }
    
    
    @Test
    public void innerFetchTest() {
        User user = addInitData(1232);
        List<User> list = us.createQuery().addFilter("id", user.getId()).innerFetch(Organization.class).list();
        TestCase.assertTrue(list.size() > 0);
        
    }

    @Test
    public void bytesTest() {
        User user = new User();
        user.setBytes("hello".getBytes());
        us.add(user);
        User usByID = us.getByID(user.getId());
        TestCase.assertEquals(new String(usByID.getBytes()), "hello");
    }

    /**
     *	自动推测测试
     * @param
     * @author  xiaoqianbin
     * @date    2020/4/20
     **/
    @Test
    public void autoSpeculateTest() throws ParseException {
        @SuppressWarnings("unchecked")
		MetaData<AutoSpeculateEntity> meta = MetaData.getMetaByClass(AutoSpeculateEntity.class);
        Collection<FieldMetaData> fieldMetas = meta.getFieldMetas();
        Column userName = null;
        for (FieldMetaData fieldMeta : fieldMetas) {
            if (fieldMeta.getField().getName().equals("userName")) {
                userName = fieldMeta.getColumn();
                break;
            }
        }
        TestCase.assertEquals(userName.value(), "USER_NAME");

        AutoSpeculateEntity ase = new AutoSpeculateEntity();
        ase.setAge(10L);
        ase.setGender(AutoSpeculateEntity.Gender.MALE);
        ase.setOrgName("my-org");
        ase.setUserName("lll");
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
        ase.setCreatedTime(formatter.parse("2020-08-08"));

        ass.add(ase);
        AutoSpeculateEntity result = ass.getByID(ase.getId());
        TestCase.assertEquals(result.getGender(), ase.getGender());
        TestCase.assertEquals(result.getAge(), ase.getAge());
        TestCase.assertEquals(result.getOrgName(), ase.getOrgName());
        TestCase.assertEquals(result.getUserName(), ase.getUserName());
        TestCase.assertEquals(formatter.format(result.getCreatedTime()), formatter.format(ase.getCreatedTime()));
    }

    @Test
    public void invalidFetchOperationExceptionTest() {
        User u = new User();
        u.setId(10L);
        try {
            us.createQuery(u).fetch(Role.class).execute().list();
            throw new RuntimeException();
        } catch (Exception e) {
            TestCase.assertSame(e.getClass(),
                    InvalidFetchOperationException.class);
        }
    }

    @Test
    public void queryByID() {
        User user = addInitData(1090);
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
                .addFilter(new ManyToManyFilter(Role.class)
	    					.on("id", user.getRoles().get(0).getId())
	    					.on("roleName",user.getRoles().get(0).getRoleName())
	    					.add(new ManyToManyFilter(Resources.class)
	    					.on("${id}", user.getRoles().get(0).getResources() .get(0).getId()))	
                		)
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
                    .addFilter(new ManyToManyFilter(Organization.class).on("id", 1))
                    .execute().unique();
            throw new RuntimeException();
        } catch (Exception e) {
            TestCase.assertEquals(e.getClass(), InvalidJoinFilterException.class);
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
                .addFilter("id", user.getId().longValue())
                .alias(Resources.class, "RESOURCES")
                .fetch(Organization.class)
                .joinFetch(Car.class)
                .addFilter(new ManyToManyFilter(Role.class)
                		.on("id", user.getRoles().get(0).getId())
                        .on("roleName",  user.getRoles().get(0).getRoleName())
                        .add(new ManyToManyFilter(Resources.class)
                        		.on("${id}", user.getRoles().get(0).getResources().get(0).getId())))
                .addFilter(new OneToManyFilter(Car.class).on("${id}", user.getCars().get(0).getId()))
                .execute().unique();
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
        query.addFilter(new ManyToManyFilter(Role.class).on("id", 1))
        		.fetch(Organization.class)
                .asc("id", Organization.class).desc("id").asc("id", Role.class)
                .execute();
    }

    /**
     * 
     * <b>Description: 条数统计测试</b><br>
     * 
     */
    @Test
    public void countTest() {
        User u = addInitData(130);
        Query<User> query = us.createQuery();
        query.joinFetch(Role.class).fetch(Organization.class)
            .joinFetch(Car.class)
            .addFilter(new ManyToManyFilter(Role.class)
            			.on("id", u.getRoles().get(0).getId())
            			.on("roleName", u.getRoles().get(0).getRoleName())
                        .add(new ManyToManyFilter(Resources.class)
                        		.on("${id}", u.getRoles().get(0).getResources().get(0).getId())))
            .addFilter(new OneToManyFilter(Car.class).on("${id}", u.getCars().get(1).getId()));
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

    /**
     * 
     * <b>Description: addNotNullFilterTest</b><br>
     * 
     */
    @Test
    public void addNotNullFilterTest() {
    	addInitData(152);
    	List<User> list = us.createQuery().addNotNullFilter("id")
    			.joinFetch(Role.class).joinFetch(Car.class)
    			.addJoinFilter("id", 1, Role.class)
    			.addJoinFilter("roleName", "R150", Role.class)
    			.addJoinFilter("id", 2, Car.class).execute().list();
    	TestCase.assertTrue(list.size() > 0);
    	
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
        user.setBytes("hello".getBytes());

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

    @Test
    public void dateListTest() throws ParseException {
        User u1 = new User();
        Date d1 = new SimpleDateFormat("yyyy-MM-dd").parse("2018-08-10");
        u1.setBirth(d1);
        us.add(u1);
        User u2 = new User();
        Date d2 = new SimpleDateFormat("yyyy-MM-dd").parse("2018-08-11");
        u2.setBirth(d2);
        us.add(u2);
        
        List<User> list = us.createQuery().addFilter("birth", 
                new Date[]{d1, d2}, FilterType.IN).desc("birth").list();
        
        TestCase.assertEquals(d2, list.get(0).getBirth());
        TestCase.assertEquals(d1, list.get(1).getBirth());
        
        TestCase.assertEquals(2, list.size());
    }
    
    @Test
    public void booleanTest() {
    	User u = new User();
    	u.setMale(false);
    	us.add(u);
    	User user = us.getByID(u.getId());
    	TestCase.assertFalse(user.getMale());
    	
    	u.setMale(true);
    	us.updateByID(u);
    	user = us.createQuery().addFilter("male", true).addFilter("id", u.getId()).unique();
    	TestCase.assertTrue(user.getMale());
    	
    }
    
    /**
     * <b>Description 如果要取关联表一定要concern </b>
     * @author 肖乾斌
     */
    @Test
    public void concernFieldTest() {
    	Organization org = new Organization("mmmOr", "mmx");
    	Leader l = new Leader("myl");
    	l.setAge(10);
    	ls.add(l);
    	org.setLeader(l);
    	os.add(org);
		Query<Organization> query = os.createQuery();
		Organization o = query.queryFields("orgCode", "leader").queryFields(Leader.class, "name")
				.fetch(Leader.class)
				.addFilter("id", org.getId())
				.unique();
		query.showUnMaskedSql();
		System.out.println(o);
		TestCase.assertNull(o.getLeader().getAge());
		TestCase.assertEquals(o.getLeader().getName(), org.getLeader().getName());
		TestCase.assertNull(o.getName());
		TestCase.assertEquals(o.getOrgCode(), org.getOrgCode());
		
		Query<Organization> q2 = os.createQuery();
		Organization o1 = q2.addFilter("id", org.getId())
						.fetch(Leader.class).unique();
		q2.showMaskedPreparedSql();
		TestCase.assertNotNull(o1.getLeader().getAge());
		TestCase.assertEquals(o1.getLeader().getName(), org.getLeader().getName());
		TestCase.assertNotNull(o1.getName());
		TestCase.assertEquals(o1.getOrgCode(), org.getOrgCode());
		System.out.println(o1);
		
		
    }
    
    @Test
    public void concernFieldTest2() {
    	Organization org = new Organization("mmmOr", "mmx");
    	Leader l = new Leader("myl");
    	l.setAge(10);
    	ls.add(l);
    	org.setLeader(l);
    	os.add(org);
    	Query<Organization> query = os.createQuery();
    	Organization o = query.tagConcern()
    			.fetch(Leader.class)
    			.addFilter("id", org.getId())
    			.unique();
    	query.showUnMaskedSql();
    	System.out.println(o);
    	TestCase.assertEquals(o.getLeader().getName(), org.getLeader().getName());
    	TestCase.assertNull(o.getName());
    	TestCase.assertEquals(o.getOrgCode(), org.getOrgCode());
    	
    }

    /**
     * 测试只查询部分字段，不会默认带出id字段
     */
    @Test
    public void filterSpecifiedFieldsTest() {
    	Organization org = new Organization("mmmOr1", "mmx");
    	os.add(org);
    	Organization org1 = new Organization("mmmOr1", "mmx");
    	os.add(org1);
    	
    	Query<Organization> query = os.createQuery();
		List<Organization> list = query.querySpecifiedFields("orgCode", "name")
				.addFilter("orgCode", org.getOrgCode()).list();
		query.showUnMaskedSql();
		TestCase.assertEquals(2, list.size());
    }

    @Test
    public void emptyFilterTest() {
    	try {
    		us.createQuery().addFilter("id", null, FilterType.IN).list();
    		throw new RuntimeException();
    	} catch (Exception e) {
    		TestCase.assertEquals(e.getClass(), EmptyListFilterException.class);
    	}
    	try {
    		us.createQuery().addFilter("id", new String[]{}, FilterType.IN).list();
    		throw new RuntimeException();
    	} catch (Exception e) {
    		TestCase.assertEquals(e.getClass(), EmptyListFilterException.class);
    	}
    	try {
    		us.createQuery().addFilter("id", new ArrayList<>(), FilterType.IN).list();
    		throw new RuntimeException();
    	} catch (Exception e) {
    		TestCase.assertEquals(e.getClass(), EmptyListFilterException.class);
    	}
    }


    @Autowired
    private MasterService masterService;

    @Autowired
    private SlaveService slaveService;

    @Autowired
    private MasterSlaveService masterSlaveService;

    @Test
    public void joinFetchWithFilterTest() {

        Master m = new Master();
        m.setName("master");
        masterService.add(m);

        Slave s1 = new Slave();
        s1.setName("s1");
        slaveService.add(s1);Slave s2 = new Slave();
        s2.setName("s2");
        slaveService.add(s2);

        List<Slave> slaves = new ArrayList<>();
        slaves.add(s1);
        slaves.add(s2);
        m.setSlaves(slaves);
        masterService.addJoinRecords(m);

        try {
            masterSlaveService.createUpdate().set("type", 1)
                    .addFilter("masterId", m.getId())
                    .execute();
            throw  new RuntimeException();
        } catch (Exception e) {
            TestCase.assertEquals(WrongJavaTypeException.class, e.getCause().getClass());
        }

        masterSlaveService.createUpdate().set("type", "1")
                .addFilter("masterId", m.getId())
                .execute();
        Master queriedMaster = masterService.createQuery().addFilter("id", m.getId())
                .joinFetchByFilter("1", Slave.class)
                .unique();

        TestCase.assertEquals(slaves.size(), queriedMaster.getSlaves().size());

        // inner join 验证
        queriedMaster = masterService.createQuery().addFilter("id", m.getId())
                .innerJoinFetchByFilter("1", Slave.class)
                .unique();
        TestCase.assertEquals(slaves.size(), queriedMaster.getSlaves().size());

        queriedMaster = masterService.createQuery().addFilter("id", m.getId())
                .innerJoinFetchByFilter("12", Slave.class)
                .unique();
        TestCase.assertNull(queriedMaster);
        
        queriedMaster = masterService.createQuery().addFilter("id", m.getId())
                .joinFetchByFilter("2", Slave.class)
                .unique();

        TestCase.assertNull(queriedMaster.getSlaves());

    }
    
    
    @Autowired
    private EnumComponentService ecs;
    
    @Autowired
    private EnumRoleService ers;
    
    /**
     * <b>@description 枚举字段测试 </b>
     */
    @Test
    public void enumFieldTest() {
    	EnumRole er = new EnumRole();
    	er.setRoleCode(EnumRoleEnum.World);
    	er.setAge(10);
    	ers.add(er);
    	
    	EnumComponent ec = new EnumComponent();
    	ec.setComponentCode(ComponentCodeEnum.Hello);
    	ec.setRole(er);
    	ecs.add(ec);
    	
    	EnumComponent c = ecs.getByID(ec.getComponentCode());
    	
    	TestCase.assertEquals(c.getComponentCode(), ec.getComponentCode());
    	TestCase.assertEquals(c.getRole().getRoleCode(), ec.getRole().getRoleCode());
    	
    	List<EnumComponent> list = ecs.createQuery()
    			.addFilter("componentCode", new ComponentCodeEnum[] {ComponentCodeEnum.Hello, ComponentCodeEnum.World },
    					FilterType.IN)
    			.fetch(EnumRole.class)
    			.list();
    	
    	TestCase.assertEquals(list.size(), 1);
    	TestCase.assertEquals(list.get(0).getRole().getAge(), er.getAge());
    }
    
    @Autowired
    DMLHomeService homeService;
    
    @Test
    public void dmlFilterExceptionTest() {
    	try {
    		Query<DMLUser> query = dmlUs.createQuery();
        	query.addFilter(new ManyToOneFilter(DMLHome.class).on("id", 1));
        	query.addFilter(new ManyToOneFilter(DMLHome.class).on("name", "lisi"));
        	query.addFilter("id", 10, DMLHome.class);
        	query.list();
        	throw new RuntimeException();
    	} catch (Exception e) {
			TestCase.assertEquals(RepeatedDMLFilterException.class, e.getClass());
		}
    	try {
    		Query<DMLUser> query = dmlUs.createQuery();
    		query.addFilter(new ManyToOneFilter(DMLHome.class).on("id", 1)
    				.add(new ManyToOneFilter(DMLHome.class).on("name", "lisi")));
    		query.list();
    		throw new RuntimeException();
    	} catch (Exception e) {
    		TestCase.assertEquals(InvalidFetchOperationException.class, e.getClass());
    	}
    	try {
    		Query<DMLUser> query = dmlUs.createQuery();
    		query.addFilter(new ManyToManyFilter(DMLRole.class).on("id", 1));
    		query.addJoinFilter("id", 2, DMLRole.class);
    		query.list();
    		throw new RuntimeException();
    	} catch (Exception e) {
    		TestCase.assertEquals(ConflictFilterException.class, e.getClass());
    	}
    	try {
    		dmlUs.createQuery().joinFetch(DMLRole.class)
			.addFilter(
					new ManyToManyFilter(DMLRole.class).on("id", 1).add(
							new ManyToManyFilter(DMLResource.class).on("name", "").add(
									new ManyToOneFilter(DMLUri.class).on("id", 1)
									)
							).add(
	    							new ManyToManyFilter(DMLResource.class).on("name", "").add(
	    									new ManyToOneFilter(DMLUri.class).on("id", 1)
	    									)
	    							)
					)
			.unique();
    		throw new RuntimeException();
    	} catch (Exception e) {
    		TestCase.assertEquals(RepeatedDMLFilterException.class, e.getClass());
		}
    }
    
    @Autowired
    private DMLOrgService orgService;
    
    @Test
    public void dmlFilterTest() {
    	DMLHome h = new DMLHome();
    	h.setName("myhome");
    	homeService.add(h);
    	
    	DMLUser user = new DMLUser();
    	user.setName("dmlUser");
    	user.setHome(h);
    	dmlUs.add(user);
    	DMLRole r1 = addRole("role1");
    	DMLRole r2 = addRole("role2");
    	DMLOrg org = new DMLOrg();
    	org.setName("myorg");
    	orgService.add(org);
    	r1.setOrg(org);
    	dmlRs.add(r2);
    	dmlRs.add(r1);
    	
    	DMLUri u1 = createURI("uri1");
    	DMLUri u2 = createURI("uri2");
    	dmlUris.add(u1);
    	dmlUris.add(u2);
    	
    	DMLResource res1 = createResource("DMLResource1");
    	res1.setUri(u1);
    	DMLResource res2 = createResource("DMLResource2");
    	res2.setUri(u2);
    	DMLResource res3 = createResource("DMLResource3");
    	DMLResource res4 = createResource("DMLResource4");
    	dmlResourceService.add(res1);
    	dmlResourceService.add(res2);
    	dmlResourceService.add(res3);
    	dmlResourceService.add(res4);
    	r1.setResources(Arrays.asList(res1, res2));
    	r2.setResources(Arrays.asList(res3, res4));
    	
    	dmlRs.addJoinRecords(r1);
    	dmlRs.addJoinRecords(r2);
    	user.setRoles(Arrays.asList(r1, r2));
    	dmlUs.addJoinRecords(user);
    	
    	TestCase.assertEquals(dmlUs.createQuery().joinFetch(DMLRole.class).unique().getRoles().size(), 2);
    	
    	DMLUser queryUser = dmlUs.createQuery().joinFetch(DMLRole.class)
    			.addFilter(new ManyToManyFilter(DMLRole.class).on("id", r1.getId()))
    			.unique();
    	TestCase.assertEquals(queryUser.getRoles().size(), 1);
    	TestCase.assertEquals(queryUser.getRoles().get(0).getName(), r1.getName());
    	
    	
    	TestCase.assertNull(dmlUs.createQuery().joinFetch(DMLRole.class)
    			.addFilter(new ManyToManyFilter(DMLRole.class).on("id", r1.getId())
    					.add(new ManyToManyFilter(DMLResource.class).on("name", res3.getName())))
    			.unique());
    	
    	
		queryUser = dmlUs.createQuery().joinFetch(DMLRole.class)
    			.addFilter(new ManyToManyFilter(DMLRole.class).on("id", r1.getId())
    					.add(new ManyToManyFilter(DMLResource.class).on("name", res1.getName()))
    					)
    			.unique();
    	TestCase.assertEquals(queryUser.getRoles().size(), 1);
    	TestCase.assertEquals(queryUser.getRoles().get(0).getName(), r1.getName());
    	
    	
    	
    	
    	queryUser = dmlUs.createQuery().joinFetch(DMLRole.class)
    			.addFilter(
    					new ManyToManyFilter(DMLRole.class).on("id", r1.getId()).add(
    							new ManyToManyFilter(DMLResource.class).on("name", res1.getName())
    								.on("id", res1.getId())
    								.add(
    									new ManyToOneFilter(DMLUri.class).on("id", res1.getId())
    									)
    							).add(new ManyToOneFilter(DMLOrg.class).on("id", org.getId()))
    					)
    			.unique();
    	TestCase.assertEquals(queryUser.getRoles().size(), 1);
    	TestCase.assertEquals(queryUser.getRoles().get(0).getName(), r1.getName());
    	
    	TestCase.assertNull(dmlUs.createQuery().joinFetch(DMLRole.class)
    			.addFilter(
    					new ManyToManyFilter(DMLRole.class).on("id", r1.getId()).add(
    							new ManyToManyFilter(DMLResource.class).on("name", res1.getName())
    								.on("id", res1.getId())
    								.add(
    									new ManyToOneFilter(DMLUri.class).on("id", res1.getId())
    									)
    							).add(new ManyToOneFilter(DMLOrg.class).on("id", org.getId() + 10000))
    					)
    			.unique());
    	
    	queryUser = dmlUs.createQuery()
    		.addFilter(new ManyToOneFilter(DMLHome.class).on("id", h.getId()).on("name", h.getName()))
    		.fetch(DMLHome.class)
    		.unique();
    	TestCase.assertEquals(queryUser.getHome().getName(), h.getName());
    	TestCase.assertEquals( dmlUs.createQuery()
        		.addFilter(new ManyToOneFilter(DMLHome.class).on("id", h.getId()).on("name", h.getName()))
        		.fetch(DMLHome.class).count(), 1);
    }

	private DMLUri createURI(String uriName) {
		DMLUri u1 = new DMLUri();
		u1.setName(uriName);
		return u1;
	}

	private DMLResource createResource(String resourceName) {
		DMLResource res1 = new DMLResource();
		res1.setName(resourceName);
		return res1;
	}

	private DMLRole addRole(String roleName) {
		DMLRole r1 = new DMLRole();
		r1.setName(roleName);
		return r1;
	}
    
}
