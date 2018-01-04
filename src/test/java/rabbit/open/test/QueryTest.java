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

import rabbit.open.orm.annotation.Relation.FilterType;
import rabbit.open.orm.dml.Query;
import rabbit.open.orm.dml.meta.JoinFilterBuilder;
import rabbit.open.orm.exception.InvalidFetchOperationException;
import rabbit.open.orm.exception.InvalidJoinFetchOperationException;
import rabbit.open.orm.exception.OrderAssociationException;
import rabbit.open.test.entity.Car;
import rabbit.open.test.entity.Organization;
import rabbit.open.test.entity.Property;
import rabbit.open.test.entity.Resources;
import rabbit.open.test.entity.Role;
import rabbit.open.test.entity.UUIDPolicyEntity;
import rabbit.open.test.entity.User;
import rabbit.open.test.entity.ZProperty;
import rabbit.open.test.entity.Zone;
import rabbit.open.test.service.CarService;
import rabbit.open.test.service.OrganizationService;
import rabbit.open.test.service.PropertyService;
import rabbit.open.test.service.ResourcesService;
import rabbit.open.test.service.RoleService;
import rabbit.open.test.service.UserService;
import rabbit.open.test.service.ZPropertyService;
import rabbit.open.test.service.ZoneService;

/**
 * <b>Description: 	查询测试</b><br>
 * <b>@author</b>	肖乾斌
 * 
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations={"classpath:applicationContext.xml"})
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
	/**
	 * 
	 * <b>Description:  添加测试数据</b><br>.
	 * @param start	
	 * 
	 */
	public User addInitData(int start){
	    User user = new User();
	    //添加组织
	    Organization org = new Organization("FBI", "联邦调查局");
	    os.add(org);
	    
	    //添加角色
	    List<Role> roles = new ArrayList<Role>();
	    for(int i = start; i < start + 2; i++){
	        Role r = new Role("R" + i);
	        rs.add(r);
	        roles.add(r);
	        //构建资源
	        List<Resources> resources = new ArrayList<Resources>();
	        for(int j = 0; j < 2; j++){
	            Resources rr = new Resources("baidu_" + j + i + ".com");
	            resService.add(rr);
	            resources.add(rr);
	        }
	        //添加角色资源映射关系
	        r.setResources(resources);
	        rs.addJoinRecords(r);
	    }
	    
	    //添加用户
	    user.setOrg(org);
	    user.setBigField(new BigDecimal(1));
	    user.setShortField((short) 1);
	    user.setDoubleField(0.1);
	    user.setFloatField(0.1f);
	    
	    user.setName("zhangsan" + System.currentTimeMillis());
		user.setBirth(new Date());
		us.add(user);
		
		//添加用户角色之间的映射关系
		user.setRoles(roles);
		us.addJoinRecords(user);
		
		//添加车辆
		cs.add(new Car("川A110", user));
		cs.add(new Car("川A120", user));
		cs.add(new Car("川A130", user));
		
		return user;
		
	}
	
	/**
	 * 
	 * <b>Description:  分页 + 排序 + 关联(多对一、多对多)查询 + distinct  </b><br>.	
	 * 
	 */
	@Test
	public void query(){
		User user = addInitData(100);
		List<User> list = us.createQuery(user)
		        .page(0, 10)
		        .joinFetch(Role.class)
		        .fetch(Organization.class)
		        .distinct()
		        .execute().list();
		list.forEach(u -> System.out.println(u));
	}

	@Test
	public void invalidFetchOperationExceptionTest(){
	    try{
	        us.createQuery().fetch(Role.class).execute().list();
	    }catch(Exception e){
	        TestCase.assertSame(e.getClass(), InvalidFetchOperationException.class);
	    }
	}

	@Test
	public void queryByID(){
	    System.out.println(us.getByID(100L));
	}

	@Test
	public void queryTest(){
		addInitData(110);
		List<User> list = us.createQuery()
		        .page(0, 10)
		        .joinFetch(Role.class)
		        .fetch(Organization.class)
		        .addFilter("${id}", new Integer[]{1}, FilterType.IN)
		        .addFilter("birth", new Date(), FilterType.LTE)
		        .addFilter("name", new String[]{"zhangsan"}, FilterType.IN)
		        .addFilter("orgCode", "MY_ORG", Organization.class)
		        .addFilter("org", new Integer[]{1}, FilterType.IN)
		        .alias(User.class, "U")
		        .execute().list();
		list.forEach(u -> System.out.println(u));
	}

	@Test
	public void queryOrderTest(){
	    addInitData(220);
	    List<User> list = us.createQuery()
	            .page(0, 10)
	            .fetch(Organization.class)
	            .desc("id")
	            .asc("name")
	            .execute().list();
	    list.forEach(u -> System.out.println(u));
	}
	
	/**
	 * 
	 * <b>Description:  新增内链接条件测试</b><br>.	
	 * 
	 */
	@Test
	public void innerJoinQueryTest(){
	    addInitData(120);
	    List<User> list = us.createQuery()
                .page(0, 10)
                .joinFetch(Role.class)
                .fetch(Organization.class)
                .addFilter("id", 1)
                .addInnerJoinFilter("id", FilterType.IN, new Integer[]{1, 3, 2}, Role.class)
                .addInnerJoinFilter("roleName", "R121", Role.class)
                .execute().list();
        list.forEach(u -> System.out.println(u));
	}

	/**
	 * 
	 * <b>Description:  新增自定义内链接条件测试</b><br>.	
	 *     ManyToMany
	 * 
	 */
	@Test
	public void joinFilterBuilderTest(){
	    addInitData(120);
	    Query<User> query = us.createQuery();
        List<User> list = query.page(0, 10)
	            .joinFetch(Role.class)
	            .fetch(Organization.class)
	            .addFilter("id", 1)
	            .addInnerJoinFilter(JoinFilterBuilder.prepare(query).join(Role.class)
	                    .on("id", 1).on("roleName", "R120").join(Resources.class).on("${id}", 2L).build())
	            .execute().list();
	    list.forEach(u -> System.out.println(u));
	}

	/**
	 * 
	 * <b>Description:  新增自定义内链接条件测试</b><br>.	
	 *         OneToMany
	 * 
	 */
	@Test
	public void joinFilterBuilderTest2(){
	    addInitData(125);
	    Query<User> query = us.createQuery();
	    List<User> list = query.page(0, 10)
	            .joinFetch(Role.class)
	            .distinct()
	            .alias(Resources.class, "RESOURCES")
	            .fetch(Organization.class)
	            .joinFetch(Car.class)
	            .addInnerJoinFilter(JoinFilterBuilder.prepare(query).join(Role.class)
	                    .on("id", 1).on("roleName", "R120").join(Resources.class).on("${id}", 2L).build())
                .addInnerJoinFilter(JoinFilterBuilder.prepare(query).join(Car.class)
                        .on("${id}", 1).build())
                .execute().list();
	    list.forEach(u -> System.out.println(u));
	}

	/**
	 * 
	 * <b>Description: 非法排序测试 </b><br>.	
	 * 
	 */
	@Test
	public void wrongOrderTest(){
	    try{
	        us.createQuery().desc("id", UUIDPolicyEntity.class).execute();
	    } catch (Exception e){
	        System.out.println(e.getMessage());
	        TestCase.assertSame(e.getClass(), OrderAssociationException.class);
	    }
	}
	
	/**
	 * 
	 * <b>Description:  条数统计测试</b><br>.	
	 * 
	 */
	@Test
	public void countTest(){
	    addInitData(130);
	    Query<User> query = us.createQuery();
	    long count = query.page(0, 10)
	            .joinFetch(Role.class)
	            .fetch(Organization.class)
	            .joinFetch(Car.class)
	            .addInnerJoinFilter(JoinFilterBuilder.prepare(query).join(Role.class)
	                    .on("id", 1).on("roleName", "R120").join(Resources.class).on("${id}", 2L).build())
                .addInnerJoinFilter(JoinFilterBuilder.prepare(query).join(Car.class)
                        .on("${id}", 1).build())
	            .count();
	    System.out.println(count);
	}
	
	/**
	 * 
	 * <b>Description:  addJoinFilterTest</b><br>.	
	 * 
	 */
	@Test
	public void addJoinFilterTest(){
	    addInitData(150);
        List<User> list = us.createQuery()
                .page(0, 10)
//                .joinFetch(Role.class)
                .joinFetch(Car.class)
                .addJoinFilter("id", 1, Role.class)
                .addJoinFilter("roleName", "R150", Role.class)
                .addJoinFilter("id", 2, Car.class)
                .execute().list();
        list.forEach(u -> System.out.println(u));
	}

	/**
	 * 
	 * <b>Description:  addJoinFilterTest</b><br>.	
	 * 
	 */
	@Test
	public void invalidJoinFilterTest(){
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
            us.createQuery().page(0, 10).addJoinFilter("name", "name", Organization.class)
                    .execute().list();
            throw new RuntimeException();
        } catch (Exception e) {
            TestCase.assertSame(e.getClass(),
                    InvalidJoinFetchOperationException.class);
        }
	}
	
	/**
	 * 
	 * <b>Description:  addNullFilterTest</b><br>.	
	 * 
	 */
	@Test
	public void addNullFilterTest(){
	    addInitData(150);
	    List<User> list = us.createQuery()
	            .page(0, 10)
	            .addNullFilter("id", false)
	            .joinFetch(Role.class)
	            .joinFetch(Car.class)
	            .addJoinFilter("id", 1, Role.class)
	            .addJoinFilter("roleName", "R150", Role.class)
	            .addJoinFilter("id", 2, Car.class)
	            .execute().list();
	    list.forEach(u -> System.out.println(u));
	    
	    list = us.createQuery()
	            .page(0, 10)
	            .addNullFilter("id")
	            .joinFetch(Role.class)
	            .joinFetch(Car.class)
	            .addJoinFilter("id", 1, Role.class)
	            .addJoinFilter("roleName", "R150", Role.class)
	            .addJoinFilter("id", 2, Car.class)
	            .execute().list();
	    list.forEach(u -> System.out.println(u));
	}
	
	
	public User addInitData2(){
	    Zone z = new Zone("华北");
	    zs.add(z);
	    User user = new User();
        //添加组织
        Organization org = new Organization("FBI", "联邦调查局", z);
        os.add(org);
        
        //添加角色
        List<Role> roles = new ArrayList<Role>();
        for(int i = 1000; i < 1002; i++){
            Role r = new Role("R" + i);
            rs.add(r);
            roles.add(r);
            //构建资源
            List<Resources> resources = new ArrayList<Resources>();
            for(int j = 0; j < 2; j++){
                Resources rr = new Resources("baidu_" + j + i + ".com");
                resService.add(rr);
                resources.add(rr);
            }
            //添加角色资源映射关系
            r.setResources(resources);
            rs.addJoinRecords(r);
        }
        
        //添加用户
        user.setOrg(org);
        user.setBigField(new BigDecimal(1));
        user.setShortField((short) 1);
        user.setDoubleField(0.1);
        user.setFloatField(0.1f);
        
        user.setName("zhangsan" + System.currentTimeMillis());
        user.setBirth(new Date());
        us.add(user);
        
        //添加用户角色之间的映射关系
        user.setRoles(roles);
        us.addJoinRecords(user);
        
        //添加车辆
        cs.add(new Car("川A110", user));
        cs.add(new Car("川A120", user));
        cs.add(new Car("川A130", user));
        return user;
	}
	
	@Test
	public void buildFetchTest(){
	    User user = addInitData2();
	    user.getOrg();
	    ps.add(new Property(user.getOrg().getId(), "P1"));
	    ps.add(new Property(user.getOrg().getId(), "P2"));
	    
	    zps.add(new ZProperty(user.getOrg().getZone().getId(), "zP4"));
	    zps.add(new ZProperty(user.getOrg().getZone().getId(), "zP3"));
	    
	    
	    List<User> list = null;
	   
	    
	    try{
	        //验证非法的joinFetch操作
	        us.createQuery().buildFetch().joinFetch(Property.class).build().execute().list();
	        throw new RuntimeException();
	    }catch(Exception e){
	        TestCase.assertSame(e.getClass(),
                    InvalidJoinFetchOperationException.class);
	    }
	    
	    //主表joinFetch
	    list = us.createQuery().buildFetch()
	            .joinFetch(Role.class).on("id", 1).build().execute().list();
	    list.forEach(u -> System.out.println(u));
	    
	    
	    //从表joinFetch
	    list = us.createQuery().buildFetch().joinFetch(Role.class).fetch(Organization.class)
	            .joinFetch(Property.class).build()
	            .addFilter("id", 1)
	            .addJoinFilter("id", 1, Role.class)
	            .execute().list();
	    list.forEach(u -> System.out.println(u));

	    //从表joinFetch
	    list = us.createQuery().buildFetch().joinFetch(Role.class).fetch(Organization.class).joinFetch(Property.class).fetch(Zone.class)
	            .joinFetch(ZProperty.class).build()
	            .execute().list();
	    list.forEach(u -> System.out.println(u));
	}
}
