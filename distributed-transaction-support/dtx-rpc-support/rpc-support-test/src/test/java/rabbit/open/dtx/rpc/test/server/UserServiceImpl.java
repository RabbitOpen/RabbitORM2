package rabbit.open.dtx.rpc.test.server;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rabbit.open.dtx.common.annotation.DistributedTransaction;
import rabbit.open.orm.core.dml.SessionFactory;
import rabbit.open.orm.core.spring.SpringDaoAdapter;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

/**
 * @author xiaoqianbin
 * @date 2020/1/16
 **/
@Service("UserServiceImpl")
public class UserServiceImpl extends SpringDaoAdapter<User> implements UserService {

    @Resource(name = "sessionFactory")
    private SessionFactory factory;

    @PostConstruct
    public void init() {
        setSessionFactory(factory);
    }

    @DistributedTransaction
    @Transactional
    @Override
    public Long addUser(String name, Integer age) {
        User user = new User(name, age);
        add(user);
        return user.getId();
    }

    @Override
    public User getUserById(Long id) {
        return getByID(id);
    }
}
