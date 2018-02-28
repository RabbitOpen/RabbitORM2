package sharding.test;

import java.sql.Connection;
import java.sql.Statement;

import junit.framework.TestCase;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import rabbit.open.orm.dml.DMLAdapter;
import rabbit.open.orm.exception.RabbitDDLException;
import rabbit.open.orm.pool.SessionFactory;
import sharding.test.entity.ShardingUser;
import sharding.test.service.ShardingUserService;

/**
 * <b>Description: 	切片表测试</b><br>
 * <b>@author</b>	肖乾斌
 * 
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations={"classpath:applicationContext.xml"})
public class ShardingTest {

    @Autowired
    ShardingUserService sus;
    
    /**
     * <b>Description  新增 + 查询测试</b>
     */
    @Test
    public void shardingQueryTest(){
        dropShardingTable("T_SHARD_USER0", sus.getFactory());
        dropShardingTable("T_SHARD_USER1", sus.getFactory());
        addUser(8L, "zhangsan");
        ShardingUser user1 = addUser(10L, "zhangsan");
        ShardingUser user2 = addUser(11L, "lis");
        ShardingUser u1 = sus.createQuery().addFilter("id", user1.getId()).execute().unique();
        ShardingUser u2 = sus.createQuery().addFilter("id", user2.getId()).execute().unique();
        TestCase.assertEquals(user1.getId(), u1.getId());
        TestCase.assertEquals(user1.getName(), u1.getName());
        TestCase.assertEquals(user2.getId(), u2.getId());
        TestCase.assertEquals(user2.getName(), u2.getName());
    }
    
    /**
     * <b>Description  删除分片表</b>
     * @param tableName
     * @param factory
     */
    private void dropShardingTable(String tableName, SessionFactory factory) {
        Connection connection = null;
        Statement stmt = null;
        try {
            connection = factory.getConnection();
            stmt = connection.createStatement();
            stmt.execute("drop table " + tableName);
            stmt.close();
        } catch (Exception e) {
            throw new RabbitDDLException(e);
        } finally {
            DMLAdapter.closeConnection(connection);
        }
    }

    private ShardingUser addUser(long id, String name) {
        ShardingUser su = new ShardingUser();
        su.setId(id);
        su.setName(name);
        sus.add(su);
        return su;
    }

}
