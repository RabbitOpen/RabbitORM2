package rabbit.open.test;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import junit.framework.TestCase;
import rabbit.open.orm.common.exception.NoField2InsertException;
import rabbit.open.orm.common.exception.RabbitDMLException;
import rabbit.open.test.entity.Organization;
import rabbit.open.test.entity.UUIDPolicyEntity;
import rabbit.open.test.entity.User;
import rabbit.open.test.entity.custom.CustomOrg;
import rabbit.open.test.entity.custom.CustomOrgService;
import rabbit.open.test.entity.custom.CustomRole;
import rabbit.open.test.entity.custom.CustomRoleService;
import rabbit.open.test.entity.custom.CustomUser;
import rabbit.open.test.entity.custom.CustomUserService;
import rabbit.open.test.service.UUIDEntityService;
import rabbit.open.test.service.UserService;

/**
 * <b>Description: 新增测试</b><br>
 * <b>@author</b> 肖乾斌
 * 
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:applicationContext.xml" })
public class InsertTest {

    @Autowired
    UserService us;

    @Autowired
    UUIDEntityService uus;

    /**
     * 
     * <b>Description: 新增数据测试</b><br>
     * 
     */
    @Test
    public void addDataTest() {
        User user = new User();
        user.setName("zhangsan");
        us.add(user);
        user = new User();
        user.setName("zhangsan1");
        user.setOrg(new Organization("MY_ORG", "MY_ORG_NAME"));
        us.add(user);
        User query = us.getByID(user.getId());
        TestCase.assertNull(query.getOrg());

        // uuid策略测试
        UUIDPolicyEntity data = new UUIDPolicyEntity("UUIDPolicyEntity-lisi");
        uus.add(data);
        UUIDPolicyEntity byID = uus.getByID(data.getId());
        TestCase.assertEquals(data.getId(), byID.getId());
        TestCase.assertEquals(data.getName(), byID.getName());
    }
    
    @Autowired
    CustomUserService cus;
    
    @Autowired
    CustomOrgService cos;
    
    /**
     * 
     * <b>@description 自定义关联字段测试 </b>
     */
    @Test
    public void customAddTest() {
    	CustomOrg org1 = new CustomOrg();
    	org1.setName("org-1");
    	cos.add(org1);
    	
    	CustomUser u = new CustomUser();
    	u.setOrg(org1);
    	u.setName("username");
    	cus.add(u);
    	CustomUser unique = cus.createQuery().fetch(CustomOrg.class).addFilter("id", u.getId()).unique();
    	TestCase.assertEquals(unique.getName(), u.getName());
    	TestCase.assertEquals(unique.getOrg().getId(), org1.getId());
    	TestCase.assertEquals(unique.getOrg().getName(), org1.getName());
    	
    	// update 
    	CustomOrg org2 = new CustomOrg();
    	org2.setName("org-2");
    	cos.add(org2);
    	u.setOrg(org2);
    	cus.createUpdate().updateByID(u);
    	
    	unique = cus.createQuery().fetch(CustomOrg.class).addFilter("id", u.getId()).unique();
    	unique = cus.createQuery(u).fetch(CustomOrg.class).unique();
    	TestCase.assertEquals(unique.getName(), u.getName());
    	TestCase.assertEquals(unique.getOrg().getId(), org2.getId());
    	TestCase.assertEquals(unique.getOrg().getName(), org2.getName());
    	
    	
    	org2.setName("org-3");
    	cus.createUpdate().set("org", org2).addFilter("id", u.getId()).execute();
    	unique = cus.createQuery().fetch(CustomOrg.class).addFilter("id", u.getId()).unique();
    	TestCase.assertEquals(unique.getName(), u.getName());
    	// 没有org-3的这个组织
    	TestCase.assertNull(unique.getOrg().getId());
    	TestCase.assertEquals(unique.getOrg().getName(), org2.getName());
    	
    	org2.setName("org-4");
    	org2.setId(null);
    	cos.add(org2);
    	cus.createUpdate().set("org", org2).addFilter("id", u.getId()).execute();
    	unique = cus.createQuery().fetch(CustomOrg.class).addFilter("id", u.getId()).unique();
    	TestCase.assertEquals(unique.getName(), u.getName());
    	TestCase.assertEquals(unique.getOrg().getId(), org2.getId());
    	TestCase.assertEquals(unique.getOrg().getName(), org2.getName());
    	
    	long count = cus.createUpdate().addFilter("name", org2.getName(), CustomOrg.class).set("age", 10).execute();
    	TestCase.assertEquals(count, 1);
    	TestCase.assertEquals(10, cus.getByID(unique.getId()).getAge().intValue());
    	
    	// DELETE
    	TestCase.assertEquals(cus.createDelete(cus.getByID(unique.getId())).execute(), 1);
    	
    }
    
    @Autowired
    CustomRoleService crs;
    
    @Test
    public void customJoinFetch() {
    	CustomUser u = new CustomUser();
    	u.setName("custom-join-user1");
    	List<CustomRole> roles = new ArrayList<>();
    	for (int i = 0; i < 5; i++) {
    		CustomRole role = new CustomRole();
    		role.setName("custom-join-role-" + i);
    		crs.add(role);
    		roles.add(role);
    	}
    	u.setRoles(roles);
    	cus.add(u);
    	cus.addJoinRecords(u);
    	
    	CustomUser unique = cus.createQuery().joinFetch(CustomRole.class).addFilter("id", u.getId()).unique();
    	TestCase.assertEquals(5, unique.getRoles().size());
    	cus.removeJoinRecords(u);
    	unique = cus.createQuery().joinFetch(CustomRole.class).addFilter("id", u.getId()).unique();
    	TestCase.assertNull(unique.getRoles());
    	
    	u.setRoles(roles);
    	cus.addJoinRecords(u);
    	unique = cus.createQuery().joinFetch(CustomRole.class).addFilter("id", u.getId()).unique();
    	TestCase.assertEquals(5, unique.getRoles().size());
    	cus.removeJoinRecords(u, CustomRole.class);
    	unique = cus.createQuery().joinFetch(CustomRole.class).addFilter("id", u.getId()).unique();
    	TestCase.assertNull(unique.getRoles());
    	
    	cus.addJoinRecords(u);
    	unique = cus.createQuery().joinFetch(CustomRole.class).addFilter("id", u.getId()).unique();
    	TestCase.assertEquals(5, unique.getRoles().size());
    	roles = new ArrayList<>();
    	for (int i = 5; i < 8; i++) {
    		CustomRole role = new CustomRole();
    		role.setName("custom-join-role-" + i);
    		crs.add(role);
    		roles.add(role);
    	}
    	u.setRoles(roles);
    	cus.replaceJoinRecords(u);
    	unique = cus.createQuery().joinFetch(CustomRole.class).addFilter("id", u.getId()).unique();
    	TestCase.assertEquals(3, unique.getRoles().size());
    	
    }
    

    @Test
    public void addDataTest2() {
    	String name = "testUserName";
    	int count = 100;
		for (int i = 0; i < count; i++) {
    		User user = new User();
			user.setName(name);
        	us.add(user);
    	}
    	TestCase.assertEquals(count, us.createQuery().addFilter("name", name).count());
    }

    @Test
    public void exceptionTest() {
        uus.add(new UUIDPolicyEntity());
        try {
            us.add(null);
            throw new RuntimeException();
        } catch (Exception e) {
            TestCase.assertSame(RabbitDMLException.class, e.getClass());
        }
        try {
            us.add(new User());
            throw new RuntimeException();
        } catch (Exception e) {
            TestCase.assertSame(NoField2InsertException.class, e.getClass());
        }
    }
}
