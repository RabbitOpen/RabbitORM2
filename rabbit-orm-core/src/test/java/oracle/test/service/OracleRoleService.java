package oracle.test.service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import oracle.test.entity.Role;

import org.springframework.stereotype.Service;

import rabbit.open.orm.pool.SessionFactory;
import rabbit.open.orm.spring.SpringDaoAdapter;

@Service
public class OracleRoleService extends SpringDaoAdapter<Role>{

    @Resource(name="sessionFactory-oracle")
    protected SessionFactory factory;
    
    @PostConstruct
    public void setUp(){
        setSessionFactory(factory);
    }
    
}
