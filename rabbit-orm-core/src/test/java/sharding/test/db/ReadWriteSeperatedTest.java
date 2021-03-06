package sharding.test.db;

import junit.framework.TestCase;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;
import rabbit.open.orm.common.dml.DMLType;
import rabbit.open.orm.common.exception.RabbitDMLException;
import rabbit.open.orm.core.annotation.Entity;
import sharding.test.db.entity.RWUser;
import sharding.test.db.service.RWUserService;
import sharding.test.db.service.WriteOnlyUserService;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * <b>Description 读写分离测试</b>
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:read-write-seperated-context.xml" })
public class ReadWriteSeperatedTest {

    @Autowired
    RWUserService rwUserService;

    @Autowired
    WriteOnlyUserService wus;

    Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * <b>Description 初始化表</b>
     */
    @Before
    public void init() {
        try {
            Connection read = rwUserService.getFactory().getConnection(null,
                    null, DMLType.SELECT);
            reCreateTable(RWUser.class.getAnnotation(Entity.class).value(),
                    read);
            read.close();
        } catch (SQLException e) {
            logger.error(e.getMessage());
        }
        try {
            Connection write = rwUserService.getFactory().getConnection(null,
                    null, DMLType.INSERT);
            reCreateTable(RWUser.class.getAnnotation(Entity.class).value(),
                    write);
            write.close();
        } catch (SQLException e) {
            logger.error(e.getMessage());
        }
    }

    /**
     * <b>Description 读写分离测试</b>
     */
    @Test
    public void readWriteSplitedTest() {
        RWUser user = new RWUser("zhangsan", 10);
        rwUserService.add(user);
        // 读写分离后，读库中是没有数据的
        TestCase.assertNull(rwUserService.getByID(user.getId()));

        // 直接查询写库
        RWUser u = wus.getByID(user.getId());
        TestCase.assertEquals(u.getName(), user.getName());
        TestCase.assertEquals(u.getId(), user.getId());
        TestCase.assertEquals(u.getAge(), user.getAge());
    }
    
    @Test
    @Transactional
    public void tranReadWriteSplitedTest() {
    	RWUser user = new RWUser("zhangsan", 10);
        rwUserService.add(user);
        // 读写分离后，如果有事务，直接操作读库读库中是没有数据的
        TestCase.assertNotNull(rwUserService.getByID(user.getId()));

        // 直接查询写库
        RWUser u = wus.getByID(user.getId());
        TestCase.assertEquals(u.getName(), user.getName());
        TestCase.assertEquals(u.getId(), user.getId());
        TestCase.assertEquals(u.getAge(), user.getAge());
    }

    @Test
    public void transactionTest() {
        rwUserService.doTransaction();
        TestCase.assertEquals(0, rwUserService.createQuery().count());
        TestCase.assertEquals(2, wus.createQuery().count());
        try {
            rwUserService.doRollBack();
            throw new RuntimeException();
        } catch (Exception e) {
            TestCase.assertEquals(e.getClass(), RabbitDMLException.class);
            TestCase.assertEquals(0, rwUserService.createQuery().count());
            TestCase.assertEquals(2, wus.createQuery().count());
        }
    }

    /**
     * <b>Description 删除表</b>
     */
    private void reCreateTable(String tableName, Connection connection) {
        Statement stmt = null;
        try {
            stmt = connection.createStatement();
            stmt.execute("drop table " + tableName);
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
        try {
            stmt.execute("CREATE TABLE " + tableName + "(ID BIGINT  NOT NULL AUTO_INCREMENT,NAME VARCHAR(50), AGE BIGINT, PRIMARY KEY (ID)) ");
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
        try {
            stmt.close();
        } catch (SQLException e) {
            logger.error(e.getMessage());
        }
    }
}
