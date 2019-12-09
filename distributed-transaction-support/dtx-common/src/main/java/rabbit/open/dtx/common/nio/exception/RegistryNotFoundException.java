package rabbit.open.dtx.common.nio.exception;

/**
 * 注册对象没声明异常
 * @author xiaoqianbin
 * @date 2019/12/7
 **/
@SuppressWarnings("serial")
public class RegistryNotFoundException extends RuntimeException {

    public RegistryNotFoundException(String registryName) {
        super(String.format("%s is not existed!", registryName));
    }

}
