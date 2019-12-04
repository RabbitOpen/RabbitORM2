package rabbit.open.dtx.client.test.service;

import rabbit.open.orm.core.dml.SessionFactory;
import rabbit.open.orm.core.spring.SpringDaoAdapter;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

/**
 * @author xiaoqianbin
 * @date 2019/12/3
 **/
public abstract class GenericService<T> extends SpringDaoAdapter<T> {

    @Resource(name = "sessionFactory")
    private SessionFactory factory;

    @PostConstruct
    public void init() {
        setSessionFactory(factory);
    }

}
