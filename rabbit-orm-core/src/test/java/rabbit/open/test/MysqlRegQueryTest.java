package rabbit.open.test;

import com.sun.tools.internal.xjc.reader.xmlschema.bindinfo.BIConversion.User;
import junit.framework.TestCase;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import rabbit.open.orm.common.dml.FilterType;
import rabbit.open.orm.common.exception.InvalidGroupByFieldException;
import rabbit.open.orm.common.exception.UnSupportedOperationException;
import rabbit.open.test.entity.RegRoom;
import rabbit.open.test.entity.RegUser;
import rabbit.open.test.service.RegRoomService;
import rabbit.open.test.service.RegUserService;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * <b>Description: 关于正则表达式参数的查询测试</b><br>
 * <b>@author</b> 肖乾斌
 * 
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:applicationContext.xml" })
public class MysqlRegQueryTest {

	@Autowired
	RegUserService rus;
	
	@Autowired
	RegRoomService rrs;

	/**
	 * <b>@description 正则表达式查询  </b>
	 */
	@Test
	public void regQueryTest() {
		RegUser user = new RegUser();
		Date start = new Date();
		// 一天的毫秒数
		long milDays = 24 * 60 * 60 * 1000;
		Date end = new Date(start.getTime() + 2L * milDays);
		user.setStart(start);
		user.setEnd(end);
		user.setFrom(100);
		user.setTo(200);
		rus.add(user);
		
		RegRoom r1 = new RegRoom();
		r1.setStart(start);
		r1.setEnd(end);
		r1.setUser(user);
		rrs.add(r1);
		RegRoom r2 = new RegRoom();
		r2.setStart(start);
		r2.setEnd(end);
		r2.setUser(user);
		rrs.add(r2);
		
		TestCase.assertEquals(0, rus.createQuery().addFilter("${to} - ${from}", 
				user.getTo() - user.getFrom(), FilterType.GT).count());
		TestCase.assertEquals(1, rus.createQuery().addFilter("${to} - ${from}", 
				user.getTo() - user.getFrom(), FilterType.LTE).count());
		
		TestCase.assertEquals(1, rus.createQuery()
				.addFilter("${to} - ${from}", user.getTo() - user.getFrom(), FilterType.LTE)
				// 起始日期和截止日期相差48小时
				.addFilter("TIMESTAMPDIFF(hour, ${start}, ${end})", 48)
				.count());
		
		RegUser unique = rus.createQuery().addFilter("${to} - ${from}", user.getTo() - user.getFrom(), FilterType.LTE)
			// 起始日期和截止日期相差48小时
			.addFilter("TIMESTAMPDIFF(hour, ${start}, ${end})", 48)
			.joinFetch(RegRoom.class)
			.addJoinFilter("TIMESTAMPDIFF(hour, ${start}, ${end})", 48, RegRoom.class)
			.addFilter("id", user.getId()).unique();
		TestCase.assertEquals(unique.getRooms().size(), 2);
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		TestCase.assertTrue(sdf.format(unique.getRooms().get(0).getStart()).equals(sdf.format(start)));
		TestCase.assertTrue(sdf.format(unique.getRooms().get(0).getEnd()).equals(sdf.format(end)));
	}
	
	@Test
	public void dynamicQueryTest() {
		int count = 5;
		int from = 100;
		int to = 200;
		for (int i = 0; i < count; i++) {
			RegUser user = new RegUser();
			Date start = new Date();
			// 一天的毫秒数
			long milDays = 24 * 60 * 60 * 1000;
			Date end = new Date(start.getTime() + 2L * milDays);
			user.setStart(start);
			user.setEnd(end);
			user.setFrom(from);
			user.setTo(to);
			user.setName("zhangsan");
			rus.add(user);
		}
		for (int i = 0; i < count; i++) {
			RegUser user = new RegUser();
			Date start = new Date();
			// 一天的毫秒数
			long milDays = 24 * 60 * 60 * 1000;
			Date end = new Date(start.getTime() + 2L * milDays);
			user.setStart(start);
			user.setEnd(end);
			user.setFrom(from);
			user.setTo(to);
			user.setName("lisi");
			rus.add(user);
		}
		List<RegUser> list = rus.createDynamicQuery()
				.querySpecifiedFields("name", "countOfName", "sumOfFrom").groupBy("name").page(0,  10)
				.list();
		TestCase.assertTrue(list.size() >= 2);
		int find = 0;
		for (RegUser u : list) {
			if ("lisi".equals(u.getName()) || "zhangsan".equals(u.getName())) {
				TestCase.assertTrue(count == u.getCountOfName());
				TestCase.assertTrue(count * from == u.getSumOfFrom());
				find++;
			}
		}
		TestCase.assertEquals(2, find);
	}
	
	@Test
	public void exceptionTest() {
		try {
			RegUser user = new RegUser();
			Date start = new Date();
			long milDays = 24 * 60 * 60 * 1000;
			Date end = new Date(start.getTime() + 2L * milDays);
			user.setStart(start);
			user.setEnd(end);
			rus.createDynamicQuery(user).querySpecifiedFields("name", "countOfName").groupBy("countOfName").list();
			throw new RuntimeException();
		} catch (InvalidGroupByFieldException e) {
		}
		
		try {
			rus.createDynamicQuery().fetch(User.class);
			throw new RuntimeException();
		} catch (UnSupportedOperationException e) {
		}
		
		try {
			rus.createDynamicQuery().innerFetch(User.class);
			throw new RuntimeException();
		} catch (UnSupportedOperationException e) {
		}
		
		try {
			rus.createDynamicQuery().joinFetch(User.class, null);
			throw new RuntimeException();
		} catch (UnSupportedOperationException e) {
		}
		
		try {
			rus.createDynamicQuery().joinFetchByFilter(1, User.class, null);
			throw new RuntimeException();
		} catch (UnSupportedOperationException e) {
		}
		
		try {
			rus.createDynamicQuery().innerJoinFetch(User.class);
			throw new RuntimeException();
		} catch (UnSupportedOperationException e) {
		}
		
		try {
			rus.createDynamicQuery().innerJoinFetchByFilter(1, User.class);
			throw new RuntimeException();
		} catch (UnSupportedOperationException e) {
		}
	}
}
