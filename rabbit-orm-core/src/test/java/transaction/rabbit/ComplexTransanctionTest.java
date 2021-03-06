package transaction.rabbit;

import junit.framework.TestCase;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import transaction.rabbit.service.SimpleService;
import transaction.rabbit.service.TOrgService;
import transaction.rabbit.service.TService;
import transaction.rabbit.service.TUserService;

/**
 * <b>@description 复杂事务测试 </b>
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:applicationContext.xml" })
public class ComplexTransanctionTest {

	@Autowired
	SimpleService ss;
	
	@Autowired
	TOrgService os;

	@Autowired
	TUserService us;

	@Autowired
	TService ts;
	
	/**
	 * <b>@description 回滚用户信息 </b>
	 */
	@Test
	public void t1() {
		String name = "user#org-0";
		try {
			ss.userRollback(name);
		} catch (Exception e) {
			TestCase.assertEquals(2, os.createQuery().addFilter("name", name).count());
			TestCase.assertEquals(0, us.createQuery().addFilter("name", name).count());
			return;
		}
		throw new RuntimeException("回滚失败");
	}

	/**
	 * <b>@description 一次事务 </b>
	 */
	@Test
	public void t2() {
		String name = "user-org-1";
		ss.simpleTransAdd(name);
		TestCase.assertEquals(2, os.createQuery().addFilter("name", name).count());
		TestCase.assertEquals(2, us.createQuery().addFilter("name", name).count());
	}

	/**
	 * <b>@description 一次事务 回滚 </b>
	 */
	@Test
	public void t3() {
		String name = "user-org-2";
		try {
			ts.simpleTransRollBack(name);
		} catch (Exception e) {
			TestCase.assertEquals(0, os.createQuery().addFilter("name", name).count());
			TestCase.assertEquals(0, us.createQuery().addFilter("name", name).count());
			return;
		}
		throw new RuntimeException("回滚失败");
	}
	
	
}
