package rabbit.open.test;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import rabbit.open.test.entity.Organization;
import rabbit.open.test.entity.UUIDPolicyEntity;
import rabbit.open.test.entity.User;
import rabbit.open.test.service.UUIDEntityService;
import rabbit.open.test.service.UserService;

/**
 * <b>Description:   公共方法测试</b>.
 * <b>@author</b>    肖乾斌
 * 
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations={"classpath:applicationContext.xml"})
public class SpringDaoAdapterTest {

    @Autowired
    UUIDEntityService uues;
    
    UUIDPolicyEntity entity;
    
    @Autowired
    UserService us;

    @Before
    public void addInitData(){
        entity = new UUIDPolicyEntity();
        entity.setName("myent" + System.currentTimeMillis());
        entity.setId(null);
        uues.add(entity);
    }
    
    @Test
    public void testQuery(){
        List<UUIDPolicyEntity> entities = uues.query(0, 10);
        entities.forEach(u -> System.out.println(u));
    }
    
    @Test
    public void testdel(){
        uues.deleteByID("10xxx");
    }

    @Test
    public void queryByCondition(){
        List<UUIDPolicyEntity> entities = uues.query(entity, 0, 10);
        entities.forEach(u -> System.out.println(u));
    }
    
    @Test
    public void createNameMappedQuery(){
        NameMappedBean nmb = new NameMappedBean(1L, "zhangsan", 10);
        List<UUIDPolicyEntity> users = uues.createNameMappedQuery(nmb).execute().list();
        users.forEach(u -> System.out.println(u));
    }

    @Test
    public void createNameMappedQueryTest(){
        NameMappedBean nmb = new NameMappedBean(1L, "zhangsan", 10, new Organization("ORG", "X"));
        List<User> users = us.createNameMappedQuery(nmb).execute().list();
        users.forEach(u -> System.out.println(u));
    }
    
    @Test
    public void deleteByEntity(){
        System.out.println(uues.delete(entity));
    }

    @Test
    public void updateByID(){
        entity.setName("lisi");
        System.out.println(uues.updateByID(entity));
    }

    @Test
    public void testGetByID(){
        System.out.println(uues.getByID(entity.getId()));
    }
    
    public class NameMappedBean{
        
        private Long id;
        
        private String name;
        
        //年龄
        private Integer age;
        
        private Organization org;

        public NameMappedBean(Long id, String name, Integer age, Organization org) {
            super();
            this.id = id;
            this.name = name;
            this.age = age;
            this.org = org;
        }
        public NameMappedBean(Long id, String name, Integer age) {
            super();
            this.id = id;
            this.name = name;
            this.age = age;
        }

        public Long getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public Integer getAge() {
            return age;
        }
        public Organization getOrg() {
            return org;
        }
        
        
        
    }
}
