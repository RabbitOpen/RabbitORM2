package rabbit.open.test.datasource;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.springframework.context.support.ClassPathXmlApplicationContext;

import rabbit.open.test.entity.User;
import rabbit.open.test.service.UserService;

/**
 * <b>Description: 测试monitor</b>. <b>@author</b> 肖乾斌
 * 
 */
public class TestMonitor {

    public static void main(String[] args) {

        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
                "classpath:applicationContext.xml");
        context.start();
        run(context);
        // 重启db
        try {
            System.in.read();
            UserService us = context.getBean(UserService.class);
            List<User> list = us.createQuery().page(0, 10).execute().list();
            list.forEach(u -> System.out.println(u));
        } catch (IOException e) {
            e.printStackTrace();
        }
        context.close();
    }

    public static void run(ClassPathXmlApplicationContext context) {
        UserService us = context.getBean(UserService.class);
        User user = new User();
        user.setName("sx");
        us.add(user);
        // 添加用户角色之间的映射关系
        Semaphore s = new Semaphore(0);
        int threadCount = 20;
        long start = System.currentTimeMillis();
        for (int i = 0; i < threadCount; i++) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    for (int i = 0; i < 100; i++) {
                        query(us);
                        if (i > 80) {
                            sleep1s();
                        }
                    }
                    s.release();
                }

                public void query(UserService us) {
                    try {
                        us.createQuery().page(0, 10).execute().list();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                public void sleep1s() {
                    try {
                        new Semaphore(0).tryAcquire(1, TimeUnit.SECONDS);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        }
        try {
            s.acquire(threadCount);
            System.out.println("cost:\t "
                    + (System.currentTimeMillis() - start));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
