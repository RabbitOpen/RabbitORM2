package rabbit.open.dtx.common.test;

import junit.framework.TestCase;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rabbit.open.dtx.common.nio.client.RoundList;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * @author xiaoqianbin
 * @date 2020/1/9
 **/
@RunWith(JUnit4.class)
public class RoundListTest {

    private Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * <b>@description 功能测试 </b>
     * @throws InterruptedException
     */
    @Test
    public void roundListTest() throws InterruptedException {
        RoundList<Integer> list = new RoundList<>();
        int result = 0;
        for (int i = 0; i < 10; i++) {
            list.add(i);
            result += i;
        }
        TestCase.assertTrue(0 == list.peekNext());
        int sum = 0;
        for (int i = 0; i < 10; i++) {
            TestCase.assertEquals(list.peekNext().intValue(), list.peekNext().intValue());
            Integer fetch = list.fetch();
            if (i == 9) {
                TestCase.assertEquals(0, list.peekNext().intValue());
            } else {
                TestCase.assertEquals(fetch + 1, list.peekNext().intValue());
            }
            sum += fetch;
        }
        TestCase.assertTrue(0 == list.peekNext());
        TestCase.assertEquals(result, sum);
        TestCase.assertEquals(10, list.size());
        for (int i = 0; i < 10; i++) {
            list.remove(i);
        }
        TestCase.assertTrue(list.isEmpty());
        TestCase.assertNull(list.fetch());
        TestCase.assertNull(list.peekNext());
        TestCase.assertNull(list.fetch(10));
        new Thread(() -> TestCase.assertTrue(10 == list.fetch(200))).start();
        holdOn(50);
        list.add(10);
    }

    private void holdOn(long millSeconds) throws InterruptedException {
        Semaphore s = new Semaphore(0);
        if (s.tryAcquire(millSeconds, TimeUnit.MILLISECONDS)) {
            logger.info("不可能走到这里来");
        }
    }
    
    /**
     * <b>@description 性能测试 </b>
     * @throws InterruptedException 
     */
    @Test
    public void benchmark() throws InterruptedException {
    	int queueSize = 64;
		ArrayBlockingQueue<Integer> queue = new ArrayBlockingQueue<>(queueSize);
    	RoundList<Integer> list = new RoundList<>();
    	for (int i = 0; i < queueSize; i++) {
    		list.add(i);
    		queue.add(i);
    	}
    	int count = 50;
    	long start = System.currentTimeMillis();
		CountDownLatch cdl = new CountDownLatch(count);
		for (int i = 0; i < count; i++) {
			new Thread(() -> {
				for (int j = 0; j < 1000000; j++) {
					try {
						Integer poll = queue.poll(3, TimeUnit.SECONDS);
						queue.put(poll);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				cdl.countDown();
			}).start();
		}
		cdl.await();
		System.out.println("cost " + (System.currentTimeMillis() - start));
		
		start = System.currentTimeMillis();
		CountDownLatch cdl2 = new CountDownLatch(count);
		for (int i = 0; i < count; i++) {
			new Thread(() -> {
				for (int j = 0; j < 1000000; j++) {
					Integer poll = list.fetch(3000);
					if (null == poll) {
						throw new RuntimeException("");
					}
				}
				cdl2.countDown();
			}).start();
		}
		cdl2.await();
		System.out.println("list : cost " + (System.currentTimeMillis() - start));
		
		TestCase.assertEquals(queue.size(), queueSize);
		TestCase.assertEquals(list.size(), queueSize);
    }

}
