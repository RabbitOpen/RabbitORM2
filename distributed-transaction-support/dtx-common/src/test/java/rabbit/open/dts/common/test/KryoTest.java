package rabbit.open.dts.common.test;

import junit.framework.TestCase;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import rabbit.open.dts.common.utils.ext.KryoObjectSerializer;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.CountDownLatch;

/**
 * @author xiaoqianbin
 * @date 2019/12/4
 **/
@RunWith(JUnit4.class)
public class KryoTest {

    @Test
    public void kryoObjectSerializerTest() throws InterruptedException {
        CountDownLatch cdl = new CountDownLatch(20);
        for (int c = 0; c < 20; c++) {
            new Thread(() -> {
                KryoObjectSerializer serializer = new KryoObjectSerializer();
                for (int i = 0; i < 50000; i++) {
                    Date date = new Date();
                    byte[] bytes = serializer.serialize(date);
                    Date s = serializer.deserialize(bytes, Date.class);
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    TestCase.assertTrue(sdf.format(date).equals(sdf.format(s)));
                }
                cdl.countDown();
            }).start();

        }
        cdl.await();

    }

    @Test
    public void serializeExceptionTest() throws InterruptedException {
        CountDownLatch cdl = new CountDownLatch(20);
        for (int c = 0; c < 20; c++) {
            new Thread(() -> {
                KryoObjectSerializer serializer = new KryoObjectSerializer();
                for (int i = 0; i < 50000; i++) {
                    Exception e = new Exception("hello");
                    byte[] bytes = serializer.serialize(e);
                    Exception s = serializer.deserialize(bytes, Exception.class);
                    TestCase.assertTrue(Exception.class.equals(s.getClass()));
                    TestCase.assertTrue(e.getMessage().equals(s.getMessage()));
                    TestCase.assertTrue("hello".equals(s.getMessage()));
                }
                cdl.countDown();
            }).start();

        }
        cdl.await();
    }
}
