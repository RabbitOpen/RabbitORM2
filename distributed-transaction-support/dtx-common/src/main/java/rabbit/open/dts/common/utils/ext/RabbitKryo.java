package rabbit.open.dts.common.utils.ext;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import org.objenesis.strategy.InstantiatorStrategy;
import org.objenesis.strategy.StdInstantiatorStrategy;

/**
 * Kryo扩展类
 * @author xiaoqianbin
 * @date 2019/12/4
 **/
public class RabbitKryo extends Kryo {

    // 该策略不支持对异常的序列化
    private InstantiatorStrategy defaultStrategy = new Kryo.DefaultInstantiatorStrategy();

    // 该策略不支持对List对象的序列化
    private InstantiatorStrategy stdStrategy = new StdInstantiatorStrategy();

    @Override
    public void writeObject(Output output, Object object) {
        if (object instanceof Throwable) {
            setInstantiatorStrategy(stdStrategy);
        } else {
            setInstantiatorStrategy(defaultStrategy);
        }
        super.writeObject(output, object);
    }

    @Override
    public <T> T readObject(Input input, Class<T> type) {
        if (Throwable.class.isAssignableFrom(type)) {
            setInstantiatorStrategy(stdStrategy);
        } else {
            setInstantiatorStrategy(defaultStrategy);
        }
        return super.readObject(input, type);
    }
}
