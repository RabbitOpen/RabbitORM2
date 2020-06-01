package oracle.test;

import junit.framework.TestCase;
import oracle.test.entity.*;
import oracle.test.service.OracleOrganizationService;
import oracle.test.service.OracleRoleService;
import oracle.test.service.OracleUserService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import rabbit.open.orm.common.dml.FilterType;
import rabbit.open.orm.core.dml.meta.MultiDropFilter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


/**
 * <b>Description: 查询测试</b><br>
 * <b>@author</b> 肖乾斌
 * 
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:applicationContext-oracle.xml" })
public class OracleTest {

    @Autowired
    OracleUserService us;

    @Autowired
    OracleOrganizationService os;

    @Autowired
    OracleRoleService rs;
    
    @Test
    public void addBatch() {
		long count = us.createQuery().count();
		List<OUser> users = new ArrayList<>();
		TestCase.assertEquals(0, us.addBatch(users));
		int size = 1000;
		for (int i = 0; i < size; i++) {
			users.add(new OUser("lisi", i, new Date()));
		}
    	us.addBatch(users);
		TestCase.assertEquals(us.createQuery().count(), size + count);
	}


    /**
     * 
     * <b>Description: 分页 + 排序 + 关联(多对一、多对多)查询 + distinct </b><br>
     * 
     */
    @Test
    public void query() {
        OUser u = new OUser();
        u.setAge(10);
        u.setIndex("myindex");
        u.setBytes("hello oracle".getBytes());
        us.add(u);
        byte[] bytes = us.getByID(u.getId()).getBytes();
		TestCase.assertEquals("hello oracle", new String(bytes));
		
		// 包含blob字段时如果表太多时distinct会报错，"数据类型不一致: 应为 -, 但却获得 BLOB", 见鬼了
        List<OUser> list = us.createQuery().page(0, 10)
                .addFilter("index", u.getIndex()).joinFetch(ORole.class)
                .fetch(OracleOrganization.class).execute().list();
        list.forEach(us -> System.out.println(us));
        TestCase.assertTrue(list.size() > 0);
       
    }

	/**
	 * 增量测试
	 */
	@Test
	public void deltaUpdateTest() {
		OUser u = new OUser();
		int age = 1100;
		u.setAge(age);
		us.add(u);
		int delta = -10;
		us.createUpdate(u).deltaUpdate("age", delta).execute();
		OUser user = us.getByID(u.getId());
		TestCase.assertEquals(user.getAge().intValue(), age + delta);
	}


    @Test
    public void simpleQueryTest() {
        OUser user = addInitData(100);
        List<OUser> list = us.createQuery(user).joinFetch(ORole.class)
                .fetch(OracleOrganization.class).execute().list();
        TestCase.assertTrue(list.size() > 0);
    }

    @Test
    public void namedQueryTest() {
        OUser user = addInitData(111);
        OUser u = us.createNamedQuery("getUserByName")
                .set("username", "%zhangsan%", "name", OUser.class)
                .set("userId", user.getId(), "id", OUser.class).execute()
                .unique();
        System.out.println(u);
        TestCase.assertEquals(user.getName(), u.getName());
    }

    /**
     * 
     * <b>Description: 添加测试数据</b><br>
     * @param start
     * 
     */
    public OUser addInitData(int start) {
        OUser user = new OUser();
        // 添加组织
        OracleOrganization org = new OracleOrganization("FBI", "联邦调查局");
        os.add(org);

        // 添加角色
        List<ORole> roles = new ArrayList<ORole>();
        for (int i = start; i < start + 2; i++) {
            ORole r = new ORole("R" + i);
            rs.add(r);
            roles.add(r);
        }

        // 添加用户
        user.setOrg(org);
        user.setBigField(new BigDecimal(1));
        user.setShortField((short) 1);
        user.setDoubleField(0.1);
        user.setFloatField(0.1f);

        user.setName("zhangsan" + System.currentTimeMillis());
        user.setBirth(new Date());
        us.add(user);

        // 添加用户角色之间的映射关系
        user.setRoles(roles);
        us.addJoinRecords(user);

        return user;

    }
    
    /**
     * <b>Description or条件查询测试</b>
     */
    @Test
    public void multiDropFilterQueryTest() {
        OUser u1 = new OUser();
        u1.setAge(10);
        u1.setDesc("H1");
        us.add(u1);
        OUser u2 = new OUser();
        u2.setAge(10);
        u2.setDesc("H2");
        us.add(u2);
        List<OUser> list = us
                .createQuery()
                .addFilter(
                        new MultiDropFilter().on("id", u1.getId(),
                                FilterType.IN).on("desc", u2.getDesc()))
                .fetch(OracleOrganization.class).asc("id").execute().list();
        TestCase.assertEquals(2, list.size());
        TestCase.assertEquals(u1.getDesc(), list.get(0).getDesc());
        TestCase.assertEquals(u2.getDesc(), list.get(1).getDesc());
    }

    /**
     * <b>Description  更新测试</b>
     */
    @Test
    public void multiDropFilterUpdateTest() {
        OUser u1 = new OUser();
        u1.setAge(10);
        u1.setDesc("1H1");
        us.add(u1);
        OUser u2 = new OUser();
        u2.setAge(10);
        u2.setDesc("1H2");
        us.add(u2);

        OUser u3 = new OUser();
        u3.setAge(10);
        u3.setDesc("1H3");
        us.add(u3);

        String name = "zhangsan";
        us.createUpdate().set("name", name)
                .addMultiDropFilter(new MultiDropFilter()
                        .on("id", u1.getId(),FilterType.IN)
                        .on("desc", u2.getDesc()))
                .execute();

        List<OUser> list = us.createQuery()
                .addFilter(
                        new MultiDropFilter().on("id", new Long[]{u1.getId(),  u3.getId()},
                                FilterType.IN).on("desc", u2.getDesc()))
                .fetch(OracleOrganization.class).asc("id").execute().list();
        TestCase.assertEquals(3, list.size());
        TestCase.assertEquals(u1.getDesc(), list.get(0).getDesc());
        TestCase.assertEquals(u2.getDesc(), list.get(1).getDesc());
        //u3不在更新条件中，所以name应该为空
        TestCase.assertNull(list.get(2).getName());
        TestCase.assertEquals(list.get(0).getName(), name);
        TestCase.assertEquals(list.get(1).getName(), name);
    }
    
    @Test
	public void updateNullTest() {
		OUser user = new OUser();
		user.setAge(23);
		String name = "lxis";
		user.setName(name);
		String desc = "x1H1";
		user.setDesc(desc);
		us.add(user);
		
		OUser u = us.getByID(user.getId());
		TestCase.assertEquals(u.getName(), name);
		TestCase.assertEquals(u.getDesc(), desc);
		
		us.createUpdate().setNull("desc", "name").addFilter("id", u.getId()).execute();
		u = us.getByID(user.getId());
		
		TestCase.assertNull(u.getName());
		TestCase.assertNull(u.getDesc());
	}

    /**
     * <b>Description  删除</b>
     */
    @Test
    public void multiDropFilterDeleteTest() {
        OUser u1 = new OUser();
        u1.setAge(10);
        u1.setDesc("2H1");
        us.add(u1);
        OUser u2 = new OUser();
        u2.setAge(10);
        u2.setDesc("2H2");
        us.add(u2);
        
        OUser u3 = new OUser();
        u3.setAge(10);
        u3.setDesc("2H3");
        us.add(u3);
        
        //删除U1和U2
        us.createDelete().addMultiDropFilter(
                new MultiDropFilter().on("id", new Long[]{ u1.getId()},
                        FilterType.IN).on("desc", u2.getDesc())).execute();
        
        List<OUser> list = us.createQuery()
                .addFilter(
                        new MultiDropFilter().on("id", new Long[]{u1.getId(),  u3.getId()},
                                FilterType.IN).on("desc", u2.getDesc()))
                                .fetch(OracleOrganization.class).asc("id").execute().list();
        TestCase.assertEquals(1, list.size());
        TestCase.assertEquals(u3.getDesc(), list.get(0).getDesc());
    }

    /**
     * <b>Description  删除</b>
     */
    @Test
    public void multiDropFilterDeleteTest2() {
        OUser u1 = new OUser();
        u1.setAge(10);
        u1.setDesc("3H1");
        us.add(u1);
        OUser u2 = new OUser();
        u2.setAge(10);
        u2.setDesc("3H2");
        us.add(u2);
        
        OUser u3 = new OUser();
        u3.setAge(10);
        u3.setDesc("3H3");
        us.add(u3);
        
        //删除U1
        us.createDelete().addMultiDropFilter(new MultiDropFilter().on("id", new Long[]{u1.getId()},
                        FilterType.IN).on("desc", "h4"))
                    .addNullFilter("id", false)
                    .execute();
        
        List<OUser> list = us.createQuery()
                .addFilter("id", new Long[]{u1.getId(), u2.getId(), u3.getId()}, FilterType.IN)
                .asc("id").execute().list();
        TestCase.assertEquals(2, list.size());
        TestCase.assertEquals(u2.getDesc(), list.get(0).getDesc());
        TestCase.assertEquals(u3.getDesc(), list.get(1).getDesc());
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
    public void oracleCustomAddTest() {
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
}
