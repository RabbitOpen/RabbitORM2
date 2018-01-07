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

import rabbit.open.orm.exception.EmptyAliasException;
import rabbit.open.orm.exception.MisMatchedNamedQueryException;
import rabbit.open.orm.exception.NoNamedSQLDefinedException;
import rabbit.open.orm.exception.RepeatedAliasException;
import rabbit.open.orm.exception.UnExistedNamedSQLException;
import rabbit.open.test.entity.Car;
import rabbit.open.test.entity.Organization;
import rabbit.open.test.entity.Property;
import rabbit.open.test.entity.Resources;
import rabbit.open.test.entity.Role;
import rabbit.open.test.entity.User;
import rabbit.open.test.entity.Zone;
import rabbit.open.test.service.CarService;
import rabbit.open.test.service.OrganizationService;
import rabbit.open.test.service.PropertyService;
import rabbit.open.test.service.ResourcesService;
import rabbit.open.test.service.RoleService;
import rabbit.open.test.service.UserService;
import rabbit.open.test.service.ZoneService;

/**
 * <b>Description: 	NamedQuery测试</b><br>
 * <b>@author</b>	肖乾斌
 * 
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations={"classpath:applicationContext.xml"})
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
	
	/**
	 * 
	 * <b>Description:	命名查询测试</b><br>	
	 * @throws Exception 
	 * 
	 */
	@Test
	public void namedQueryTest() {
	    User user = createTestData();
	    User u = us.createNamedQuery("getUserByName")
                .setValue("username", "%leifeng%")
                .setValue("userId", user.getId())
				.execute().unique();
		System.out.println(u);
		TestCase.assertEquals(user.getName(), u.getName());
		TestCase.assertEquals(u.getCars().size(), 3);
		TestCase.assertEquals(u.getRoles().size(), 2);
		TestCase.assertEquals(u.getOrg().getZone().getName(), user.getOrg().getZone().getName());
	}

	@Test
	public void countTest() {
	    User user = createTestData();
	    long count = us.createNamedQuery("getUserByName")
    	            .setValue("username", "%leifeng%")
    	            .setValue("userId", user.getId())
    	            .count();
	    //角色个数 * 车辆个数 * 属性个数【 笛卡尔积】
	    TestCase.assertEquals(2 * 3 * 2 , count);
	}

	@Test
	public void misMatchedNamedQueryExceptionTest() {
	    try {
	        us.createNamedQuery("misMatchedNamedQueryExceptionTest")
                .execute().unique();
	    } catch (Exception e){
	        TestCase.assertEquals(e.getClass(), MisMatchedNamedQueryException.class);
	    }
	}
	
	@Test
	public void emptyAliasExceptionTest() {
	    try{
	        us.createNamedQuery("emptyAliasExceptionTest")
                .setValue("userId", 1)
                .execute().unique();
	    }catch(Exception e){
	        TestCase.assertEquals(e.getClass(), EmptyAliasException.class);
	    }
	}

	@Test
	public void repeatedAliasExceptionTest() {
	    try{
	        us.createNamedQuery("repeatedAliasExceptionTest")
    	        .setValue("userId", 1)
    	        .execute().unique();
	    }catch(Exception e){
	        TestCase.assertEquals(e.getClass(), RepeatedAliasException.class);
	    }
	}
	
	@Test
	public void noNamedSQLDefinedTest(){
	    try {
	        os.createNamedQuery("xx").execute();
	    } catch (Exception e){
	        TestCase.assertEquals(NoNamedSQLDefinedException.class, e.getClass());
	    }
	}

	@Test
	public void unExistedNamedSQLTest(){
	    try {
	        us.createNamedQuery("xx").execute();
	    } catch (Exception e){
	        TestCase.assertEquals(UnExistedNamedSQLException.class, e.getClass());
	    }
	}

	/**
	 * 
	 * <b>Description:	生成测试数据数据</b><br>	
	 * 
	 */
	private User createTestData() {
	    Zone z = new Zone("华北");
        zs.add(z);
		User user = new User();
        //添加组织
        Organization org = new Organization("FBI", "联邦调查局",z);
        os.add(org);
        
        //添加角色
        List<Role> roles = new ArrayList<Role>();
        for(int i = 555; i < 555 + 2; i++){
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
        
        user.setName("leifeng" + System.currentTimeMillis());
        user.setBirth(new Date());
        us.add(user);
        
        //添加用户角色之间的映射关系
        user.setRoles(roles);
        us.addJoinRecords(user);
        
        //添加车辆
        cs.add(new Car("川A110", user));
        cs.add(new Car("川A120", user));
        cs.add(new Car("川A130", user));
        
        ps.add(new Property(user.getOrg().getId(), "P1"));
        ps.add(new Property(user.getOrg().getId(), "P2"));
        
        return user;
	}
}
