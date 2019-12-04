package rabbit.open.dts.common.utils;

/**
 * 对象序列化，反序列化工具
 * @author xiaoqianbin
 * @date 2019/12/4
 **/
public interface ObjectSerializer {

    /**
     * 序列化
     * @param	object
     * @author  xiaoqianbin
     * @date    2019/12/4
     **/
    byte[] serialize(Object object);

    /**
     * 反序列化
     * @param	bytes
	 * @param	clz
     * @author  xiaoqianbin
     * @date    2019/12/4
     **/
    <T> T deserialize(byte[] bytes, Class<T> clz);
}
