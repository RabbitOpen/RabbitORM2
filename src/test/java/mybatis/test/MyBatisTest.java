package mybatis.test;

import junit.framework.TestCase;
import mybatis.test.mybatis.dao.OrgDao;
import mybatis.test.mybatis.dao.UserDao;
import mybatis.test.mybatis.service.MyBatisService;
import mybatis.test.mybatis.service.MyBatisTranService;
import mybatis.test.service.MOrgService;
import mybatis.test.service.MUserService;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * <b>@description mybatis和rabbit混合测试 </b>
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:applicationContext-mybatis.xml" })
public class MyBatisTest {

	@Autowired
	private MyBatisService myBatisService;

	@Autowired
	private MyBatisTranService mbs;

	@Autowired
	private OrgDao orgDao;

	@Autowired
	private UserDao userDao;

	@Autowired
	private MUserService us;

	@Autowired
	MOrgService os;

	@Test
	public void addTest() {
		orgDao.clear("myorg");
		userDao.clear("zhangsan");
		TestCase.assertEquals(1, myBatisService.addUser("zhangsan"));
		TestCase.assertEquals(1, myBatisService.addOrg("myorg"));
	}

	/**
	 * <b>@description 事务测试 </b>
	 */
	@Test
	public void tranTest() {
		String name = "tranxxx";
		orgDao.clear(name);
		userDao.clear(name);
		mbs.tranadd(name);
		TestCase.assertEquals(1, orgDao.count(name));
		TestCase.assertEquals(1, userDao.count(name));

		name = "tranxxxRollback";
		orgDao.clear(name);
		userDao.clear(name);
		try {
			mbs.rollback(name);
		} catch (Exception e) {
			TestCase.assertEquals(0, orgDao.count(name));
			TestCase.assertEquals(0, userDao.count(name));
			return;
		}
		throw new RuntimeException("回滚失败");
	}

	/**
	 * <b>@description mybatis 混合rabbit一起测试 </b>
	 */
	@Test
	public void combinedCallTest() {
		String name = "combinedTest";
		orgDao.clear(name);
		userDao.clear(name);
		us.combinedAdd(name);
		TestCase.assertEquals(1, orgDao.count(name));
		TestCase.assertEquals(2, userDao.count(name));
	}

	/**
	 * <b>@description mybatis 混合rabbit一起测试回滚 </b>
	 */
	@Test
	public void combinedCallRollbackTest() {
		String name = "combinedRollbackTest";
		orgDao.clear(name);
		userDao.clear(name);
		try {
			us.combinedRollback(name);
		} catch (Exception e) {
			TestCase.assertEquals(0, orgDao.count(name));
			TestCase.assertEquals(0, userDao.count(name));
			return;
		}
		throw new RuntimeException("回滚失败");
	}
	
}
