package rabbit.open.dtx.common.nio.pub;

/**
 * 数据处理接口
 * @author xiaoqianbin
 * @date 2019/12/7
 **/
public interface DataHandler {

    /**
     * 处理协议数据
     * @param	protocolData
     * @author  xiaoqianbin
     * @date    2019/12/7
     **/
    Object process(ProtocolData protocolData);

}
