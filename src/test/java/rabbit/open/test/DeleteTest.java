package rabbit.open.test;

import java.util.Date;

import junit.framework.TestCase;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import rabbit.open.test.entity.Organization;
import rabbit.open.test.entity.User;
import rabbit.open.test.service.UserService;

/**
 * <b>Description: 	delete测试</b><br>
 * <b>@author</b>	肖乾斌
 * 
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations={"classpath:applicationContext.xml"})
public class DeleteTest {

	@Autowired
	UserService us;
	
	public User addInitUser(){
	    User user = new User();
		user.setName("wangwu");
		user.setBirth(new Date());
		us.add(user);
		return user;
	}
	
	/**
	 * 
	 * <b>Description:	清除表中所有数据</b><br>	
	 * 
	 */
	@Test
	public void clear(){
	    us.clearAll();
	    TestCase.assertEquals(0, us.createQuery().count());
	}
	
	@Test
	public void deleteByID(){
	    us.deleteByID(1);
	    TestCase.assertEquals(0, us.createQuery().addFilter("id", 1).count());
	}

	@Test
	public void delete(){
	    User user = addInitUser();
		System.out.println(us.delete(user));
	}

	@Test
	public void deleteFilterTest(){
	    us.createDelete().addFilter("id", 3).execute();
	    TestCase.assertEquals(0, us.createQuery().addFilter("id", 3).count());
	}

	@Test
	public void deleteFilterTest2(){
	    long result = us.createDelete().addNullFilter("birth").addFilter("orgCode", "myorg", Organization.class).execute();
	    System.out.println("deleteFilterTest2 : " + result);
	}
	
	
}
