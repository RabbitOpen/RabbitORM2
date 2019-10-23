package sharding.test.shardquery;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import junit.framework.TestCase;
import rabbit.open.orm.common.dml.FilterType;
import rabbit.open.orm.core.dialect.ddl.DDLHelper;
import rabbit.open.orm.core.dml.Cursor;
import rabbit.open.orm.core.dml.meta.MetaData;
import rabbit.open.orm.core.dml.shard.execption.InvalidShardedQueryException;
import sharding.test.shardquery.entity.Order;
import sharding.test.shardquery.service.OrderService;
import sharding.test.shardquery.service.PlanService;

/**
 * <b>Description: 分片表测试</b><br>
 * <b>@author</b> 肖乾斌
 * 
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:shard.xml" })
public class PrimaryKeyModShardingPolicyTest {

	Logger logger = LoggerFactory.getLogger(getClass());
	
	@Autowired
    private OrderService os;
	
	@Autowired
	PlanService ps;
	
	int scanCount = 0;
	
	/**
	 * <b>@description 简单dml操作测试  </b>
	 */
	@Test
	public void simpleDmlByIDTest() {
		initTables();
		// add测试
		for (int i = 0; i < 20; i++) {
			Order o = new Order();
			o.setId(i);
			o.setUsername("order-" + i);
			os.add(o);
		}
		
		// 根据主键查询测试
		for (int i = 0; i < 20; i++) {
			Order result = os.getByID(i);
			TestCase.assertEquals("order-" + i, result.getUsername());
		}
		// 更新测试
		for (int i = 0; i < 20; i++) {
			Order result = os.getByID(i);
			result.setUsername("xxx" + i);
			os.updateByID(result);
			result = os.getByID(i);
			TestCase.assertEquals("xxx" + i, result.getUsername());
		}
		// 删除测试
		for (int i = 0; i < 20; i++) {
			os.deleteByID(i);
			Order result = os.getByID(i);
			TestCase.assertNull(result);
		}
	}

	@Test
	public void queryTest() {
		initTables();
		// add测试
		for (int i = 0; i < 50; i++) {
			Order o = new Order();
			o.setId(i);
			o.setUsername("order-" + i);
			os.add(o);
		}
		
		// 根据id查询
		Cursor<Order> cursor = os.createShardedQuery().addFilter("id", 1).cursor();
		
		List<Order>  orders = new ArrayList<>();
		scanCount = 0;
		long ret = cursor.scanData(list -> {
			orders.addAll(list);
			scanCount++;
		});
		TestCase.assertEquals(1, scanCount);
		TestCase.assertEquals(1, orders.size());
		TestCase.assertEquals(1, ret);
		TestCase.assertEquals("order-1", orders.get(0).getUsername());
		
		cursor = os.createShardedQuery().addFilter("id", 3, FilterType.GT).cursor();
		orders.clear();
		scanCount = 0;
		ret = cursor.scanData(list -> {
			orders.addAll(list);
			scanCount++;
		});
		TestCase.assertEquals(scanCount, 10);
		TestCase.assertEquals(orders.size(), 46);
		
		cursor = os.createShardedQuery().cursor();
		orders.clear();
		scanCount = 0;
		ret = cursor.scanData(list -> {
			orders.addAll(list);
			scanCount++;
		});
		TestCase.assertEquals(scanCount, 10);
		TestCase.assertEquals(orders.size(), 50);
		TestCase.assertEquals(cursor.count(), 50);
	}

	
	@Test
	public void pageTest() {
		initTables();
		// add测试
		for (int i = 0; i < 50; i++) {
			Order o = new Order();
			o.setId(i);
			o.setUsername("order-" + i);
			os.add(o);
		}
		List<Integer> idList = Arrays.asList(1, 3, 2, 13, 23);
		Cursor<Order> cursor = os.createShardedQuery().setPageSize(2).addFilter("id", idList, FilterType.IN).cursor();
		List<Order>  orders = new ArrayList<>();
		scanCount = 0;
		long ret = cursor.scanData(list -> {
			orders.addAll(list);
			scanCount++;
		});
		TestCase.assertEquals(4, scanCount);
		TestCase.assertEquals(idList.size(), orders.size());
		TestCase.assertEquals(idList.size(), ret);
		for (Order o : orders) {
			boolean exits = false;
			for (int id : idList) {
				if (o.getId().equals(id)) {
					exits = true;
					break;
				}
			}
			TestCase.assertTrue(exits);
			TestCase.assertEquals("order-" + o.getId(), o.getUsername());
		}
	}

	
	@Test
	public void inByListTest() {
		initTables();
		// add测试
		for (int i = 0; i < 50; i++) {
			Order o = new Order();
			o.setId(i);
			o.setUsername("order-" + i);
			os.add(o);
		}
		List<Integer> idList = Arrays.asList(1, 13, 2, 18, 12);
		Cursor<Order> cursor = os.createShardedQuery().addFilter("id", idList, FilterType.IN)
				.desc("id").addNullFilter("username", false).cursor();
		
		List<Order>  orders = new ArrayList<>();
		scanCount = 0;
		long ret = cursor.scanData(list -> {
			orders.addAll(list);
			scanCount++;
		});
		TestCase.assertEquals(4, scanCount);
		TestCase.assertEquals(idList.size(), orders.size());
		TestCase.assertEquals(idList.size(), ret);
		
		TestCase.assertEquals(idList.size(), cursor.count());
		
		orders.clear();
		scanCount = 0;
		ret = cursor.scanData(list -> {
			orders.addAll(list);
			scanCount++;
		});
		TestCase.assertEquals(4, scanCount);
		TestCase.assertEquals(idList.size(), orders.size());
		TestCase.assertEquals(idList.size(), ret);
		
		TestCase.assertEquals(idList.size(), cursor.count());
		
		for (Order o : orders) {
			boolean exits = false;
			for (int id : idList) {
				if (o.getId().equals(id)) {
					exits = true;
					break;
				}
			}
			TestCase.assertTrue(exits);
			TestCase.assertEquals("order-" + o.getId(), o.getUsername());
		}
	}

	@Test
	public void inByArrTest() {
		initTables();
		// add测试
		for (int i = 0; i < 50; i++) {
			Order o = new Order();
			o.setId(i);
			o.setUsername("order-" + i);
			os.add(o);
		}
		Integer[] ids = new Integer[] {1, 12, 15, 19};
		Cursor<Order> cursor = os.createShardedQuery().addFilter("id", ids, FilterType.IN)
				.asc("id").cursor();
		List<Order>  orders = new ArrayList<>();
		scanCount = 0;
		long ret = cursor.scanData(list -> {
			orders.addAll(list);
			scanCount++;
		});
		TestCase.assertEquals(4, scanCount);
		TestCase.assertEquals(4, orders.size());
		TestCase.assertEquals(ids.length, ret);
		for (Order o : orders) {
			boolean exits = false;
			for (int id : ids) {
				if (o.getId().equals(id)) {
					exits = true;
					break;
				}
			}
			TestCase.assertTrue(exits);
			TestCase.assertEquals("order-" + o.getId(), o.getUsername());
		}
		
	}
	
	
	private void initTables() {
		try {
			Connection read = os.getDs1().getConnection();
			for (int i = 0; i < 5; i++) {
				reCreateTable(MetaData.getMetaByClass(Order.class).getTableName() + String.format("_%04d", i), read);
			}
			read.close();
			
			Connection write = os.getDs2().getConnection();
			for (int i = 5; i < 10; i++) {
				reCreateTable(MetaData.getMetaByClass(Order.class).getTableName() + String.format("_%04d", i), write);
			}
			write.close();
		} catch (SQLException e) {
			logger.error(e.getMessage(), e);
		}
	}

	private void reCreateTable(String tableName, Connection connection) {
		try {
			dropShardingTable(tableName, connection);
			DDLHelper.createTable(connection, os.getFactory().getDialectType(), tableName, Order.class);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	}
	
	@Test
	public void exceptionTest() {
		try {
			ps.createShardedQuery();
			throw new RuntimeException();
		} catch (Exception e) {
			TestCase.assertEquals(InvalidShardedQueryException.class, e.getClass());
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
            stmt.execute("drop table if exists " + tableName);
            stmt.close();
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
    }
}
