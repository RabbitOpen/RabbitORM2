package rabbit.open.dts.common.utils.ext;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import rabbit.open.dts.common.utils.ObjectSerializer;

import java.util.concurrent.ArrayBlockingQueue;

/**
 * 基于Kryo序列化技术的序列化工具, 该对象线程安全
 * @author xiaoqianbin
 * @date 2019/12/4
 **/
public class KryoObjectSerializer implements ObjectSerializer {

    private static ArrayBlockingQueue<Kryo> cache = new ArrayBlockingQueue<>(2000);

    private Kryo getKryo() {
        Kryo kryo = cache.poll();
        if (null != kryo) {
            return kryo;
        } else {
            kryo = new RabbitKryo();
            kryo.setClassLoader(Thread.currentThread().getContextClassLoader());
            return kryo;
        }
    }

    private boolean release(Kryo kryo) {
        try {
            return cache.add(kryo);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 序列化（对象 -> 字节数组）
     */
    public byte[] serialize(Object obj) {
        Kryo kryo = getKryo();
        try (Output output = new Output(4 * 1024, 8 * 1024 * 1024)) {
            kryo.writeObject(output, obj);
            output.flush();
            return output.toBytes();
        } finally {
            release(kryo);
        }
    }

    /**
     * 反序列化（字节数组 -> 对象）
     */
    public <T> T deserialize(byte[] data, Class<T> cls) {
        Kryo kryo = getKryo();
        try (Input input = new Input(data)) {
            return (T) kryo.readObject(input, kryo.getRegistration(cls).getType());
        } finally {
            release(kryo);
        }
    }

}
