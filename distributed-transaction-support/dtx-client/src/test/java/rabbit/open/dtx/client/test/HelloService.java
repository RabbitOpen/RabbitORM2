package rabbit.open.dtx.client.test;

import org.springframework.stereotype.Component;
import rabbit.open.dtx.client.test.impl.MyAop;

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
}
