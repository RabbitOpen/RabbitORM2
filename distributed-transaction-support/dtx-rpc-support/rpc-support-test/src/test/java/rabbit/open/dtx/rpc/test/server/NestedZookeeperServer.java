package rabbit.open.dtx.rpc.test.server;

import org.apache.zookeeper.server.ZooKeeperServerMain;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import java.io.File;

/**
 * 内置zookeeper
 * @author xiaoqianbin
 * @date 2020/1/16
 **/
public class NestedZookeeperServer extends ZooKeeperServerMain {

    private Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    public void shutdown() {
        super.shutdown();
    }

    @PostConstruct
    public void start() {
        new Thread(() -> {
            try {
                super.initializeAndRun(new String[]{new File(NestedZookeeperServer.class.getClassLoader().getResource("zoo.cfg").toURI()).getAbsolutePath()});
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        }).start();
    }



}
