package rabbit.open.dtx.server.test;

import org.springframework.stereotype.Component;
import rabbit.open.dtx.common.annotation.DistributedTransaction;

/**
 * @author xiaoqianbin
 * @date 2020/1/9
 **/
@Component
public class HelloService {

    @DistributedTransaction
    public void hello() {

    }

    @DistributedTransaction
    public void hello2() {

    }

    @DistributedTransaction
    public void hello1() {

    }

    @DistributedTransaction
    public void hello3() {

    }

    @DistributedTransaction
    public void hello4() {

    }
}
