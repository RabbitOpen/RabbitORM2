package sharding.test.shardquery;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import rabbit.open.orm.core.dml.meta.MetaData;
import rabbit.open.orm.core.dml.meta.TableMeta;
import rabbit.open.orm.core.dml.shard.execption.InvalidShardedQueryException;
import rabbit.open.orm.core.dml.shard.execption.NoShardTableException;
import rabbit.open.orm.core.dml.shard.impl.DeleteCursor;
import rabbit.open.orm.core.dml.shard.impl.QueryCursor;
import rabbit.open.orm.core.dml.shard.impl.ShardedQueryCursor;
import rabbit.open.orm.core.dml.shard.impl.UpdateCursor;
import sharding.test.shardquery.entity.Order;
import sharding.test.shardquery.service.OrderService;
import sharding.test.shardquery.service.PlanService;
import sharding.test.shardquery.service.PlanXService;

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
	
	@Autowired
	PlanXService pxs;
	
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
	public void counterTest() {
		initTables();
		// add测试
		for (int i = 0; i < 50; i++) {
			Order o = new Order();
			o.setId(i);
			o.setUsername("orderx-" + i);
			os.add(o);
		}
		QueryCursor<Order> cursor = os.createShardedQuery().cursor();
		Map<TableMeta, Long> names = new HashMap<>();
		long total = cursor.count((count, tableMeta) -> {
			TestCase.assertEquals(5, count);
			names.put(tableMeta, count);
		});
		TestCase.assertEquals(10, names.size());
		TestCase.assertEquals(50, total);
		cursor.next((list, tableMeta) -> {
			TestCase.assertEquals(list.size(), names.get(tableMeta).intValue());
		});
	}
	
	@Test
	public void sqlQueryTest() {
		initTables();
		// add测试
		for (int i = 0; i < 30; i++) {
			Order o = new Order();
			o.setId(i);
			o.setUsername("order-" + i);
			os.add(o);
		}
		ShardedQueryCursor<Order> cursor = os.createShardedSQLQuery("readFromOrder").set("id", 3, FilterType.GT).cursor();
		scanCount = 0;
		long count = cursor.next((list, tabMeta) -> {
			scanCount++;
		});
		TestCase.assertEquals(10, scanCount);
		TestCase.assertEquals(26, count);
		scanCount = 0;
		TestCase.assertEquals(26, cursor.count((num, tabMeta) -> {
			scanCount++;
		}));
		TestCase.assertEquals(10, scanCount);
		
		cursor = os.createShardedSQLQuery("readFromOrder2")
				.set("id", 3, FilterType.GT)
				.set("username", "order-20")
				.cursor();
		scanCount = 0;
		count = cursor.next((list, tabMeta) -> {
			scanCount++;
		});
		TestCase.assertEquals(10, scanCount);
		TestCase.assertEquals(1, count);
		
		// 测试分页
		cursor = os.createShardedSQLQuery("readFromOrder")
				.set("id", 9, FilterType.GT)
				.setPageSize(2)
				.cursor();
		scanCount = 0;
		count = cursor.next((list, tabMeta) -> {
			scanCount++;
		});
		TestCase.assertEquals(20, scanCount);
		TestCase.assertEquals(20, count);
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
		QueryCursor<Order> cursor = os.createShardedQuery().addFilter("id", 1).cursor();
		
		List<Order>  orders = new ArrayList<>();
		scanCount = 0;
		long ret = cursor.next((list, tableMeta) -> {
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
		ret = cursor.next((list, tableMeta) -> {
			orders.addAll(list);
			scanCount++;
		});
		TestCase.assertEquals(scanCount, 10);
		TestCase.assertEquals(orders.size(), 46);
		
		cursor = os.createShardedQuery().cursor();
		orders.clear();
		scanCount = 0;
		ret = cursor.next((list, tableMeta) -> {
			orders.addAll(list);
			scanCount++;
		});
		TestCase.assertEquals(scanCount, 10);
		TestCase.assertEquals(orders.size(), 50);
		TestCase.assertEquals(cursor.count(), 50);
		
		// 多条件取交集
		cursor = os.createShardedQuery()
				.addFilter("id", new Integer[] {1, 2, 3}, FilterType.IN)
				.addFilter("id", new Integer[] {4, 2, 3}, FilterType.IN)
				.cursor();
		scanCount = 0;
		ret = cursor.next((list, tableMeta) -> {
			scanCount++;
		});
		TestCase.assertEquals(scanCount, 2);
		TestCase.assertEquals(ret, 2);
		
		cursor = os.createShardedQuery()
				.addFilter("id", new Integer[] {1, 2, 3}, FilterType.IN)
				.addFilter("id", new Integer[] {4, 5, 6}, FilterType.IN)
				.cursor();
		scanCount = 0;
		ret = cursor.next((list, tableMeta) -> {
			scanCount++;
		});
		TestCase.assertEquals(scanCount, 1);
		TestCase.assertEquals(ret, 0);
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
		QueryCursor<Order> cursor = os.createShardedQuery().setPageSize(2).addFilter("id", idList, FilterType.IN).cursor();
		List<Order>  orders = new ArrayList<>();
		scanCount = 0;
		long ret = cursor.next((list, tableMeta) -> {
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
		TestCase.assertEquals(os.createShardedQuery().addFilter("id", idList, FilterType.IN)
				.desc("id").addNullFilter("username").cursor().count(), 0);
		QueryCursor<Order> cursor = os.createShardedQuery().addFilter("id", idList, FilterType.IN)
				.desc("id").addNullFilter("username", false).cursor();
		
		List<Order>  orders = new ArrayList<>();
		scanCount = 0;
		long ret = cursor.next((list, tableMeta) -> {
			orders.addAll(list);
			scanCount++;
		});
		TestCase.assertEquals(4, scanCount);
		TestCase.assertEquals(idList.size(), orders.size());
		TestCase.assertEquals(idList.size(), ret);
		
		TestCase.assertEquals(idList.size(), cursor.count());
		
		orders.clear();
		scanCount = 0;
		ret = cursor.next((list, tableMeta) -> {
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
	public void shardedDeleteTest() {
		initTables();
		// add测试
		for (int i = 0; i < 50; i++) {
			Order o = new Order();
			o.setId(i);
			o.setUsername("order-dd" + i);
			os.add(o);
		}
		Order o = new Order();
		int index = 5;
		o.setId(index);
		o.setUsername("order-dd" + index);
		DeleteCursor<Order> cursor = os.createShardedDelete(o).addFilter("id", index).cursor();
		scanCount = 0;
		long total = cursor.next((count, tableMeta) -> {
			scanCount++;
			TestCase.assertEquals(1, count);
		});
		TestCase.assertEquals(1, total);
		TestCase.assertEquals(1, scanCount);
		
		cursor = os.createShardedDelete().addFilter("id", 10, FilterType.GTE).cursor();
		scanCount = 0;
		total = cursor.count((count, tableMeta) -> {
			scanCount++;
			TestCase.assertEquals(4, count);
		});
		TestCase.assertEquals(40, total);
		TestCase.assertEquals(10, scanCount);
		
		TestCase.assertEquals(os.createShardedDelete().addNullFilter("username").cursor().next(), 0);
		TestCase.assertEquals(os.createShardedDelete().addNullFilter("username", false).cursor().count(), 9);
		
	}

	@Test
	public void shardedUpdateTest() {
		initTables();
		// add测试
		for (int i = 0; i < 50; i++) {
			Order o = new Order();
			o.setId(i);
			o.setUsername("order-dd" + i);
			os.add(o);
		}
		Order o = new Order();
		int index = 8;
		o.setId(index);
		o.setUsername("order-dd" + index);
		String username = "hello";
		UpdateCursor<Order> cursor = os.createShardedUpdate(o)
				.set("username", username)
				.addFilter("id", index)
				.cursor();
		scanCount = 0;
		long total = cursor.next((count, tabMeta) -> {
			scanCount++;
			TestCase.assertEquals(1, count);
		});
		TestCase.assertEquals(1, total);
		TestCase.assertEquals(1, scanCount);
		TestCase.assertEquals(os.getByID(index).getUsername(), username);
		
		
		o = new Order();
		o.setUsername("xxx");
		cursor = os.createShardedUpdate().setValue(o).addFilter("id", 10, FilterType.GTE).cursor();
		scanCount = 0;
		total = cursor.count((count, tabMeta) -> {
			scanCount++;
			TestCase.assertEquals(4, count);
		});
		TestCase.assertEquals(40, total);
		TestCase.assertEquals(10, scanCount);
		
		cursor = os.createShardedUpdate(o)
				.setNull("username")
				.addFilter("id", 18)
				.cursor();
		TestCase.assertEquals(cursor.next(), 1);
		TestCase.assertNull(os.getByID(18).getUsername());
		TestCase.assertEquals(os.createShardedUpdate().set("username", "23").addNullFilter("username", false).cursor().count(), 49);
		TestCase.assertEquals(os.createShardedUpdate().set("username", "23").addNullFilter("username").cursor().count(), 1);
		TestCase.assertEquals(os.createShardedQuery().addFilter("username", "23").cursor().count(), 50);
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
		QueryCursor<Order> cursor = os.createShardedQuery().addFilter("id", ids, FilterType.IN)
				.asc("id").cursor();
		List<Order>  orders = new ArrayList<>();
		scanCount = 0;
		long ret = cursor.next((list, tableMeta) -> {
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
			for (int i = 0; i < 5; i++) {
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
		try {
			ps.createShardedDelete();
			throw new RuntimeException();
		} catch (Exception e) {
			TestCase.assertEquals(InvalidShardedQueryException.class, e.getClass());
		}
		try {
			pxs.createShardedDelete().cursor().next();
			throw new RuntimeException();
		} catch (Exception e) {
			TestCase.assertEquals(NoShardTableException.class, e.getClass());
		}
		try {
			pxs.createShardedUpdate().cursor().next();
			throw new RuntimeException();
		} catch (Exception e) {
			TestCase.assertEquals(NoShardTableException.class, e.getClass());
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
