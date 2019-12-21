package rabbit.open.dtx.common.nio.pub;

/**
 * 协议数据  LTV结构
 * @author xiaoqianbin
 * @date 2019/12/7
 **/
public class ProtocolData {

    // 协议包类型字段长度：8字节  4字节长度， 4字节类型
    public static final int PROTOCOL_HEADER_LENGTH = 8;

    private Object data;

    // 请求id
    private Long requestId;

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

    public Long getRequestId() {
        return requestId;
    }

    public void setRequestId(Long requestId) {
        this.requestId = requestId;
    }

}
