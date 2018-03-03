package rabbit.open.test;

import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

import org.apache.log4j.Logger;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.AfterTransaction;
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
    
    Map<String, Long> counter = new HashMap<>();
    
    Logger logger = Logger.getLogger(getClass());
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
     * <b>Description: 注解回滚事务测试 </b><br>.	
     * 
     */
    @Test
    @Transactional
    @Rollback(false)
    public void annotationRollbackTest(){
        counter.put("annotationRollbackTest", us.createQuery().count());
        us.springTransactionTest();
    }
    
    /**
     * <b>Description  事务执行完后执行的验证代码</b>
     */
    @AfterTransaction()
    public void transactionAssert(){
        if(counter.containsKey("springTransactionTest")){
            TestCase.assertEquals(counter.get("springTransactionTest") + 2, 
                    us.createQuery().count());
        }
        if(counter.containsKey("annotationRollbackTest2")){
            TestCase.assertEquals(counter.get("annotationRollbackTest2").intValue(), 
                    us.createQuery().count());
        }
        if(counter.containsKey("readOnlyTest")){
            TestCase.assertEquals(counter.get("readOnlyTest").intValue() + 2, 
                    us.createQuery().count());
        }
        if(counter.containsKey("readOnlyTest2")){
            TestCase.assertEquals(counter.get("readOnlyTest2").intValue() + 2, 
                    us.createQuery().count());
        }
    }

    /**
     * 
     * <b>Description: 注解回滚事务测试 </b><br>.	
     * 
     */
    @Test
    @Transactional
    @Rollback(true)
    public void annotationRollbackTest2(){
        counter.put("annotationRollbackTest2", us.createQuery().count());
        us.springTransactionTest();
    }

    /**
     * 
     * <b>Description: Rollback(true)失效测试 </b><br>.	
     * 
     */
    @Test
    @Transactional(readOnly=true)
    @Rollback(true)
    public void readOnlyTest(){
        counter.put("readOnlyTest", us.createQuery().count());
        us.springTransactionTest();
    }

    /**
     * 
     * <b>Description: Rollback(false)失效测试 </b><br>.	
     * 
     */
    @Test
    @Transactional(readOnly=true)
    @Rollback(false)
    public void readOnlyTest2(){
        counter.put("readOnlyTest2", us.createQuery().count());
        us.springTransactionTest();
    }
    
    /**
     * <b>Description  空任务事务，什么都不做</b>
     */
    @Test
    @Transactional
    @Rollback(false)
    public void emptyTransactionTestCommit() {
        
    }

    /**
     * <b>Description  空任务事务，什么都不做</b>
     */
    @Test
    @Transactional
    public void emptyTransactionTestRollBack() {
        
    }
    
}
