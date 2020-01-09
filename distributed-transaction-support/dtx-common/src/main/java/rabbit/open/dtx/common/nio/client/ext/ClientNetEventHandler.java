package rabbit.open.dtx.common.nio.client.ext;

import rabbit.open.dtx.common.nio.client.FutureResult;
import rabbit.open.dtx.common.nio.pub.ChannelAgent;
import rabbit.open.dtx.common.nio.pub.ProtocolData;
import rabbit.open.dtx.common.nio.pub.ext.AbstractNetEventHandler;
import rabbit.open.dtx.common.nio.server.handler.DataDispatcher;

/**
 * 客户端读事件处理器
 * @author xiaoqianbin
 * @date 2019/12/8
 **/
public class ClientNetEventHandler extends AbstractNetEventHandler {

    private DtxChannelAgentPool dtxChannelAgentPool;

    protected DataDispatcher dispatcher;

    public ClientNetEventHandler() {

    }

    public ClientNetEventHandler(DtxChannelAgentPool dtxChannelAgentPool) {
        setChannelAgentPool(dtxChannelAgentPool);
        initDispatcher();
    }

    protected void initDispatcher() {
        dispatcher = new DataDispatcher() {
            @SuppressWarnings("unchecked")
            @Override
            public Object process(ProtocolData protocolData) {
                if (isNotifyMessage(protocolData)) {
                    Object notifyMsg = protocolData.getData();
                    if (getChannelAgentPool().getListenerMap().containsKey(notifyMsg.getClass())) {
                        getChannelAgentPool().getListenerMap().get(notifyMsg.getClass()).onMessageReceived(notifyMsg);
                    } else {
                        logger.info("discard notify info: {}", notifyMsg);
                    }
                } else {
                    FutureResult result = ChannelAgent.findFutureResult(protocolData.getRequestId());
                    if (null != result) {
                        result.wakeUp(protocolData.getData());
                    } else {
                        logger.warn("discard response data, {}", protocolData.getRequestId());
                    }
                }
                return null;
            }
        };
    }

    public void setChannelAgentPool(DtxChannelAgentPool dtxChannelAgentPool) {
        this.dtxChannelAgentPool = dtxChannelAgentPool;
    }

    public DtxChannelAgentPool getChannelAgentPool() {
        return dtxChannelAgentPool;
    }

    /**
     * 是通知消息
     * @param protocolData
     * @author xiaoqianbin
     * @date 2019/12/10
     **/
    protected boolean isNotifyMessage(ProtocolData protocolData) {
        return null == protocolData.getRequestId();
    }

    /**
     * 重写基类，不再返回信息
     * @param protocolData
     * @param agent
     * @author xiaoqianbin
     * @date 2019/12/8
     **/
    @Override
    protected void processData(ProtocolData protocolData, ChannelAgent agent) {
        getDispatcher().process(protocolData);
    }

    @Override
    protected DataDispatcher getDispatcher() {
        return dispatcher;
    }

    @Override
    protected void executeReadTask(Runnable task) {
        task.run();
    }

    @Override
    public void onDisconnected(ChannelAgent agent) {
        logger.info("server[{}] channel closed!", agent.getServerAddr());
    }

    @Override
    protected void wakeUpSelector(ChannelAgent agent) {
        // client是同步读写，所以不需要唤醒
    }
}
