package oracle.test.service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import oracle.test.entity.User;

import org.springframework.stereotype.Service;

import rabbit.open.orm.pool.SessionFactory;
import rabbit.open.orm.spring.SpringDaoAdapter;

@Service
public class OracleUserService extends SpringDaoAdapter<User>{

    @Resource(name="sessionFactory-oracle")
    protected SessionFactory factory;
    
    @PostConstruct
    public void setUp(){
        setSessionFactory(factory);
    }
    
}
