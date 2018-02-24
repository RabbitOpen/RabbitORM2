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

import rabbit.open.test.entity.Organization;
import rabbit.open.test.entity.Role;
import rabbit.open.test.entity.User;
import rabbit.open.test.service.OrganizationService;
import rabbit.open.test.service.RoleService;
import rabbit.open.test.service.UserService;

/**
 * <b>Description: 	关联表测试</b><br>
 * <b>@author</b>	肖乾斌
 * 
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations={"classpath:applicationContext.xml"})
public class JoinTableManagerTest {

	@Autowired
	UserService us;

	@Autowired
	RoleService rs;
	
	@Autowired
	OrganizationService os;
	
	/**
	 * 
	 * <b>Description:	添加中间表记录</b><br>	
	 * 
	 */
	@Test
	public void addJoinRecordsTest(){
		User user = addRecords(1);
		User u = query(user);
		TestCase.assertEquals(u.getRoles().size(), 1);
	}

	/**
	 * 
	 * <b>Description:	删除中间表记录</b><br>	
	 * 
	 */
	@Test
	public void removeJoinRecordsTest(){
		User user = addRecords(2);
		user = query(user);
		TestCase.assertEquals(user.getRoles().size(), 2);
		us.removeJoinRecords(user);
		user = query(user);
		TestCase.assertNull(user.getRoles());
	}
	
	/**
	 * 
	 * <b>Description:	清空中间表记录</b><br>	
	 * 
	 */
	@Test
	public void clearJoinRecordsTest(){
		User user = addRecords(3);
		user = query(user);
		TestCase.assertEquals(user.getRoles().size(), 3);
		us.clearJoinRecords(user, Role.class);
		user = query(user);
		TestCase.assertNull(user.getRoles());
	}
	
	/**
	 * 
	 * <b>Description:	替换中间表记录</b><br>	
	 * 
	 */
	@Test
	public void replaceJoinRecordsTest(){
		User user = addRecords(1);
		user = query(user);
		TestCase.assertEquals(user.getRoles().size(), 1);
		List<Role> roles = new ArrayList<Role>();
		for(int i = 3; i < 5; i++){
			Role r = new Role("R" + i);
			rs.add(r);
			roles.add(r);
		}
		user.setRoles(roles);
		us.replaceJoinRecords(user);
		user = query(user);
		TestCase.assertEquals(user.getRoles().size(), 3);
	}
	

	private User addRecords(int count) {
	    User user = new User();
		user.setName("zhangsan" + System.currentTimeMillis());
		user.setBirth(new Date());
		us.add(user);
		List<Role> roles = new ArrayList<Role>();
		for(int i = 0; i < count; i++){
			Role r = new Role("R" + i);
			rs.add(r);
			roles.add(r);
		}
		user.setRoles(roles);
		us.addJoinRecords(user);
		return user;
	}
	
	private User query(User user){
	    return us.createQuery(user)
	            .joinFetch(Role.class)
	            .fetch(Organization.class)
	            .execute().unique();
	}
	
}
