package rabbit.open.dtx.common.test.rpc;

import org.springframework.stereotype.Component;
import rabbit.open.dtx.common.nio.server.DtxServer;
import rabbit.open.dtx.common.nio.server.ext.AbstractServerEventHandler;

import javax.annotation.PreDestroy;
import java.io.IOException;

/**
 * @author xiaoqianbin
 * @date 2019/12/9
 **/
@Component
public class TestServerWrapper {

    DtxServer server;

    public void start(int port, AbstractServerEventHandler handler) throws IOException {
        server = new DtxServer(port, handler);
        server.start();
    }

    @PreDestroy
    public void close() {
        server.shutdown();
    }

    public DtxServer getServer() {
        return server;
    }
}
