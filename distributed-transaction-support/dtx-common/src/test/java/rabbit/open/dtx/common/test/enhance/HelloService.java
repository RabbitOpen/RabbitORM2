package rabbit.open.dtx.common.test.enhance;

import org.springframework.stereotype.Component;
import rabbit.open.dtx.common.annotation.DistributedTransaction;

/**
 * @author xiaoqianbin
 * @date 2019/12/3
 **/
@Component
public class HelloService {

    @MyAop
    public String sayHello(String username) {
        return "hello" + username;
    }


    @DistributedTransaction
    public void hello() {

    }
}
