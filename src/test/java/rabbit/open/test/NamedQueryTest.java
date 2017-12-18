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

import rabbit.open.orm.annotation.Relation.FilterType;
import rabbit.open.orm.dml.Result;
import rabbit.open.orm.exception.UnSupportedMethodException;
import rabbit.open.test.entity.Role;
import rabbit.open.test.entity.User;
import rabbit.open.test.service.RoleService;
import rabbit.open.test.service.UserService;

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
	
	/**
	 * 
	 * <b>Description:	命名查询测试</b><br>	
	 * @throws Exception 
	 * 
	 */
	@Test
	public void namedQueryTest(){
		createTestData();
		Result<User> result = us.createNamedQuery("getUserByName")
				.joinFetch(Role.class)
				.distinct()
				.alias(User.class, "u")
				.alias(Role.class, "r")
				.addFilter("id", 1)
				.addFilter("id", 1, FilterType.LIKE)
				.addNullFilter("id")
                .setParameterValue("username", "%zhangsan%")
                .setParameterValue("userId", 1)
				.execute();
		result.list().forEach(u -> System.out.println(u));
		
		result = us.createNamedQuery("getUserByName")
				.alias(User.class, "u")
				.alias(Role.class, "r")
				.setParameterValue("userId", 1)
				.setParameterValue("username", "%zhangsan%")
				.execute();
		result.list().forEach(u -> System.out.println(u));
	}

	@Test
	public void unsupportedMethodTest() {
	    try{
	        us.createNamedQuery("getUserByName").count();
	    }catch(Exception e){
	        TestCase.assertSame(UnSupportedMethodException.class, e.getClass());
	    }
	    try{
	        us.createNamedQuery("getUserByName").addInnerJoinFilter(null);
	    }catch(Exception e){
	        TestCase.assertSame(UnSupportedMethodException.class, e.getClass());
	    }
	}

	/**
	 * 
	 * <b>Description:	生成测试数据数据</b><br>	
	 * 
	 */
	private void createTestData() {
		User user = new User();
		user.setName("zhangsan" + System.currentTimeMillis());
		user.setBirth(new Date());
		us.add(user);
		List<Role> roles = new ArrayList<Role>();
		for(int i = 0; i < 3; i++){
			Role r = new Role("R" + i);
			rs.add(r);
			roles.add(r);
		}
		user.setRoles(roles);
		us.addJoinRecords(user);
	}
}
