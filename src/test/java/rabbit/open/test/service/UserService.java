package rabbit.open.test.service;

import java.util.Date;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import rabbit.open.orm.exception.RabbitDMLException;
import rabbit.open.test.entity.User;

@Service
public class UserService extends BaseService<User>{

    @Transactional
    public void rollBakcTest(){
        add(new User("lisi", 10, new Date()));
        add(new User("lisi", 10, new Date()));
        throw new RabbitDMLException("rollback");
    }

    @Transactional
    public void springTransactionTest(){
        add(new User("lisi", 10, new Date()));
        add(new User("lisi", 10, new Date()));
    }
}
