package rabbit.open.dtx.common.nio.server.handler;

import rabbit.open.dtx.common.nio.pub.DataHandler;
import rabbit.open.dtx.common.nio.pub.ProtocolData;
import rabbit.open.dtx.common.nio.pub.protocol.ClientInstance;
import rabbit.open.dtx.common.nio.server.ext.AbstractServerEventHandler;

/**
 * @author xiaoqianbin
 * @date 2020/1/8
 **/
public class InstanceIDGenerator implements DataHandler {

    private AbstractServerEventHandler eventHandler = null;

    public InstanceIDGenerator(AbstractServerEventHandler eventHandler) {
        this.eventHandler = eventHandler;
    }

    @Override
    public Object process(ProtocolData protocolData) {
        Long globalId = 0L;
        if (null != eventHandler.getTransactionHandler()) {
            globalId = eventHandler.getTransactionHandler().getNextGlobalId();
        }
        return new ClientInstance(globalId);
    }
}
