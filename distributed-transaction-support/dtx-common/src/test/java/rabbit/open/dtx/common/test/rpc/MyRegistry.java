package rabbit.open.dtx.common.test.rpc;

import org.springframework.stereotype.Component;
import rabbit.open.dtx.common.nio.client.Node;
import rabbit.open.dtx.common.nio.client.Registry;

import java.util.Arrays;
import java.util.List;

/**
 * @author xiaoqianbin
 * @date 2019/12/9
 **/
@Component("myRegistry")
public class MyRegistry extends Registry {

    @Override
    public List<Node> getNodes() {
        return Arrays.asList(new Node("localhost", 10086));
    }
}
