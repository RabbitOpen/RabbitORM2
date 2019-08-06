package sqlite.test.service;

import java.util.Date;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import rabbit.open.common.exception.RabbitDMLException;
import sqlite.test.entity.SQLiteUser;

@Service
public class SQLiteUserService extends BaseService<SQLiteUser>{

    @Transactional(transactionManager="transactionManager-sqlite")
    public void rollBakcTest(){
        add(new SQLiteUser("lisi", 10, new Date()));
        add(new SQLiteUser("lisi", 11, new Date()));
        add(new SQLiteUser("lisi", 12, new Date()));
        throw new RabbitDMLException("rollback");
    }

    @Transactional(transactionManager="transactionManager-sqlite")
    public void springTransactionTest(){
        add(new SQLiteUser("lisi", 10, new Date()));
        add(new SQLiteUser("lisi", 10, new Date()));
    }
}
