package rabbit.open.dtx.common.utils.ext;

import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import rabbit.open.dtx.common.utils.ObjectSerializer;

import java.util.concurrent.ArrayBlockingQueue;

/**
 * 基于Kryo序列化技术的序列化工具, 该对象线程安全
 * @author xiaoqianbin
 * @date 2019/12/4
 **/
public class KryoObjectSerializer implements ObjectSerializer {

    private static ArrayBlockingQueue<RabbitKryo> cache = new ArrayBlockingQueue<>(2000);

    private RabbitKryo getKryo(boolean resolveException) {
        RabbitKryo kryo = cache.poll();
        if (null != kryo) {
            kryo.setStrategy(resolveException);
            return kryo;
        } else {
            kryo = new RabbitKryo();
            kryo.setStrategy(resolveException);
            kryo.setClassLoader(Thread.currentThread().getContextClassLoader());
            return kryo;
        }
    }

    // 释放对象，如果池子满了就直接丢弃
    protected boolean release(RabbitKryo kryo) {
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
        RabbitKryo kryo = getKryo(false);
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
	public <T> T deserialize(byte[] data, Class<T> clz) {
        return deserialize(data, clz, false);
    }

    /**
     * 反序列化（字节数组 -> 对象）
     */
    @SuppressWarnings("unchecked")
	@Override
    public <T> T deserialize(byte[] data, Class<T> clz, boolean containException) {
        RabbitKryo kryo = getKryo(containException);
        try (Input input = new Input(data)) {
            return (T) kryo.readObject(input, kryo.getRegistration(clz).getType());
        } finally {
            release(kryo);
        }
    }

}
