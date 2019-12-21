package rabbit.open.dtx.common.nio.pub.protocol;

/**
 * Rpc协议
 * @author xiaoqianbin
 * @date 2019/12/7
 **/
public class RpcProtocol {

    // 调用的rpc服务的namespace
    private String namespace;

    // 方法名
    private String methodName;

    // 参数类型
    private Class<?>[] argTypes;

    // 值
    private Object[] values;

    public RpcProtocol(String namespace, String methodName, Class<?>[] argTypes, Object[] values) {
        this.namespace = namespace;
        this.methodName = methodName;
        this.argTypes = argTypes;
        this.values = values;
    }

    public RpcProtocol() {

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

    public Object[] getValues() {
        return values;
    }

}
