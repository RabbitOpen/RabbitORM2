package rabbit.open.dts.common.rpc.nio.pub;

import java.io.Serializable;

/**
 * Rpc协议
 * @author xiaoqianbin
 * @date 2019/12/7
 **/
@SuppressWarnings("serial")
public class RabbitProtocol implements Serializable {

    // 调用的rpc服务的namespace
    private String namespace;

    // 方法名
    private String methodName;

    // 参数类型
    private Class<?>[] argTypes;

    // 值
    private Serializable[] values;

    public RabbitProtocol(String namespace, String methodName, Class<?>[] argTypes, Serializable[] values) {
        this.namespace = namespace;
        this.methodName = methodName;
        this.argTypes = argTypes;
        this.values = values;
    }

    public String getNamespace() {
        return namespace;
    }

    public String getMethodName() {
        return methodName;
    }

    public Class<?>[] getArgTypes() {
        return argTypes;
    }

    public Serializable[] getValues() {
        return values;
    }

}
