package oracle.test;

import junit.framework.TestCase;
import oracle.test.entity.ORegion;
import oracle.test.entity.OShardingUser;
import oracle.test.service.OracleRegionService;
import oracle.test.service.OracleShardingUserService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import rabbit.open.orm.core.dialect.ddl.DDLHelper;
import rabbit.open.orm.core.dml.DMLObject;

import java.sql.Connection;
import java.sql.Statement;

/**
 * <b>Description: 分片表测试</b><br>
 * <b>@author</b> 肖乾斌
 * 
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:applicationContext-oracle.xml" })
public class OracleShardingTest {

    @Autowired
    OracleShardingUserService sus;

    @Autowired
    OracleRegionService rs;

    Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * <b>Description 新增 + 查询测试</b>
     */
    @Test
    public void shardingQueryTest() {
        reCreateTable("TO_SHARD_USER0");
        reCreateTable("TO_SHARD_USER1");
        addUser(8L, "zhangsan");
        OShardingUser user1 = addUser(10L, "zhangsan");
        OShardingUser user2 = addUser(11L, "lis");
        OShardingUser u1 = sus.createQuery().addFilter("id", user1.getId())
                .execute().unique();
        OShardingUser u2 = sus.createQuery().addFilter("id", user2.getId())
                .execute().unique();
        TestCase.assertEquals(user1.getId(), u1.getId());
        TestCase.assertEquals(user1.getName(), u1.getName());
        TestCase.assertEquals(user2.getId(), u2.getId());
        TestCase.assertEquals(user2.getName(), u2.getName());

        TestCase.assertEquals(sus.createQuery().addFilter("id", 11L).count(), 1);
    }

    /**
     * <b>Description 根据id删除分区表中的数据</b>
     */
    @Test
    public void deleteByIDTest() {
        reCreateTable("TO_SHARD_USER1");
        OShardingUser user = addUser(113L, "zhangsan");
        TestCase.assertNotNull(sus.getByID(user.getId()));
        sus.deleteByID(user.getId());
        TestCase.assertNull(sus.getByID(user.getId()));
    }

    /**
     * <b>Description 动态条件删除测试</b>
     */
    @Test
    public void deleteTest() {
        reCreateTable("TO_SHARD_USER1");
        OShardingUser user = addUser(115L, "zhangsan");
        sus.createDelete(user).execute();
        TestCase.assertNull(sus.getByID(user.getId()));

        user = addUser(117L, "zhangsan");
        sus.createDelete(user).addFilter("name", user.getName()).execute();
        TestCase.assertNull(sus.getByID(user.getId()));

        user = addUser(119L, "zxx");
        sus.createDelete().addFilter("id", user.getId())
                .addFilter("name", user.getName()).execute();
        TestCase.assertNull(sus.getByID(user.getId()));

        ORegion r = new ORegion();
        r.setId("r33");
        r.setName("r1-name");
        rs.add(r);
        user = addUser(121, "lyixhuxomixn", r);
        sus.createDelete().addFilter("id", user.getId())
                .addFilter("name", user.getName())
                .addFilter("region", r.getId())
                .addFilter("id", r.getId(), ORegion.class)
                .addFilter("name", r.getName(), ORegion.class).execute();
        TestCase.assertNull(sus.getByID(user.getId()));

        user = addUser(123, "lyixhuxomixn", r);
        sus.createDelete(user).execute();
        TestCase.assertNull(sus.getByID(user.getId()));

        user = addUser(125, "lyixhuxomixn", r);
        sus.createDelete(user).addFilter("id", user.getId()).execute();
        TestCase.assertNull(sus.getByID(user.getId()));
    }

    @Test
    public void updateByIDTest() {
        reCreateTable("TO_SHARD_USER1");
        OShardingUser user1 = addUser(13L, "zhangsan");
        user1.setName("zhang33");
        sus.updateByID(user1);
        OShardingUser u1 = sus.getByID(user1.getId());
        TestCase.assertEquals(user1.getId(), u1.getId());
        TestCase.assertEquals(user1.getName(), u1.getName());
    }

    /**
     * <b>Description 普通更新测试</b>
     */
    @Test
    public void updateTest() {
        reCreateTable("TO_SHARD_USER1");
        OShardingUser user = addUser(15L, "zhangsan");
        String g = "male";
        long age = 10L;
        sus.createUpdate(user).set("age", age).set("gender", g).execute();
        OShardingUser u = sus.getByID(user.getId());
        TestCase.assertEquals(user.getId(), u.getId());
        TestCase.assertEquals(user.getName(), u.getName());
        TestCase.assertEquals(u.getGender(), g);
        TestCase.assertTrue(u.getAge() == age);
        String g2 = "female";
        long age2 = 12;
        sus.createUpdate().set("age", age2).set("gender", g2)
                .addFilter("id", 15L).addFilter("name", "zhangsan").execute();
        u = sus.getByID(user.getId());
        TestCase.assertTrue(age2 == u.getAge());
        TestCase.assertEquals(u.getGender(), g2);
    }

    /**
     * <b>Description 复杂更新测试</b>
     */
    @Test
    public void complexUpdateTest() {
        reCreateTable("TO_SHARD_USER0");
        ORegion r = new ORegion();
        r.setId("id-r1");
        r.setName("r1-name");
        rs.add(r);
        OShardingUser user = addUser(20L, "lyixhuxomixn", r);
        String g = "male";
        long age = 10L;
        sus.createUpdate(user).set("age", age).set("gender", g).execute();
        OShardingUser u = sus.getByID(user.getId());
        TestCase.assertEquals(user.getId(), u.getId());
        TestCase.assertEquals(user.getName(), u.getName());
        TestCase.assertEquals(u.getGender(), g);
        TestCase.assertTrue(u.getAge() == age);
        g = "malex";
        age = 101L;
        sus.createUpdate().set("age", age).set("gender", g)
                .addFilter("id", user.getId()).addFilter("region", r.getId())
                .addFilter("name", r.getName(), ORegion.class).execute();
        u = sus.createQuery().addFilter("id", user.getId()).fetch(ORegion.class)
                .execute().unique();
        TestCase.assertEquals(user.getId(), u.getId());
        TestCase.assertEquals(user.getName(), u.getName());
        TestCase.assertEquals(u.getGender(), g);
        TestCase.assertTrue(u.getAge() == age);
        TestCase.assertEquals(u.getRegion().getId(), r.getId());
        TestCase.assertEquals(u.getRegion().getName(), r.getName());
    }

    /**
     * <b>Description 复杂查询测试</b>
     */
    @Test
    public void complexQueryTest() {
        reCreateTable("TO_SHARD_USER0");
        ORegion r = new ORegion();
        String regionId = "id-r2";
        String regionName = "r1-name";
        r.setId(regionId);
        r.setName(regionName);
        rs.add(r);
        OShardingUser user = addUser(22L, "lyixhuxomixn", r);
        OShardingUser u = sus.createQuery().addFilter("id", user.getId())
                .addFilter("name", regionName, ORegion.class)
                .fetch(ORegion.class).execute().unique();
        TestCase.assertEquals(user.getId(), u.getId());
        TestCase.assertEquals(user.getName(), u.getName());
        TestCase.assertEquals(u.getRegion().getId(), r.getId());
        TestCase.assertEquals(u.getRegion().getName(), r.getName());
    }

    private void reCreateTable(String tableName) {
        Connection connection = null;
		try {
			connection = sus.getFactory().getConnection();
			dropShardingTable(tableName, connection);
			DDLHelper.createTable(connection, sus.getFactory().getDialectType(), tableName, OShardingUser.class);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		} finally {
			DMLObject.closeConnection(connection);
		}
    }
    
    /**
     * <b>Description 删除分片表</b>
     * @param tableName
     * @param connection
     */
    private void dropShardingTable(String tableName, Connection connection) {
        Statement stmt = null;
        try {
            stmt = connection.createStatement();
            stmt.execute("drop table " + tableName);
            stmt.close();
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
    }

    private OShardingUser addUser(long id, String name) {
        OShardingUser su = new OShardingUser();
        su.setId(id);
        su.setName(name);
        sus.add(su);
        return su;
    }

    private OShardingUser addUser(long id, String name, ORegion r) {
        OShardingUser su = new OShardingUser();
        su.setId(id);
        su.setName(name);
        su.setRegion(r);
        sus.add(su);
        return su;
    }

}
