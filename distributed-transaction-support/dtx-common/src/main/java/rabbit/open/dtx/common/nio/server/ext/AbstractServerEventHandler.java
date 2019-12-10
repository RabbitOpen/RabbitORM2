package rabbit.open.dtx.common.nio.server.ext;

import rabbit.open.dtx.common.nio.pub.ChannelAgent;
import rabbit.open.dtx.common.nio.pub.ext.AbstractNetEventHandler;
import rabbit.open.dtx.common.nio.server.DtxServer;
import rabbit.open.dtx.common.nio.server.handler.ApplicationDataHandler;
import rabbit.open.dtx.common.nio.server.handler.DataDispatcher;

/**
 * 抽象服务端时间处理器
 * @author xiaoqianbin
 * @date 2019/12/9
 **/
public abstract class AbstractServerEventHandler extends AbstractNetEventHandler {

    // 单例dispatcher
    protected DataDispatcher dispatcher = new DataDispatcher();

    protected DtxServer dtxServer;

    @Override
    protected DataDispatcher getDispatcher() {
        return dispatcher;
    }

    @Override
    protected void executeReadTask(Runnable task) {
        task.run();
    }

    @Override
    protected void wakeUpSelector(ChannelAgent agent) {
        getDtxServer().wakeup();
    }

    /**
     * 移除缓存的agent
     * @param	agent
     * @author  xiaoqianbin
     * @date    2019/12/10
     **/
    @Override
    public void onDisconnected(ChannelAgent agent) {
        super.onDisconnected(agent);
        ApplicationDataHandler.removeAgent(agent);
    }

    @Override
    protected void closeAgentChannel(ChannelAgent agent) {
        agent.setClosed(true);
        getDtxServer().closeChannelKey(agent.getSelectionKey());
    }

    /**
     * 获取监听的DtxServer对象
     * @author  xiaoqianbin
     * @date    2019/12/9
     **/
    protected DtxServer getDtxServer() {
        return dtxServer;
    }

    public void setDtxServer(DtxServer dtxServer) {
        this.dtxServer = dtxServer;
    }

    /**
     * dtxServer 关闭后置事件
     * @author  xiaoqianbin
     * @date    2019/12/9
     **/
    public abstract void onServerClosed();
}
