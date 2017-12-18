package oracle.test;

import java.util.List;

import oracle.test.entity.Organization;
import oracle.test.entity.Role;
import oracle.test.entity.User;
import oracle.test.service.OracleUserService;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * <b>Description: 	查询测试</b><br>
 * <b>@author</b>	肖乾斌
 * 
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations={"classpath:applicationContext-oracle.xml"})
public class OracleTest {

	@Autowired
	OracleUserService us;

	
	/**
	 * 
	 * <b>Description:  分页 + 排序 + 关联(多对一、多对多)查询 + distinct  </b><br>.	
	 * 
	 */
	@Test
	public void query(){
	    User u = new User();
	    u.setAge(10);
	    us.add(u);
		List<User> list = us.createQuery()
		        .page(0, 10)
		        .joinFetch(Role.class)
		        .fetch(Organization.class)
		        .distinct()
		        .execute().list();
		list.forEach(us -> System.out.println(us));
	}


}
