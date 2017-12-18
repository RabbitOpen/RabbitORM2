package rabbit.open.test;

import junit.framework.TestCase;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import rabbit.open.test.service.UserService;

/**
 * <b>Description:   事务方法测试</b>.
 * <b>@author</b>    肖乾斌
 * 
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations={"classpath:applicationContext.xml"})
public class TransactionTest {

    @Autowired
    UserService us;
    
    /**
     * 
     * <b>Description: 回滚测试 </b><br>.	
     * 
     */
    @Test
    public void rollBackTest(){
        long before = us.createQuery().count();
        try{
            us.rollBakcTest();
            TestCase.assertTrue(false);
        }catch(Exception e){
            TestCase.assertEquals(before, us.createQuery().count());
        }
    }

    /**
     * 
     * <b>Description: spring事务测试 </b><br>.	
     * 
     */
    @Test
    @Transactional
    @Rollback(false)
    public void springTransactionTest(){
        long before = us.createQuery().count();
        us.springTransactionTest();
        TestCase.assertEquals(before + 2, us.createQuery().count());
    }
}
