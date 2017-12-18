package rabbit.open.test;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

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
	
	User user = new User();
	
	/**
	 * 
	 * <b>Description:	添加中间表记录</b><br>	
	 * 
	 */
	@Test
	public void addJoinRecordsTest(){
		addRecords(1);
		query();
	}

	/**
	 * 
	 * <b>Description:	删除中间表记录</b><br>	
	 * 
	 */
	@Test
	public void removeJoinRecordsTest(){
		addRecords(2);
		query();
		us.removeJoinRecords(user);
		query();
	}
	
	/**
	 * 
	 * <b>Description:	清空中间表记录</b><br>	
	 * 
	 */
	@Test
	public void clearJoinRecordsTest(){
		addRecords(3);
		query();
		us.clearJoinRecords(user, Role.class);
		query();
	}
	
	/**
	 * 
	 * <b>Description:	替换中间表记录</b><br>	
	 * 
	 */
	@Test
	public void replaceJoinRecordsTest(){
		addRecords(1);
		query();
		List<Role> roles = new ArrayList<Role>();
		for(int i = 3; i < 5; i++){
			Role r = new Role("R" + i);
			rs.add(r);
			roles.add(r);
		}
		user.setRoles(roles);
		us.replaceJoinRecords(user);
		query();
	}
	

	private void addRecords(int count) {
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
	}
	
	private void query(){
	    List<User> list = us.createQuery(user)
	            .page(0, 10)
	            .joinFetch(Role.class)
	            .fetch(Organization.class)
	            .execute().list();
	    list.forEach(u -> System.out.println(u));
	}
	
}
