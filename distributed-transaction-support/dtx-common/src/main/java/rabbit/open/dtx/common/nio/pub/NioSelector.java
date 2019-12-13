package rabbit.open.dtx.common.nio.pub;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.spi.SelectorProvider;
import java.util.Set;

/**
 * 原生selector的静态代理, 使用select()同步等操作才能有效监控，使用select(timeout)暂时不监控
 * @author xiaoqianbin
 * @date 2019/12/13
 **/
public class NioSelector extends Selector {

    private static Logger logger = LoggerFactory.getLogger(NioSelector.class);

    // 真实的selector对象
    private Selector realSelector;

    // 发生错误次数
    private int errCount = 0;

    // 错误阈值
    private int errCountThreshold = 100;

    public NioSelector(Selector realSelector) {
        this.realSelector = realSelector;
    }

    public Selector getRealSelector() {
        return realSelector;
    }

    public void reduceErrorCount() {
        errCount--;
    }

    /**
     * epoll 空循环bug检测
     * @author  xiaoqianbin
     * @date    2019/12/13
     **/
    public void epollBugDetection() throws IOException {
        if (errCount < errCountThreshold) {
            return;
        }
        logger.error("epoll bug is detected, errCount: {}", errCount);
        errCount = 0;
        Selector newSelector = Selector.open();
        for (SelectionKey key : realSelector.keys()) {
            if (!key.isValid()) {
                continue;
            }
            int ops = key.interestOps();
            key.channel().register(newSelector, ops, key.attachment());
            key.cancel();
        }
        closeResource(realSelector);
        realSelector = newSelector;
    }

    /**
     * 关闭资源
     * @param	closeable
     * @author  xiaoqianbin
     * @date    2019/12/13
     **/
    public static void closeResource(Closeable closeable) {
        try {
            closeable.close();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    @Override
    public boolean isOpen() {
        return realSelector.isOpen();
    }

    @Override
    public SelectorProvider provider() {
        return realSelector.provider();
    }

    @Override
    public Set<SelectionKey> keys() {
        return realSelector.keys();
    }

    @Override
    public Set<SelectionKey> selectedKeys() {
        return realSelector.selectedKeys();
    }

    @Override
    public int selectNow() throws IOException {
        return realSelector.selectNow();
    }

    @Override
    public int select(long timeout) throws IOException {
        return realSelector.select(timeout);
    }

    @Override
    public int select() throws IOException {
        int cnt = realSelector.select();
        if (0 == cnt) {
            errCount++;
        }
        return cnt;
    }

    @Override
    public Selector wakeup() {
        return realSelector.wakeup();
    }

    @Override
    public void close() throws IOException {
        realSelector.close();
    }
}
