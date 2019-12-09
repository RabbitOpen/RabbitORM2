package rabbit.open.dts.common.rpc.nio.pub;

import java.io.Serializable;

/**
 * 协议数据
 * @author xiaoqianbin
 * @date 2019/12/7
 **/
@SuppressWarnings("serial")
public class ProtocolData implements Serializable {

    // 协议包类型字段长度：4字节
    public static final int PROTOCOL_HEADER_LENGTH = 4;

    // 数据类型
    private int dataType = DataType.REQUEST;

    private int dataLength = 0;

    private int responseStatus = ResponseStatus.SUCCESS;

    private Serializable data;

    // 请求id
    private Long requestId;

    public int getDataType() {
        return dataType;
    }

    public void setDataType(int dataType) {
        this.dataType = dataType;
    }

    public int getDataLength() {
        return dataLength;
    }

    public void setDataLength(int dataLength) {
        this.dataLength = dataLength;
    }

    public int getResponseStatus() {
        return responseStatus;
    }

    public void setResponseStatus(int responseStatus) {
        this.responseStatus = responseStatus;
    }

    public Serializable getData() {
        return data;
    }

    public void setData(Serializable data) {
        this.data = data;
    }

    public Long getRequestId() {
        return requestId;
    }

    public void setRequestId(Long requestId) {
        this.requestId = requestId;
    }
}
