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
class ClientNetEventHandler extends AbstractNetEventHandler {

    private DtxChannelAgentPool dtxChannelAgentPool;

    private DataDispatcher dispatcher = new DataDispatcher() {

        @Override
        public Object process(ProtocolData protocolData) {
            if (isNotifyMessage(protocolData)) {
                Object notifyMsg = protocolData.getData();
                if (dtxChannelAgentPool.getListenerMap().containsKey(notifyMsg.getClass())) {
                    dtxChannelAgentPool.getListenerMap().get(notifyMsg.getClass()).onMessageReceived(notifyMsg);
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

        /**
         * 是通知消息
         * @param    protocolData
         * @author xiaoqianbin
         * @date 2019/12/10
         **/
        private boolean isNotifyMessage(ProtocolData protocolData) {
            return null == protocolData.getRequestId();
        }
    };

    public ClientNetEventHandler(DtxChannelAgentPool dtxChannelAgentPool) {
        this.dtxChannelAgentPool = dtxChannelAgentPool;
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
