package rabbit.open.test;


import java.util.Date;

import junit.framework.TestCase;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import rabbit.open.orm.exception.UnKnownFieldException;
import rabbit.open.test.entity.Organization;
import rabbit.open.test.entity.User;
import rabbit.open.test.service.OrganizationService;
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
	
	@Autowired
	OrganizationService os;
	
	/**
	 * 
	 * <b>Description:	updateByID测试</b><br>	
	 * 
	 */
	@Test
	public void updateByID(){ 
	    User user = new User();
	    user.setName("hello");
	    us.add(user);
	    user.setName("htllo1");
	    us.updateByID(user);
	    TestCase.assertEquals(user.getName(), us.getByID(user.getId()).getName());
	}

	/**
	 * 
	 * <b>Description:	update测试</b><br>	
	 * 
	 */
	@Test
	public void updateTest(){
	    Organization org = new Organization();
	    org.setId(510L);
	    User user = new User("lili", 510, new Date(), org);
	    us.add(user);
	    us.createUpdate().addFilter("id", user.getId()).set("org", 12)
	        .setValue(user)
	        .setNull("birth")
	        .set("name", "lisi")
	        .execute();
	    User u = us.createQuery().addFilter("id", user.getId())
	            .fetch(Organization.class).execute()
	            .unique();
//	    Date birth = u.getBirth();
//        TestCase.assertNull(birth);
//        TestCase.assertEquals(u.getOrg().getId().intValue(), 12);
	    TestCase.assertEquals(u.getName(), "lisi");
	   
	}

	/**
	 * 
	 * <b>Description:	update测试</b><br>	
	 * 
	 */
	@Test
	public void updateFilter(){
	    Organization org = new Organization();
        org.setId(110L);
        User user = new User("lili", 10, null, org);
        us.add(user);
	    us.createUpdate().addFilter("id", user.getId())
	        .addFilter("org", 110L)
	        .set("name", "lisi").execute();
	    User u = us.createQuery().addFilter("id", user.getId()).execute().unique();
	    TestCase.assertEquals(u.getName(), "lisi");
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
        User user = new User("lili", 10, null, null);
        us.add(user);
	    us.createUpdate().addNullFilter("birth").set("name", "lisi").execute();
	    User u = us.createQuery().addFilter("id", user.getId()).execute().unique();
        TestCase.assertEquals(u.getName(), "lisi");
	    
	}

	/**
	 * 
	 * <b>Description:	update测试</b><br>	
	 * 
	 */
	@Test
	public void update2Null(){
	    User user = new User("lili", 10, null, null);
        us.add(user);
	    us.createUpdate(user).addFilter("id", user.getId()).set("name", null).execute();
	    User u = us.createQuery().addFilter("id", user.getId()).execute().unique();
        TestCase.assertNull(u.getName());
	}
	
	/**
	 * 
	 * <b>Description:	多表联合更新</b><br>
	 * @throws Exception	
	 * 
	 */
	@Test
	public void joinUpdate() throws Exception{
	    Organization org = new Organization();
	    org.setName("myorg");
        os.add(org);
        User user = new User("lili", 10, null, org);
        us.add(user);
		String newName = "lisi44";
        us.createUpdate().addFilter("id", user.getId()).set("name", newName)
		    .addFilter("id", org.getId(), Organization.class, User.class)
		    .execute();
		User u = us.createQuery().addFilter("id", user.getId()).execute().unique();
        TestCase.assertEquals(newName, u.getName());
	}

}
