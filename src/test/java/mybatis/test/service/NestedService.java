package mybatis.test.service;

import mybatis.test.mybatis.dao.UserDao;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NestedService {

	@Autowired
	UserDao userDao;
	
	@Autowired
	MUserService us;
	
	/**
	 * <b>@description 全部回滚 </b>
	 * @param name
	 */
	@Transactional
	public void testRollAll(String name) {
		userDao.add(name);
		us.nestedTranAdd(name);
	}

	/**
	 * <b>@description 只回滚部分 </b>
	 * @param name
	 */
	@Transactional
	public void testRollNested(String name) {
		userDao.add(name);
		try {
			us.nestedTranAdd(name);
		} catch (Exception e) {
		}
		try {
			us.nestedTranAdd(name);
		} catch (Exception e) {
		}
	}

	/**
	 * <b>@description 不回滚 </b>
	 * @param name
	 */
	@Transactional
	public void noRollback(String name) {
		userDao.add(name);
		try {
			us.tranAdd(name);
		} catch (Exception e) {
		}
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void propagation(String name) {
		userDao.add(name);
	}
}
