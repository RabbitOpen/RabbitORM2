package sharding.test.table;

import java.sql.Connection;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

import org.apache.log4j.Logger;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import rabbit.open.orm.dialect.ddl.DDLHelper;
import rabbit.open.orm.dml.DMLAdapter;
import rabbit.open.orm.exception.FetchShardEntityException;
import rabbit.open.orm.pool.SessionFactory;
import sharding.test.table.entity.Region;
import sharding.test.table.entity.ShardCar;
import sharding.test.table.entity.ShardDept;
import sharding.test.table.entity.ShardRoom;
import sharding.test.table.entity.ShardingUser;
import sharding.test.table.service.RegionService;
import sharding.test.table.service.ShardCarService;
import sharding.test.table.service.ShardDeptService;
import sharding.test.table.service.ShardRoomService;
import sharding.test.table.service.ShardingUserService;

/**
 * <b>Description: 分片表测试</b><br>
 * <b>@author</b> 肖乾斌
 * 
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:applicationContext.xml" })
public class ShardingTableTest {

    @Autowired
    ShardingUserService sus;

    @Autowired
    ShardDeptService sds;

    @Autowired
    RegionService rs;
    
    @Autowired
    ShardCarService scs;
    
    @Autowired
    ShardRoomService srs;

    Logger logger = Logger.getLogger(getClass());

    /**
     * <b>Description 新增 + 查询测试</b>
     */
    @Test
    public void shardingQueryTest() {
        reCreateTable("T_SHARD_USER0");
        reCreateTable("T_SHARD_USER1");
        addUser(8L, "zhangsan");
        ShardingUser user1 = addUser(10L, "zhangsan");
        ShardingUser user2 = addUser(11L, "lis");
        ShardingUser u1 = sus.createQuery().addFilter("id", user1.getId())
                .execute().unique();
        ShardingUser u2 = sus.createQuery().addFilter("id", user2.getId())
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
        reCreateTable("T_SHARD_USER1");
        ShardingUser user = addUser(113L, "zhangsan");
        TestCase.assertNotNull(sus.getByID(user.getId()));
        sus.deleteByID(user.getId());
        TestCase.assertNull(sus.getByID(user.getId()));
    }

    /**
     * <b>Description 动态条件删除测试</b>
     */
    @Test
    public void deleteTest() {
        reCreateTable("T_SHARD_USER1");
        ShardingUser user = addUser(115L, "zhangsan");
        sus.createDelete(user).execute();
        TestCase.assertNull(sus.getByID(user.getId()));

        user = addUser(117L, "zhangsan");
        sus.createDelete(user).addFilter("name", user.getName()).execute();
        TestCase.assertNull(sus.getByID(user.getId()));

        user = addUser(119L, "zxx");
        sus.createDelete().addFilter("id", user.getId())
                .addFilter("name", user.getName()).execute();
        TestCase.assertNull(sus.getByID(user.getId()));

        Region r = new Region();
        r.setId("r33");
        r.setName("r1-name");
        rs.add(r);
        user = addUser(121, "lyixhuxomixn", r);
        sus.createDelete().addFilter("id", user.getId())
                .addFilter("name", user.getName())
                .addFilter("region", r.getId())
                .addFilter("id", r.getId(), Region.class)
                .addFilter("name", r.getName(), Region.class).execute();
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
        reCreateTable("T_SHARD_USER1");
        ShardingUser user1 = addUser(13L, "zhangsan");
        user1.setName("zhang33");
        sus.updateByID(user1);
        ShardingUser u1 = sus.getByID(user1.getId());
        TestCase.assertEquals(user1.getId(), u1.getId());
        TestCase.assertEquals(user1.getName(), u1.getName());
    }

    /**
     * <b>Description 普通更新测试</b>
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
                .addFilter("id", user.getId()).addFilter("region", r.getId())
                .addFilter("name", r.getName(), Region.class).execute();
        u = sus.createQuery().addFilter("id", user.getId()).fetch(Region.class)
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
        reCreateTable("T_SHARD_USER0");
        Region r = new Region();
        String regionId = "id-r2";
        String regionName = "r1-name";
        r.setId(regionId);
        r.setName(regionName);
        rs.add(r);
        ShardingUser user = addUser(22L, "lyixhuxomixn", r);
        ShardingUser u = sus.createQuery().addFilter("id", user.getId())
                .addFilter("name", regionName, Region.class)
                .fetch(Region.class).execute().unique();
        TestCase.assertEquals(user.getId(), u.getId());
        TestCase.assertEquals(user.getName(), u.getName());
        TestCase.assertEquals(u.getRegion().getId(), r.getId());
        TestCase.assertEquals(u.getRegion().getName(), r.getName());
        
        u = sus.createQuery().addFilter("id", user.getId())
                .addFilter("name", regionName, Region.class)
                .buildFetch().fetch(Region.class).build().execute().unique();
        TestCase.assertEquals(user.getId(), u.getId());
        TestCase.assertEquals(user.getName(), u.getName());
        TestCase.assertEquals(u.getRegion().getId(), r.getId());
        TestCase.assertEquals(u.getRegion().getName(), r.getName());
    }
    
    /**
     * <b>Description  一对多/多对多 查询测试</b>
     */
    @Test
    public void joinFetchTest() {
        
        //=========构造测试数据=============
        reCreateTable("T_SHARD_USER0");
        Region r = new Region();
        String regionId = "myregionID";
        String regionName = "myregion";
        r.setId(regionId);
        r.setName(regionName);
        rs.add(r);
        ShardingUser user = addUser(2022L, "user2022", r);
        ShardCar sc1 = new ShardCar("sc1", "川A7566F", user.getId());
        scs.add(sc1);
        ShardCar sc2 = new ShardCar("sc2", "川B7566F", user.getId());
        scs.add(sc2);
        List<ShardCar> cars = new ArrayList<>();
        cars.add(sc1);
        cars.add(sc2);
        
        ShardRoom sr1 = new ShardRoom("sr1", "406");
        ShardRoom sr2 = new ShardRoom("sr2", "407");
        srs.add(sr1);
        srs.add(sr2);
        
        List<ShardRoom> rooms = new ArrayList<>();
        rooms.add(sr1);
        rooms.add(sr2);
        user.setRooms(rooms);
        sus.addJoinRecords(user);
        
        //test
        ShardingUser su = sus.createQuery().addFilter("id", user.getId())
                .fetch(Region.class)   
                .joinFetch(ShardCar.class)
                .joinFetch(ShardRoom.class)
                .execute().unique();
        TestCase.assertEquals(su.getRegion().getName(), user.getRegion().getName());
        TestCase.assertEquals(su.getRooms().size(), user.getRooms().size());
        TestCase.assertEquals(su.getCars().size(), cars.size());

        su = sus.createQuery().addFilter("id", user.getId())
                .fetch(Region.class)   
                .buildFetch().joinFetch(ShardCar.class).on("id", sc1.getId()).build()
                .joinFetch(ShardRoom.class)
                .execute().unique();
        TestCase.assertEquals(su.getRegion().getName(), user.getRegion().getName());
        TestCase.assertEquals(su.getRooms().size(), user.getRooms().size());
        TestCase.assertEquals(su.getCars().size(), 1);
        TestCase.assertEquals(su.getCars().get(0).getCarNo(), sc1.getCarNo());

        su = sus.createQuery().addFilter("id", user.getId())
                .fetch(Region.class)   
                .buildFetch().joinFetch(ShardCar.class).on("id", sc1.getId()).build()
                .buildFetch().joinFetch(ShardRoom.class).on("id", sr1.getId()).build()
                .execute().unique();
        TestCase.assertEquals(su.getRegion().getName(), user.getRegion().getName());
        TestCase.assertEquals(su.getRooms().size(), 1);
        TestCase.assertEquals(su.getRooms().get(0).getRoomNo(), sr1.getRoomNo());
        TestCase.assertEquals(su.getCars().size(), 1);
        TestCase.assertEquals(su.getCars().get(0).getCarNo(), sc1.getCarNo());

        su = sus.createQuery().addFilter("id", user.getId())
                .fetch(Region.class)   
                .buildFetch().joinFetch(ShardCar.class).on("id", sc1.getId()).build()
                .joinFetch(ShardRoom.class, sr1)
                .execute().unique();
        TestCase.assertEquals(su.getRegion().getName(), user.getRegion().getName());
        TestCase.assertEquals(su.getRooms().size(), 1);
        TestCase.assertEquals(su.getRooms().get(0).getRoomNo(), sr1.getRoomNo());
        TestCase.assertEquals(su.getCars().size(), 1);
        TestCase.assertEquals(su.getCars().get(0).getCarNo(), sc1.getCarNo());
        
        
        su = sus.createQuery().addFilter("id", user.getId())
                .fetch(Region.class)   
                .addInnerJoinFilter("id", sr1.getId(), ShardRoom.class)
                .addInnerJoinFilter("carNo", sc1.getCarNo(), ShardCar.class)
                .joinFetch(ShardRoom.class)
                .joinFetch(ShardCar.class)
                .execute().unique();
        TestCase.assertEquals(su.getRegion().getName(), user.getRegion().getName());
        TestCase.assertEquals(su.getRooms().size(), 1);
        TestCase.assertEquals(su.getRooms().get(0).getRoomNo(), sr1.getRoomNo());
        TestCase.assertEquals(su.getCars().size(), 1);
        TestCase.assertEquals(su.getCars().get(0).getCarNo(), sc1.getCarNo());
    }

    private void reCreateTable(String tableName) {
        dropShardingTable(tableName, sus.getFactory());
        DDLHelper.createTable(sus.getFactory(), tableName,
                ShardingUser.class);
    }
    
    /**
     * <b>Description  异常测试</b>
     */
    @Test
    public void exceptionTest(){
        //准备数据
        reCreateTable("T_SHARD_USER0");
        ShardingUser u500 = addUser(500, "u500");
        ShardDept sd = new ShardDept(1L, u500);
        sds.add(sd);
        List<ShardingUser> users = new ArrayList<>();
        users.add(u500);
        sd.setUsers(users);
        sds.addJoinRecords(sd);
        TestCase.assertNotNull(sds.getByID(sd.getId()));
        
        //test
        try {
            sds.createQuery().addFilter("id", sd.getId())
                .fetch(ShardingUser.class).execute();
            throw new RuntimeException();
        } catch (Exception e) {
            TestCase.assertEquals(e.getClass(), FetchShardEntityException.class);
        }
        
        try {
            sds.createQuery().addFilter("id", sd.getId())
                .joinFetch(ShardingUser.class).execute();
            throw new RuntimeException();
        } catch (Exception e) {
            TestCase.assertEquals(e.getClass(), FetchShardEntityException.class);
        }

        try {
            sds.createQuery().addFilter("id", sd.getId())
                .buildFetch().fetch(ShardingUser.class).joinFetch(ShardingUser.class).build().execute();
            throw new RuntimeException();
        } catch (Exception e) {
            TestCase.assertEquals(e.getClass(), FetchShardEntityException.class);
        }
        try {
            sds.createQuery().addFilter("id", sd.getId())
                .buildFetch().joinFetch(ShardingUser.class).build().execute();
            throw new RuntimeException();
        } catch (Exception e) {
            TestCase.assertEquals(e.getClass(), FetchShardEntityException.class);
        }
    }
    
    
    /**
     * <b>Description 删除分片表</b>
     * 
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
