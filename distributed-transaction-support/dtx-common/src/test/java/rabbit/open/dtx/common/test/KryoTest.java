package rabbit.open.dtx.common.test;

import junit.framework.TestCase;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import rabbit.open.dtx.common.nio.pub.ProtocolData;
import rabbit.open.dtx.common.utils.ObjectSerializer;
import rabbit.open.dtx.common.utils.ext.KryoObjectSerializer;
import rabbit.open.dtx.common.utils.ext.RabbitKryo;

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
        int count = 20;
        CountDownLatch cdl = new CountDownLatch(count);
        for (int c = 0; c < count; c++) {
            new Thread(() -> {
                for (int i = 0; i < 50000; i++) {
                ObjectSerializer serializer = new KryoObjectSerializer();
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
                    Exception s = serializer.deserialize(bytes, Exception.class, true);
                    TestCase.assertTrue(Exception.class.equals(s.getClass()));
                    TestCase.assertTrue(e.getMessage().equals(s.getMessage()));
                    TestCase.assertTrue("hello".equals(s.getMessage()));
                }
                cdl.countDown();
            }).start();

        }
        cdl.await();
    }

    @Test
    public void exceptionFieldTest() {
        MyKryoObjectSerializer serializer = new MyKryoObjectSerializer();
        ProtocolData data = new ProtocolData();
        data.setData(new Exception());
        byte[] bytes = serializer.serialize(data);
        serializer.deserialize(bytes, ProtocolData.class, true);

        for (int i = 0; i < 2000; i++) {
            serializer.release(new RabbitKryo());
        }
        TestCase.assertTrue(!serializer.release(new RabbitKryo()));
    }

    class MyKryoObjectSerializer extends KryoObjectSerializer {
        @Override
        public boolean release(RabbitKryo kryo) {
            return super.release(kryo);
        }
    }
}
