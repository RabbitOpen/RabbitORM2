package rabbit.open.dtx.common.nio.pub.protocol;

import rabbit.open.algorithm.elect.data.ProtocolPacket;

/**
 * 协调数据
 * @author xiaoqianbin
 * @date 2020/1/7
 **/
public class Coordination {

    ProtocolPacket protocolPacket;

    public Coordination() {
    }

    public Coordination(ProtocolPacket protocolPacket) {
        this.protocolPacket = protocolPacket;
    }

    public ProtocolPacket getProtocolPacket() {
        return protocolPacket;
    }

    public void setProtocolPacket(ProtocolPacket protocolPacket) {
        this.protocolPacket = protocolPacket;
    }
}
