package rabbit.open.test;

import java.util.List;

import junit.framework.TestCase;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import rabbit.open.orm.annotation.FilterType;
import rabbit.open.orm.dml.meta.MultiDropFilter;
import rabbit.open.test.entity.Organization;
import rabbit.open.test.entity.User;
import rabbit.open.test.service.UserService;

/**
 * <b>Description OR 条件查询</b>
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:applicationContext.xml" })
public class MultiDropFilterTest {

    @Autowired
    UserService us;

    /**
     * <b>Description or条件查询测试</b>
     */
    @Test
    public void multiDropFilterQueryTest() {
        User u1 = new User();
        u1.setAge(10);
        u1.setDesc("H1");
        us.add(u1);
        User u2 = new User();
        u2.setAge(10);
        u2.setDesc("H2");
        us.add(u2);
        List<User> list = us
                .createQuery()
                .addMultiDropFilter(
                        new MultiDropFilter().on("id", u1.getId(),
                                FilterType.IN).on("desc", u2.getDesc()))
                .fetch(Organization.class).asc("id").execute().list();
        TestCase.assertEquals(2, list.size());
        TestCase.assertEquals(u1.getDesc(), list.get(0).getDesc());
        TestCase.assertEquals(u2.getDesc(), list.get(1).getDesc());
    }

    /**
     * <b>Description  更新测试</b>
     */
    @Test
    public void multiDropFilterUpdateTest() {
        User u1 = new User();
        u1.setAge(10);
        u1.setDesc("1H1");
        us.add(u1);
        User u2 = new User();
        u2.setAge(10);
        u2.setDesc("1H2");
        us.add(u2);

        User u3 = new User();
        u3.setAge(10);
        u3.setDesc("1H3");
        us.add(u3);

        String name = "zhangsan";
        us.createUpdate().set("name", name)
                .addMultiDropFilter(new MultiDropFilter()
                        .on("id", u1.getId(),FilterType.IN)
                        .on("desc", u2.getDesc()))
                .execute();

        List<User> list = us.createQuery()
                .addMultiDropFilter(
                        new MultiDropFilter().on("id", new Long[]{u1.getId(),  u3.getId()},
                                FilterType.IN).on("desc", u2.getDesc()))
                .fetch(Organization.class).asc("id").execute().list();
        TestCase.assertEquals(3, list.size());
        TestCase.assertEquals(u1.getDesc(), list.get(0).getDesc());
        TestCase.assertEquals(u2.getDesc(), list.get(1).getDesc());
        //u3不在更新条件中，所以name应该为空
        TestCase.assertNull(list.get(2).getName());
        TestCase.assertEquals(list.get(0).getName(), name);
        TestCase.assertEquals(list.get(1).getName(), name);
    }
    

    /**
     * <b>Description  删除</b>
     */
    @Test
    public void multiDropFilterDeleteTest() {
        User u1 = new User();
        u1.setAge(10);
        u1.setDesc("2H1");
        us.add(u1);
        User u2 = new User();
        u2.setAge(10);
        u2.setDesc("2H2");
        us.add(u2);
        
        User u3 = new User();
        u3.setAge(10);
        u3.setDesc("2H3");
        us.add(u3);
        
        //删除U1和U2
        us.createDelete().addMultiDropFilter(new MultiDropFilter().on("id", new Long[]{ u1.getId()}, FilterType.IN)
        		.on("desc", u2.getDesc())).addMultiDropFilter(new MultiDropFilter().on("id", new Long[]{ u1.getId()}, FilterType.IN)
        		.on("desc", u2.getDesc()))
        		.execute();
        
        List<User> list = us.createQuery()
                .addMultiDropFilter(new MultiDropFilter().on("id", new Long[]{u1.getId(),  u3.getId()},
                                FilterType.IN).on("desc", u2.getDesc()))
                .addMultiDropFilter(new MultiDropFilter().on("id", new Long[]{u1.getId(),  u3.getId()},
                		FilterType.IN).on("desc", u2.getDesc()))
                                .fetch(Organization.class).asc("id").execute().list();
        TestCase.assertEquals(1, list.size());
        TestCase.assertEquals(u3.getDesc(), list.get(0).getDesc());
    }

    /**
     * <b>Description  删除</b>
     */
    @Test
    public void multiDropFilterDeleteTest2() {
        User u1 = new User();
        u1.setAge(10);
        u1.setDesc("3H1");
        us.add(u1);
        User u2 = new User();
        u2.setAge(10);
        u2.setDesc("3H2");
        us.add(u2);
        
        User u3 = new User();
        u3.setAge(10);
        u3.setDesc("3H3");
        us.add(u3);
        
        //删除U1
        us.createDelete().addMultiDropFilter(new MultiDropFilter().on("id", new Long[]{u1.getId()},
                        FilterType.IN).on("desc", "h4"))
                    .addNullFilter("id", false)
                    .execute();
        
        List<User> list = us.createQuery()
                .addFilter("id", new Long[]{u1.getId(), u2.getId(), u3.getId()}, FilterType.IN)
                .asc("id").execute().list();
        TestCase.assertEquals(2, list.size());
        TestCase.assertEquals(u2.getDesc(), list.get(0).getDesc());
        TestCase.assertEquals(u3.getDesc(), list.get(1).getDesc());
    }
    
}
