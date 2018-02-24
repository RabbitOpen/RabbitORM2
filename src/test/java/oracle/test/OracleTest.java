package oracle.test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import junit.framework.TestCase;
import oracle.test.entity.Organization;
import oracle.test.entity.Role;
import oracle.test.entity.User;
import oracle.test.service.OracleOrganizationService;
import oracle.test.service.OracleRoleService;
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
	
	@Autowired
	OracleOrganizationService os;
	
	@Autowired
	OracleRoleService rs;

	
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
		TestCase.assertTrue(list.size() > 0);
	}

	@Test
    public void simpleQueryTest(){
        User user = addInitData(100);
        List<User> list = us.createQuery(user)
                .joinFetch(Role.class)
                .fetch(Organization.class)
                .distinct()
                .execute().list();
        TestCase.assertTrue(list.size() > 0);
    }
	
	/**
     * 
     * <b>Description:  添加测试数据</b><br>.
     * @param start 
     * 
     */
    public User addInitData(int start){
        User user = new User();
        //添加组织
        Organization org = new Organization("FBI", "联邦调查局");
        os.add(org);
        
        //添加角色
        List<Role> roles = new ArrayList<Role>();
        for(int i = start; i < start + 2; i++){
            Role r = new Role("R" + i);
            rs.add(r);
            roles.add(r);
        }
        
        //添加用户
        user.setOrg(org);
        user.setBigField(new BigDecimal(1));
        user.setShortField((short) 1);
        user.setDoubleField(0.1);
        user.setFloatField(0.1f);
        
        user.setName("zhangsan" + System.currentTimeMillis());
        user.setBirth(new Date());
        us.add(user);
        
        //添加用户角色之间的映射关系
        user.setRoles(roles);
        us.addJoinRecords(user);
        
        return user;
        
    }
}
