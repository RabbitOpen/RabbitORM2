package test.mapper;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import junit.framework.TestCase;
import test.mapper.entity.MappingUser;

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

	@Test
	public void mapperTest() {
		userMapper.toString();
		//hashCode被重载了
		TestCase.assertEquals(0, userMapper.hashCode());
		//equal被重载了
		TestCase.assertTrue(userMapper.equals(us));
		MappingUser u = new MappingUser();
		u.setName("leifeng");
		us.add(u);
		MappingUser user = userMapper.getUser(u.getId(), "leifeng");
		TestCase.assertEquals(u.getName(), user.getName());
		TestCase.assertEquals(u.getId(), user.getId());
		
		//更新
		String newName = "asdf";
		userMapper.updateNameById(u.getId(), newName);
		TestCase.assertEquals(us.getByID(u.getId()).getName(), newName);
		
		//删除
		userMapper.namedDelete(u.getId());
		TestCase.assertNull(us.getByID(u.getId()));
	}
}
