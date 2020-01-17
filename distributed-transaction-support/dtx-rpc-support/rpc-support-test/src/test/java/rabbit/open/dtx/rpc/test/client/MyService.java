package rabbit.open.dtx.rpc.test.client;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rabbit.open.dtx.common.annotation.DistributedTransaction;
import rabbit.open.dtx.rpc.test.server.User;
import rabbit.open.dtx.rpc.test.server.UserService;

/**
 * @author xiaoqianbin
 * @date 2020/1/16
 **/
@Service
public class MyService {

    @Autowired
    UserService us;

    @DistributedTransaction
    @Transactional
    public Long clientAppAddUser(String name, Integer age) {
        return us.addUser(name, age);
    }

    public User getUserById(Long id) {
        return us.getUserById(id);
    }

    @DistributedTransaction
    @Transactional
    public Long addUserAndRollback(String name, Integer age) {
        throw new RollbackException(us.addUser(name, age));
    }


}
