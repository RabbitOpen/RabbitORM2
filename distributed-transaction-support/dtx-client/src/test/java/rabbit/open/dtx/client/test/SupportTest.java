package rabbit.open.dtx.client.test;

import junit.framework.TestCase;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import rabbit.open.dtx.client.test.entity.Enterprise;
import rabbit.open.dtx.client.test.entity.Product;
import rabbit.open.dtx.client.test.impl.FirstEnhancer;
import rabbit.open.dtx.client.test.impl.LastEnhancer;
import rabbit.open.dtx.client.test.service.EnterpriseService;
import rabbit.open.dtx.client.test.service.ProductService;


/**
 * @author xiaoqianbin
 * @date 2019/12/3
 **/
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:support.xml"})
public class SupportTest {

    private Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private HelloService helloService;

    @Autowired
    private EnterpriseService es;

    @Autowired
    private ProductService productService;

    @Test
    public void handlerTest() {
        String name = " zhangsan ";
        String result = helloService.sayHello(name);
        logger.info(result);
        TestCase.assertEquals(FirstEnhancer.class.getSimpleName() + LastEnhancer.class.getSimpleName()
                + "hello" + name, result);
    }

    @Test
    public void dbTest() {
        Enterprise enterprise = new Enterprise();
        enterprise.setName("jdd");
        es.addEnterprise(enterprise);
        TestCase.assertEquals(1, es.getCount());
    }

    @Test
    public void enhancerTest() {
        Product product = new Product();
        product.setName("ZD-0013");
        product.setAddr("chengdu");
        productService.addProduct(product);
    }


}
