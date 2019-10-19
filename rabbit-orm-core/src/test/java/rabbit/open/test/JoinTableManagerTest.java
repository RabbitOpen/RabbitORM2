package rabbit.open.test;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import junit.framework.TestCase;
import rabbit.open.orm.common.exception.EmptyPrimaryKeyValueException;
import rabbit.open.orm.common.exception.InvalidJoinMergeException;
import rabbit.open.test.entity.Car;
import rabbit.open.test.entity.Organization;
import rabbit.open.test.entity.Role;
import rabbit.open.test.entity.User;
import rabbit.open.test.entity.jointable.JCar;
import rabbit.open.test.entity.jointable.JCarService;
import rabbit.open.test.entity.jointable.JRole;
import rabbit.open.test.entity.jointable.JRoleService;
import rabbit.open.test.entity.jointable.JUser;
import rabbit.open.test.entity.jointable.JUserService;
import rabbit.open.test.service.OrganizationService;
import rabbit.open.test.service.RoleService;
import rabbit.open.test.service.UserService;

/**
 * <b>Description: 关联表测试</b><br>
 * <b>@author</b> 肖乾斌
 * 
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:applicationContext.xml" })
public class JoinTableManagerTest {

	@Autowired
	UserService us;

	@Autowired
	RoleService rs;

	@Autowired
	OrganizationService os;

	/**
	 * 
	 * <b>Description: 添加中间表记录</b><br>
	 * 
	 */
	@Test
	public void addJoinRecordsTest() {
		User user = addRecords(1);
		User u = query(user);
		TestCase.assertEquals(u.getRoles().size(), 1);
	}

	/**
	 * 
	 * <b>Description: 删除中间表记录</b><br>
	 * 
	 */
	@Test
	public void removeJoinRecordsTest() {
		User u = addRecords(2);
		User user = query(u);
		TestCase.assertEquals(user.getRoles().size(), 2);
		us.removeJoinRecords(user);
		user = query(u);
		TestCase.assertNull(user.getRoles());
	}

	/**
	 * 
	 * <b>Description: 清空中间表记录</b><br>
	 * 
	 */
	@Test
	public void clearJoinRecordsTest() {
		User u = addRecords(3);
		User user = query(u);
		TestCase.assertEquals(user.getRoles().size(), 3);
		us.removeJoinRecords(u, Role.class);
		user = query(u);
		TestCase.assertNull(user.getRoles());
	}

	@Test
	public void exceptionTest() {
		try {
			User user = new User();
			us.removeJoinRecords(user, Role.class);
			throw new RuntimeException();
		} catch (Exception e) {
			TestCase.assertEquals(PoolTest.getRootCause(e).getClass(), EmptyPrimaryKeyValueException.class);
		}
		try {
			User user = new User();
			us.mergeJoinRecords(user, JRole.class);
			throw new RuntimeException();
		} catch (Exception e) {
			TestCase.assertEquals(PoolTest.getRootCause(e).getClass(), InvalidJoinMergeException.class);
		}
	}

	/**
	 * 
	 * <b>Description: 替换中间表记录</b><br>
	 * 
	 */
	@Test
	public void replaceJoinRecordsTest() {
		User user = addRecords(1);
		user = query(user);
		TestCase.assertEquals(user.getRoles().size(), 1);
		List<Role> roles = new ArrayList<Role>();
		for (int i = 3; i < 5; i++) {
			Role r = new Role("R" + i);
			rs.add(r);
			roles.add(r);
		}
		user.setRoles(roles);
		us.replaceJoinRecords(user, Role.class);
		user = query(user);
		TestCase.assertEquals(user.getRoles().size(), 2);
		TestCase.assertEquals(getRoleByID(roles.get(0).getId(), user.getRoles()).getRoleName(),
				roles.get(0).getRoleName());
		TestCase.assertEquals(getRoleByID(roles.get(1).getId(), user.getRoles()).getRoleName(),
				roles.get(1).getRoleName());

	}

	private Role getRoleByID(Integer id, List<Role> roles) {
		for (Role r : roles) {
			if (id.equals(r.getId())) {
				return r;
			}
		}
		return null;
	}

	private User addRecords(int roleSize) {
		User user = new User();
		user.setName("zhangsan" + System.currentTimeMillis());
		user.setBirth(new Date());
		us.add(user);
		List<Role> roles = new ArrayList<Role>();
		for (int i = 0; i < roleSize; i++) {
			Role r = new Role("R" + i);
			rs.add(r);
			roles.add(r);
		}
		user.setRoles(roles);
		List<Car> cars = new ArrayList<>();
		Car c = new Car();
		c.setId(1);
		cars.add(c);
		user.setCars(cars);
		us.addJoinRecords(user);
		return user;
	}

	private User query(User user) {
		return us.createQuery(user).joinFetch(Role.class).fetch(Organization.class).execute().unique();
	}

	@Autowired
	JUserService jus;

	@Autowired
	JRoleService jrs;

	@Autowired
	JCarService jcs;

	@Test
	public void joinTableManagerTest() {
		JUser u = new JUser();
		u.setName("juser");
		jus.add(u);
		List<JRole> roles = new ArrayList<>();
		for (int i = 0; i < 3; i++) {
			JRole r = new JRole();
			r.setName("role-" + i);
			jrs.add(r);
			roles.add(r);
		}
		u.setRoles(roles);
		List<JCar> cars = new ArrayList<>();
		for (int i = 0; i < 4; i++) {
			JCar c = new JCar();
			c.setName("role-" + i);
			jcs.add(c);
			cars.add(c);
		}
		u.setCars(cars);

		// 验证 addJoinRecords(T data)
		jus.addJoinRecords(u);
		JUser unique = jus.createQuery().addFilter("id", u.getId())
				.joinFetch(JRole.class)
				.joinFetch(JCar.class)
				.unique();
		TestCase.assertEquals(roles.size(), unique.getRoles().size());
		for (int i = 0; i < roles.size(); i++) {
			TestCase.assertEquals(roles.get(i).getName(), unique.getRoles().get(i).getName());
			TestCase.assertEquals(roles.get(i).getId(), unique.getRoles().get(i).getId());
		}
		TestCase.assertEquals(cars.size(), unique.getCars().size());
		for (int i = 0; i < cars.size(); i++) {
			TestCase.assertEquals(cars.get(i).getName(), unique.getCars().get(i).getName());
			TestCase.assertEquals(cars.get(i).getId(), unique.getCars().get(i).getId());
		}

		// 验证 removeJoinRecords(T data)
		jus.removeJoinRecords(u);
		unique = jus.createQuery().addFilter("id", u.getId())
				.joinFetch(JRole.class)
				.joinFetch(JCar.class)
				.unique();
		TestCase.assertNull(unique.getRoles());
		TestCase.assertNull(unique.getCars());

		// 验证 removeJoinRecords(T data, Class<?> joinClass)
		jus.addJoinRecords(u, JRole.class);
		unique = jus.createQuery().addFilter("id", u.getId())
				.joinFetch(JRole.class)
				.joinFetch(JCar.class)
				.unique();
		TestCase.assertNull(unique.getCars());
		TestCase.assertEquals(roles.size(), unique.getRoles().size());
		for (int i = 0; i < roles.size(); i++) {
			TestCase.assertEquals(roles.get(i).getName(), unique.getRoles().get(i).getName());
			TestCase.assertEquals(roles.get(i).getId(), unique.getRoles().get(i).getId());
		}
		
		
		List<JRole> newRoles = new ArrayList<>();
		newRoles.addAll(roles);
		JRole r = new JRole();
		r.setName("role-88");
		jrs.add(r);
		roles.add(r);
		newRoles.add(r);
		newRoles.remove(0);
		TestCase.assertEquals(3, newRoles.size());
		TestCase.assertEquals(4, roles.size());
		
		//验证 mergeJoinRecords(T data, Class<?> joinClass)
		u.setRoles(newRoles);
		jus.mergeJoinRecords(u, JRole.class);
		unique = jus.createQuery().addFilter("id", u.getId())
				.joinFetch(JRole.class)
				.joinFetch(JCar.class)
				.unique();
		TestCase.assertNull(unique.getCars());
		TestCase.assertEquals(roles.size(), unique.getRoles().size());
		for (int i = 0; i < roles.size(); i++) {
			TestCase.assertEquals(roles.get(i).getName(), unique.getRoles().get(i).getName());
			TestCase.assertEquals(roles.get(i).getId(), unique.getRoles().get(i).getId());
		}
		
		//验证 replaceJoinRecords(T data, Class<?> joinClass)
		u.setRoles(newRoles);
		jus.replaceJoinRecords(u, JRole.class);
		unique = jus.createQuery().addFilter("id", u.getId())
				.joinFetch(JRole.class)
				.joinFetch(JCar.class)
				.unique();
		TestCase.assertNull(unique.getCars());
		TestCase.assertEquals(newRoles.size(), unique.getRoles().size());
		for (int i = 0; i < newRoles.size(); i++) {
			TestCase.assertEquals(newRoles.get(i).getName(), unique.getRoles().get(i).getName());
			TestCase.assertEquals(newRoles.get(i).getId(), unique.getRoles().get(i).getId());
		}
	}
	
	
	@Test
	public void joinTableManagerTest2() {
		JUser u = new JUser();
		u.setName("juser");
		jus.add(u);
		List<JRole> roles = new ArrayList<>();
		for (int i = 0; i < 3; i++) {
			JRole r = new JRole();
			r.setName("role-" + i);
			jrs.add(r);
			roles.add(r);
		}
		u.setRoles(roles);
		

		// 验证 addJoinRecords(T data, Class<?> joinClass)
		jus.addJoinRecords(u, JRole.class);
		JUser unique = jus.createQuery().addFilter("id", u.getId())
				.joinFetch(JRole.class)
				.unique();
		TestCase.assertEquals(roles.size(), unique.getRoles().size());
		for (int i = 0; i < roles.size(); i++) {
			TestCase.assertEquals(roles.get(i).getName(), unique.getRoles().get(i).getName());
			TestCase.assertEquals(roles.get(i).getId(), unique.getRoles().get(i).getId());
		}
		
		// 验证 removeJoinRecords(T data, Class<?> joinClass)
		jus.removeJoinRecords(u, JRole.class);
		unique = jus.createQuery().addFilter("id", u.getId())
				.joinFetch(JRole.class)
				.unique();
		TestCase.assertNull(unique.getRoles());
		
	}

}
