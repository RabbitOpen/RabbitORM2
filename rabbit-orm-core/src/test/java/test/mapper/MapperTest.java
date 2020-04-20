package test.mapper;

import junit.framework.TestCase;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import rabbit.open.orm.core.dialect.page.PageHelper;
import rabbit.open.orm.core.dml.SQLQuery;
import test.mapper.entity.MapperRole;
import test.mapper.entity.MappingUser;
import test.mapper.entity.MappingUserBean;

import java.util.List;

/**
 * <b>@description mapper测试 </b>
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:mapper.xml" })
public class MapperTest {

	@Autowired
	UserMapper userMapper;

	@Autowired
	MapperUserService us;
	
	@Autowired
	private MapperRoleService roleService;

	@Test
	public void mapperTest() {
		userMapper.toString();
		// hashCode被重载了
		TestCase.assertEquals(0, userMapper.hashCode());
		// equal被重载了
		TestCase.assertTrue(userMapper.equals(us));
		MappingUser u = new MappingUser();
		u.setName("leifeng");
		us.add(u);
		
		PageHelper.page(0, 10);
		MappingUser user = userMapper.getUser(u.getId(), "leifeng");
		user = userMapper.getUser(u.getId(), "leifeng");
		TestCase.assertEquals(u.getName(), user.getName());
		TestCase.assertEquals(u.getId(), user.getId());

		// 更新
		String newName = "asdf";
		userMapper.updateNameById(u.getId(), newName);
		TestCase.assertEquals(us.getByID(u.getId()).getName(), newName);

		PageHelper.page(0, 10);
		MappingUser userByJdbc = userMapper.getUserByJdbc(u.getId());
		userByJdbc = userMapper.getUserByJdbc(u.getId());
		TestCase.assertEquals(userByJdbc.getName(), newName);
		TestCase.assertEquals(userByJdbc.getUsername(), newName);

		PageHelper.page(0, 10);
		List<MappingUser> users = userMapper.getUserByJdbcs(u.getId());
		TestCase.assertEquals(1, users.size());
		TestCase.assertEquals(users.get(0).getName(), newName);
		TestCase.assertEquals(users.get(0).getUsername(), newName);
		
		PageHelper.page(0, 10);
		List<MappingUserBean> beans = userMapper.getUserByJdbc2Bean(u.getId());
		TestCase.assertEquals(1, users.size());
		TestCase.assertEquals(beans.get(0).getName(), newName);
		TestCase.assertEquals(beans.get(0).getUsername(), newName);

		// 删除
		userMapper.namedDelete(u.getId());
		TestCase.assertNull(us.getByID(u.getId()));
	}


	@Test
	public void namedDeleteTest() {
		MappingUser u = new MappingUser();
		u.setName("leifengxx");
		us.add(u);
		TestCase.assertEquals(1, us.createQuery().addFilter("name", u.getName()).count());
		us.createNamedDelete("namedDelete").set("userId", u.getId(), null, null).execute();
		TestCase.assertEquals(0, us.createQuery().addFilter("name", u.getName()).count());
	}

	@Test
	public void namedUpdateTest() {
		MappingUser u = new MappingUser();
		u.setName("leifengxx");
		us.add(u);
		String newName = "nsx";
		us.createNamedUpdate("updateNameById").set("name", newName, null, null)
			.set("userId", u.getId(), null, null).execute();
		TestCase.assertEquals(newName, us.getByID(u.getId()).getName());
	}

	@Test
	public void namedJdbcTest() {
		
		MappingUser u = new MappingUser();
		u.setName("leifengxx222");
		us.add(u);
		
		SQLQuery<MappingUser> query = us.createSQLQuery("getUserByJdbc");
		MappingUser user = query.page(0, 1)
				.set("userId", u.getId(), "id", MappingUser.class).unique();
		user = query.unique();
		
		TestCase.assertEquals(u.getName(), user.getName());
	}

	@Test
	public void namedJdbcTest2() {
		MapperRole role = new MapperRole();
		role.setName("my-role");
		roleService.add(role);
		
		MappingUser u = new MappingUser();
		u.setName("leifengxx222");
		u.setRole(role);
		u.setAge(100);
		us.add(u);
		
		SQLQuery<MappingUser> query = us.createSQLQuery("getUserByJdbc2");
		MappingUser user = query.page(0, 1)
				.set("userId", u.getId(), "id", MappingUser.class).unique();
		user = query.unique();
		
		TestCase.assertEquals(u.getName(), user.getName());
		TestCase.assertEquals(user.getRole().getId(), role.getId());
		
		MappingUserBean ub = query.unique(MappingUserBean.class);
		TestCase.assertEquals(u.getName(), ub.getName());
		TestCase.assertEquals(ub.getRole().getId(), role.getId());
		
		TestCase.assertEquals(1, query.count());
		TestCase.assertEquals(1, query.count());
	}
}
