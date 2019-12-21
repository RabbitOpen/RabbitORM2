package rabbit.open.dtx.common.utils.ext;

import com.esotericsoftware.kryo.Kryo;
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

    /**
     *
     * @param	resolveException 是否需要解析异常
     * @author  xiaoqianbin
     * @date    2019/12/9
     **/
    public void setStrategy(boolean resolveException) {
        if (resolveException) {
            setInstantiatorStrategy(stdStrategy);
        } else {
            setInstantiatorStrategy(defaultStrategy);
        }
    }

}
