package rabbit.open.dtx.client.test;

import junit.framework.TestCase;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import rabbit.open.dts.common.utils.ext.KryoObjectSerializer;
import rabbit.open.dtx.client.datasource.proxy.RollbackInfo;
import rabbit.open.dtx.client.test.entity.Enterprise;
import rabbit.open.dtx.client.test.entity.Product;
import rabbit.open.dtx.client.test.entity.RollbackEntity;
import rabbit.open.dtx.client.test.impl.FirstEnhancer;
import rabbit.open.dtx.client.test.impl.LastEnhancer;
import rabbit.open.dtx.client.test.service.EnterpriseService;
import rabbit.open.dtx.client.test.service.ProductService;
import rabbit.open.dtx.client.test.service.RollbackInfoService;

import java.util.List;


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

    @Autowired
    private RollbackInfoService rbs;

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

    /**
     * 插入增强测试
     * @author  xiaoqianbin
     * @date    2019/12/4
     **/
    @Test
    public void insertEnhancerTest() {
        Product product = new Product();
        product.setName("ZD-0013");
        product.setAddr("chengdu");
        productService.addProduct(product);
        RollbackEntity rollbackEntity = rbs.createQuery().addFilter("txGroupId", product.getTxId()).unique();
        KryoObjectSerializer serializer = new KryoObjectSerializer();
        RollbackInfo rollbackInfo = serializer.deserialize(rollbackEntity.getRollbackInfo(), RollbackInfo.class);
        // 回滚信息中包含3个字段
        TestCase.assertEquals(rollbackInfo.getMeta().getColumns().size(), 3);
    }

    /**
     * 更新增强测试
     * @author  xiaoqianbin
     * @date    2019/12/4
     **/
    @Test
    public void updateEnhancerTest() {
        // 创建原始信息
        Product product = new Product();
        product.setName("ZD-0014");
        product.setAddr("CD");
        productService.add(product);

        //更新对象
        product.setName("ZD-0015");
        product.setAddr("CD1");
        productService.updateProduct(product);
        RollbackEntity rollbackEntity = rbs.createQuery().addFilter("txGroupId", product.getTxId()).unique();
        KryoObjectSerializer serializer = new KryoObjectSerializer();
        RollbackInfo rollbackInfo = serializer.deserialize(rollbackEntity.getRollbackInfo(), RollbackInfo.class);
        // 回滚信息中包含3个字段
        TestCase.assertEquals(rollbackInfo.getMeta().getColumns().size(), 2);
    }
}
