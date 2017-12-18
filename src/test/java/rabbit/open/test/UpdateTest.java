package rabbit.open.test;


import junit.framework.TestCase;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import rabbit.open.orm.exception.UnKnownFieldException;
import rabbit.open.test.entity.Organization;
import rabbit.open.test.entity.User;
import rabbit.open.test.service.UserService;

/**
 * <b>Description: 	update测试</b><br>
 * <b>@author</b>	肖乾斌
 * 
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations={"classpath:applicationContext.xml"})
public class UpdateTest {

	@Autowired
	UserService us;
	
	/**
	 * 
	 * <b>Description:	updateByID测试</b><br>	
	 * 
	 */
	@Test
	public void updateByID(){ 
	    User user = new User();
	    user.setId(10L);
	    user.setName("hello");
	    us.updateByID(user);
	}

	/**
	 * 
	 * <b>Description:	update测试</b><br>	
	 * 
	 */
	@Test
	public void update(){
	    Organization org = new Organization();
	    org.setId(10L);
	    User user = new User("lili", 10, null, org);
	    us.createUpdate().set("org", 12).setValue(user).setNull("birth").set("name", "lisi").execute();
	}

	/**
	 * 
	 * <b>Description:	update测试</b><br>	
	 * 
	 */
	@Test
	public void updateFilter(){
	    us.createUpdate().addFilter("id", 1)
	        .addFilter("org", 10L)
	        .set("name", "lisi").execute();
	}

	/**
	 * <b>Description  测试添加非法属性作为过滤条件.</b>
	 */
	@Test
	public void invalidFilterTest(){
	    try{
	        us.createUpdate().set("id", 10).addFilter("idx", 1).execute();
	    } catch (Exception e){
	        TestCase.assertSame(UnKnownFieldException.class, e.getClass());
	    }
	}

	@Test
	public void invalidFieldTest(){
	    try{
	        us.createUpdate().set("idxs", 10).addFilter("id", 1).execute();
	    } catch (Exception e){
	        TestCase.assertSame(UnKnownFieldException.class, e.getClass());
	    }
	}

	/**
	 * 
	 * <b>Description:	update测试</b><br>	
	 * 
	 */
	@Test
	public void updateNullFilter(){
	    us.createUpdate().addNullFilter("birth").set("name", "lisi").execute();
	}

	/**
	 * 
	 * <b>Description:	update测试</b><br>	
	 * 
	 */
	@Test
	public void update2Null(){
	    us.createUpdate().addFilter("id", 1).set("name", null).execute();
	}
	
	/**
	 * 
	 * <b>Description:	多表联合更新</b><br>
	 * @throws Exception	
	 * 
	 */
	@Test
	public void joinUpdate() throws Exception{
		us.createUpdate().addFilter("id", 1).set("name", "lisi44").addFilter("orgCode", "orgCode", Organization.class).execute();
	}

}
