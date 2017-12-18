package rabbit.open.test.datasource;

import java.util.concurrent.Semaphore;

import org.springframework.context.support.ClassPathXmlApplicationContext;

import rabbit.open.test.entity.Organization;
import rabbit.open.test.entity.User;
import rabbit.open.test.service.OrganizationService;
import rabbit.open.test.service.UserService;

/**
 * <b>Description:   测试rabbit orm在不同数据源下的速度性能</b>.
 *                   hikari
 *                   dbcp
 *                   c3p0
 *                   rabbit内置数据源
 *                   
 *                   通过切换applicationContext.xml中的数据源类型来测试
 * <b>@author</b>    肖乾斌
 * 
 */
public class TestDatasource {

    public static void main(String[] args) {
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("classpath:applicationContext.xml");
        context.start();
        run(context);
        context.close();
    }

    public static void run(ClassPathXmlApplicationContext context) {
        
        UserService us = context.getBean(UserService.class);
        OrganizationService os = context.getBean(OrganizationService.class);
        
        User user = new User();
        Organization org = new Organization("org", "org");
        os.add(org);
        user.setOrg(org);
        user.setName("sx");
        us.add(user);
        Semaphore s = new Semaphore(0);
        int threadCount = 50;
        long start = System.currentTimeMillis();
        for(int i = 0; i < threadCount; i++){
            new Thread(new Runnable() {
                @Override
                public void run() {
                    for(int i = 0; i < 1000; i++){
                        us.createQuery()
                            .fetch(Organization.class)
                            .addFilter("id", 1L)
                            .execute().list();
                    }
                    s.release();
                }
            }).start();
        }
        try {
            s.acquire(threadCount);
            System.out.println("cost:\t " + (System.currentTimeMillis() - start));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
