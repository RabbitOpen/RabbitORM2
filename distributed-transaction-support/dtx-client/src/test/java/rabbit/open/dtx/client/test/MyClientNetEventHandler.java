package rabbit.open.dtx.client.test;

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
class MyClientNetEventHandler extends AbstractNetEventHandler {

    private DataDispatcher dispatcher = new DataDispatcher() { 

        @Override
        public Object process(ProtocolData protocolData) {
	    	FutureResult result = ChannelAgent.findFutureResult(protocolData.getRequestId());
	        if (null != result) {
	            result.wakeUp(protocolData.getData());
	        } else {
	            logger.warn("discard response data, {}", protocolData.getRequestId());
	        }
            return null;
        }

   
    };

    /**
     * 重写基类，不再返回信息
     * @param    protocolData
     * @param    agent
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
