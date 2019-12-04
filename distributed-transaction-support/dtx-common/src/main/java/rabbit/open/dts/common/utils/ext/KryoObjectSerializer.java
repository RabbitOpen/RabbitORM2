package rabbit.open.dts.common.utils.ext;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import org.objenesis.strategy.StdInstantiatorStrategy;
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
            kryo = new Kryo() ;
            kryo.setClassLoader(Thread.currentThread().getContextClassLoader());
            // 该策略支持对异常的序列化
            kryo.setInstantiatorStrategy(new StdInstantiatorStrategy());
            return kryo;
        }
    }

    private boolean release(Kryo kryo) {
        return cache.add(kryo);
    }

    /**
     * 序列化（对象 -> 字节数组）
     */
    public byte[] serialize(Object obj) {
        Output output = null;
        Kryo kryo = getKryo();
        try {
            output = new Output(4 * 1024, 8 * 1024 * 1024);
            kryo.writeObject(output, obj);
            output.flush();
            return output.toBytes();
        } finally {
            release(kryo);
            if (null != output) {
                output.close();
            }
        }
    }

    /**
     * 反序列化（字节数组 -> 对象）
     */
    public <T> T deserialize(byte[] data, Class<T> cls) {
        Input input = null;
        Kryo kryo = getKryo();
        try {
            input = new Input(data);
            return (T) kryo.readObject(input, kryo.getRegistration(cls).getType());
        } finally {
            release(kryo);
            if (null != input) {
                input.close();
            }
        }
    }

}
