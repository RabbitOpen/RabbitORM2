package sharding.test;

import java.sql.Connection;
import java.sql.Statement;

import junit.framework.TestCase;

import org.apache.log4j.Logger;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import rabbit.open.orm.dialect.ddl.DDLHelper;
import rabbit.open.orm.dml.DMLAdapter;
import rabbit.open.orm.pool.SessionFactory;
import sharding.test.entity.Region;
import sharding.test.entity.ShardingUser;
import sharding.test.service.RegionService;
import sharding.test.service.ShardingUserService;

/**
 * <b>Description: 	分片表测试</b><br>
 * <b>@author</b>	肖乾斌
 * 
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations={"classpath:applicationContext.xml"})
public class ShardingTest {

    @Autowired
    ShardingUserService sus;
    
    @Autowired
    RegionService rs;
    
    Logger logger = Logger.getLogger(getClass());
    /**
     * <b>Description  新增 + 查询测试</b>
     */
    @Test
    public void shardingQueryTest(){
        reCreateTable("T_SHARD_USER0");
        reCreateTable("T_SHARD_USER1");
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
    
    @Test
    public void updateByIDTest() {
        reCreateTable("T_SHARD_USER1");
        ShardingUser user1 = addUser(13L, "zhangsan");
        user1.setName("zhang33");
        sus.updateByID(user1);
        ShardingUser u1 = sus.getByID(user1.getId());
        TestCase.assertEquals(user1.getId(), u1.getId());
        TestCase.assertEquals(user1.getName(), u1.getName());
    }

    /**
     * <b>Description  普通更新测试</b>
     */
    @Test
    public void updateTest() {
        reCreateTable("T_SHARD_USER1");
        ShardingUser user = addUser(15L, "zhangsan");
        String g = "male";
        long age = 10L;
        sus.createUpdate(user).set("age", age).set("gender", g).execute();
        ShardingUser u = sus.getByID(user.getId());
        TestCase.assertEquals(user.getId(), u.getId());
        TestCase.assertEquals(user.getName(), u.getName());
        TestCase.assertEquals(u.getGender(), g);
        TestCase.assertTrue(u.getAge() == age);
        String g2 = "female";
        long age2 = 12;
        sus.createUpdate().set("age", age2).set("gender", g2)
            .addFilter("id", 15L)
            .addFilter("name", "zhangsan")
            .execute();
        u = sus.getByID(user.getId());
        TestCase.assertTrue(age2 == u.getAge());
        TestCase.assertEquals(u.getGender(), g2);
    }

    /**
     * <b>Description  复杂更新测试</b>
     */
    @Test
    public void complexUpdateTest() {
        reCreateTable("T_SHARD_USER0");
        Region r = new Region();
        r.setId("id-r1");
        r.setName("r1-name");
        rs.add(r);
        ShardingUser user = addUser(20L, "lyixhuxomixn", r);
        String g = "male";
        long age = 10L;
        sus.createUpdate(user).set("age", age).set("gender", g).execute();
        ShardingUser u = sus.getByID(user.getId());
        TestCase.assertEquals(user.getId(), u.getId());
        TestCase.assertEquals(user.getName(), u.getName());
        TestCase.assertEquals(u.getGender(), g);
        TestCase.assertTrue(u.getAge() == age);
        
        g = "malex";
        age = 101L;
        sus.createUpdate().set("age", age).set("gender", g)
            .addFilter("id", user.getId())
            .addFilter("region", r.getId())
            .execute();
        u = sus.createQuery().addFilter("id", user.getId())
            .fetch(Region.class)
            .execute().unique();
        TestCase.assertEquals(user.getId(), u.getId());
        TestCase.assertEquals(user.getName(), u.getName());
        TestCase.assertEquals(u.getGender(), g);
        TestCase.assertTrue(u.getAge() == age);
        TestCase.assertEquals(u.getRegion().getId(), r.getId());
        TestCase.assertEquals(u.getRegion().getName(), r.getName());
    }
    
    private void reCreateTable(String tableName) {
        dropShardingTable(tableName, sus.getFactory());
        DDLHelper.addShardingTable(sus.getFactory(), tableName, ShardingUser.class);
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
            logger.error(e.getMessage());
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

    private ShardingUser addUser(long id, String name, Region r) {
        ShardingUser su = new ShardingUser();
        su.setId(id);
        su.setName(name);
        su.setRegion(r);
        sus.add(su);
        return su;
    }

}
