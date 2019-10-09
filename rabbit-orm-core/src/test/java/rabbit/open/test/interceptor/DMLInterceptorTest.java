package rabbit.open.test.interceptor;

import junit.framework.TestCase;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import rabbit.open.orm.core.dml.Query;
import rabbit.open.test.entity.User;
import rabbit.open.test.service.UserService;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:applicationContext.xml" })
public class DMLInterceptorTest {

    @Autowired
    UserService us;
    
    //自定义的过滤器，在DML操作前对User对象中的name字段加密，在查询出以后对该字段解密
    SuffixDMLInterceptor interceptor = new SuffixDMLInterceptor();
    
    @Test
    public void encryptTest() {
        //set filter
        us.getFactory().setInterceptor(interceptor);
        User u = new User();
        u.setName("lucy");
        u.setAge(100);
        us.add(u);
        Query<User> query = us.createQuery();
        
        //默认允许get操作时执行onValueGetted操作
        User uq = query.addFilter("id", u.getId()).unique();
        TestCase.assertEquals(uq.getName(), u.getName());
        TestCase.assertEquals(uq.getAge(), u.getAge());
        
        //禁止get操作时执行onValueGetted操作
        uq = query.disableGetFilter().unique();
        TestCase.assertEquals(uq.getName(), u.getName() + SuffixDMLInterceptor.suffix);
        TestCase.assertEquals(uq.getAge(), u.getAge());
        
        //允许get操作时执行onValueGetted操作
        uq = query.enableGetFilter().unique();
        TestCase.assertEquals(uq.getName(), u.getName());
        TestCase.assertEquals(uq.getAge(), u.getAge());
        
        us.getFactory().setInterceptor(null);
        
    }

    @Test
    public void encryptQueryTest() {
        //set filter
        us.getFactory().setInterceptor(interceptor);
        User u = new User();
        u.setName("lili");
        u.setAge(100);
        us.add(u);
        Query<User> query = us.createQuery();
        
        //默认允许get操作时执行onValueGetted操作
        User uq = query.addFilter("id", u.getId()).addFilter("name", u.getName()).unique();
        TestCase.assertEquals(uq.getName(), u.getName());
        TestCase.assertEquals(uq.getAge(), u.getAge());
        
        //禁止get操作时执行onValueGetted操作
        uq = query.disableGetFilter().unique();
        TestCase.assertEquals(uq.getName(), u.getName() + SuffixDMLInterceptor.suffix);
        TestCase.assertEquals(uq.getAge(), u.getAge());
        
        us.getFactory().setInterceptor(null);
    }
    
    
}
