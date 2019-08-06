package mybatis.test;

import junit.framework.TestCase;
import mybatis.test.mybatis.dao.UserDao;
import mybatis.test.service.MUserService;
import mybatis.test.service.NestedService;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:applicationContext-mybatis.xml" })
public class RabbitTransactionManagerTest {

	@Autowired
	UserDao userDao;
	
	@Autowired
	MUserService us;
	
	@Autowired
	NestedService nestedService;
	
	/**
	 * <b>@description 全部回滚 </b>
	 */
	@Test
	public void testRollAll() {
		String name = "rollall";
		try {
			userDao.clear(name);
			nestedService.testRollAll(name);
		} catch (Exception e) {
			TestCase.assertEquals(0, userDao.count(name));
			return;
		}
		throw new RuntimeException();
	}

	/**
	 * <b>@description 局部(nested事务部分)回滚 </b>
	 */
	@Test
	public void testRollNested() {
		String name = "testRollNested";
		userDao.clear(name);
		nestedService.testRollNested(name);
		TestCase.assertEquals(1, userDao.count(name));
	}
	
	/**
	 * <b>@description 没有内嵌事务就不进行局部回滚 </b>
	 */
	@Test
	public void noRollbackTest() {
		String name = "noRollbackTest";
		userDao.clear(name);
		nestedService.noRollback(name);
		TestCase.assertEquals(3, userDao.count(name));
	}

}
