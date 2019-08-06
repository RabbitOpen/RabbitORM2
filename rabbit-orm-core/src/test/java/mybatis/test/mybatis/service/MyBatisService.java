package mybatis.test.mybatis.service;

import mybatis.test.mybatis.dao.OrgDao;
import mybatis.test.mybatis.dao.UserDao;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MyBatisService {

	@Autowired
	private OrgDao orgDao;
	
	@Autowired
	private UserDao userDao;
	
	public int addUser(String name) {
		userDao.add(name);
		return userDao.count(name);
	}

	public int addOrg(String name) {
		orgDao.add(name);
		return orgDao.count(name);
	}
	
	@Transactional
	public void tranAddUser(String name) {
		userDao.add(name);
	}

	@Transactional
	public void tranAddOrg(String name) {
		orgDao.add(name);
	}
	
}
