package rabbit.open.dtx.client.test;

import junit.framework.TestCase;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import rabbit.open.dtx.client.datasource.proxy.RollBackRecord;
import rabbit.open.dtx.client.datasource.proxy.RollbackInfo;
import rabbit.open.dtx.client.net.TransactionMessageHandler;
import rabbit.open.dtx.client.test.entity.Enterprise;
import rabbit.open.dtx.client.test.entity.Product;
import rabbit.open.dtx.client.test.entity.RollbackEntity;
import rabbit.open.dtx.client.test.service.EnterpriseService;
import rabbit.open.dtx.client.test.service.ProductService;
import rabbit.open.dtx.client.test.service.RollbackInfoService;
import rabbit.open.dtx.client.test.service.SimpleTransactionManager;
import rabbit.open.dtx.common.exception.DistributedTransactionException;
import rabbit.open.dtx.common.exception.IsolationException;
import rabbit.open.dtx.common.utils.ext.KryoObjectSerializer;
import rabbit.open.orm.core.dml.meta.MetaData;

import java.sql.Connection;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;


/**
 * @author xiaoqianbin
 * @date 2019/12/3
 **/
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:support.xml"})
public class SupportTest {

    @Autowired
    private EnterpriseService es;

    @Autowired
    private ProductService productService;

    @Autowired
    private RollbackInfoService rbs;

    @Autowired
    private SimpleTransactionManager transactionManger;

    @Test
    public void dbTest() {
        Enterprise enterprise = new Enterprise();
        enterprise.setName("jdd");
        es.addEnterprise(enterprise);
        TestCase.assertEquals(1, es.getCount());
    }

    /**
     * 插入增强测试
     * @author xiaoqianbin
     * @date 2019/12/4
     **/
    @Test
    public void insertEnhancerTest() {
        Product product = new Product();
        product.setName("ZD-0013");
        product.setAddr("chengdu");
        product.setDate(new Date());
        product.setBytes("hello world".getBytes());
        product.setDoubleField(new Double(10.0));
        product.setFloatField(new Float("10.2"));
        product.setGender(Product.Gender.FEMALE);
        productService.addProduct(product);
        RollbackEntity rollbackEntity = rbs.createQuery().addFilter("txBranchId", transactionManger.getLastBranchId()).unique();
        KryoObjectSerializer serializer = new KryoObjectSerializer();
        RollbackInfo rollbackInfo = serializer.deserialize(rollbackEntity.getRollbackInfo(), RollbackInfo.class);
        // 回滚信息中包含3个字段
        TestCase.assertEquals(rollbackInfo.getMeta().getColumns().size(), 7);

        // 验证回滚失败
        TransactionMessageHandler rollbackFailedHandler = new TransactionMessageHandler() {
            @Override
            protected boolean doRollback(List<RollBackRecord> records, Connection conn) throws SQLException {
                throw new RuntimeException("mock a roll back exception");
            }
        };
        try {
            rollbackFailedHandler.rollback(transactionManger.getApplicationName(), transactionManger.getLastBranchId() - 1, transactionManger.getLastBranchId());
            throw new RuntimeException("impossible");
        } catch (DistributedTransactionException e) {
            // 验证抛出异常
            // 验证回滚失败
            TestCase.assertNotNull(productService.getByID(product.getId()));
        }

        // 开始回滚
        TransactionMessageHandler handler = new TransactionMessageHandler();
        handler.rollback(transactionManger.getApplicationName(), transactionManger.getLastBranchId() - 1, transactionManger.getLastBranchId());

        // 验证回滚已经删除掉数据了
        TestCase.assertNull(productService.getByID(product.getId()));
    }

    /**
     * 更新增强测试
     * @author xiaoqianbin
     * @date 2019/12/4
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

        Product byID = productService.getByID(product.getId());
        TestCase.assertEquals(byID.getName(), product.getName());
        TestCase.assertEquals(byID.getAddr(), product.getAddr());

        //！！！！ 因为单元测试是串行运行，transactionManger.getLastBranchId()不存在并发问题，所以可以这样用
        RollbackEntity rollbackEntity = rbs.createQuery().addFilter("txBranchId", transactionManger.getLastBranchId()).unique();
        KryoObjectSerializer serializer = new KryoObjectSerializer();
        RollbackInfo rollbackInfo = serializer.deserialize(rollbackEntity.getRollbackInfo(), RollbackInfo.class);
        // 回滚信息中包含2个字段
        TestCase.assertEquals(rollbackInfo.getMeta().getColumns().size(), 2);

        // 开始回滚
        TransactionMessageHandler handler = new TransactionMessageHandler();
        handler.rollback(transactionManger.getApplicationName(), transactionManger.getLastBranchId() - 1, transactionManger.getLastBranchId());
        // 验证回滚
        byID = productService.getByID(product.getId());
        TestCase.assertEquals(byID.getName(), "ZD-0014");
        TestCase.assertEquals(byID.getAddr(), "CD");

        //更新对象
        product.setName("ZD-0016");
        product.setAddr("CD16");
        productService.updateProduct(product);

        byID = productService.getByID(product.getId());
        TestCase.assertEquals(byID.getName(), product.getName());
        TestCase.assertEquals(byID.getAddr(), product.getAddr());
        handler = new TransactionMessageHandler();
        handler.commit();

        // 验证提交
        byID = productService.getByID(product.getId());
        TestCase.assertEquals(byID.getName(), product.getName());
        TestCase.assertEquals(byID.getAddr(), product.getAddr());
    }

    @Test
    public void strictUpdateEnhancerTest() {
        // 创建原始信息
        Product product = new Product();
        product.setName("ZD-0024");
        product.setAddr("CD");
        productService.add(product);

        //更新对象
        product.setName("ZD-0025");
        product.setAddr("CD1");
        productService.strictUpdateProduct(product);

        Product byID = productService.getByID(product.getId());
        TestCase.assertEquals(byID.getName(), product.getName());
        TestCase.assertEquals(byID.getAddr(), product.getAddr());

        //！！！！ 因为单元测试是串行运行，transactionManger.getLastBranchId()不存在并发问题，所以可以这样用
        RollbackEntity rollbackEntity = rbs.createQuery().addFilter("txBranchId", transactionManger.getLastBranchId()).unique();
        KryoObjectSerializer serializer = new KryoObjectSerializer();
        RollbackInfo rollbackInfo = serializer.deserialize(rollbackEntity.getRollbackInfo(), RollbackInfo.class);
        // 回滚信息中包含2个字段
        TestCase.assertEquals(rollbackInfo.getMeta().getColumns().size(), 2);

        // 开始回滚
        TransactionMessageHandler handler = new TransactionMessageHandler();
        handler.rollback(transactionManger.getApplicationName(), transactionManger.getLastBranchId() - 1, transactionManger.getLastBranchId());
        // 验证回滚
        byID = productService.getByID(product.getId());
        TestCase.assertEquals(byID.getName(), "ZD-0024");
        TestCase.assertEquals(byID.getAddr(), "CD");

        //更新对象
        product.setName("ZD-0016");
        product.setAddr("CD16");
        productService.updateProduct(product);

        byID = productService.getByID(product.getId());
        TestCase.assertEquals(byID.getName(), product.getName());
        TestCase.assertEquals(byID.getAddr(), product.getAddr());
        handler = new TransactionMessageHandler();
        handler.commit();

        // 验证提交
        byID = productService.getByID(product.getId());
        TestCase.assertEquals(byID.getName(), product.getName());
        TestCase.assertEquals(byID.getAddr(), product.getAddr());
    }

    /**
     * 删除增强测试
     * @author xiaoqianbin
     * @date 2019/12/4
     **/
    @Test
    public void deleteEnhancerTest() {
        // 创建原始信息
        Product product = new Product();
        product.setName("ZD-0214");
        product.setAddr("CDX");
        product.setDate(new Date());
        productService.add(product);
        productService.deleteProduct(product.getId());

        // 验证数据已经删除了
        TestCase.assertNull(productService.getByID(product.getId()));

        RollbackEntity rollbackEntity = rbs.createQuery().addFilter("txBranchId", transactionManger.getLastBranchId()).unique();
        KryoObjectSerializer serializer = new KryoObjectSerializer();
        RollbackInfo rollbackInfo = serializer.deserialize(rollbackEntity.getRollbackInfo(), RollbackInfo.class);
        TestCase.assertEquals(rollbackInfo.getOriginalData().get(0).get(MetaData.getCachedFieldsMeta(Product.class,
                "name").getColumn().value()), product.getName());
        TestCase.assertEquals(rollbackInfo.getOriginalData().get(0).get(MetaData.getCachedFieldsMeta(Product.class,
                "addr").getColumn().value()), product.getAddr());
        // 开始回滚
        TransactionMessageHandler handler = new TransactionMessageHandler();
        handler.rollback(transactionManger.getApplicationName(), transactionManger.getLastBranchId() - 1, transactionManger.getLastBranchId());

        // 验证空回滚
        productService.deleteProduct(product.getId() + 1010000);
        rollbackEntity = rbs.createQuery().addFilter("txBranchId", transactionManger.getLastBranchId()).unique();
        rollbackInfo = serializer.deserialize(rollbackEntity.getRollbackInfo(), RollbackInfo.class);
        TestCase.assertTrue(rollbackInfo.getOriginalData().isEmpty());
        handler.rollback(transactionManger.getApplicationName(), transactionManger.getLastBranchId() - 1, transactionManger.getLastBranchId());


        // 验证数据已经回滚了
        Product byID = productService.getByID(product.getId());
        TestCase.assertEquals(byID.getAddr(), product.getAddr());
        TestCase.assertEquals(byID.getName(), product.getName());
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        TestCase.assertEquals(sdf.format(byID.getDate()), sdf.format(product.getDate()));
    }

    @Test
    public void jdbcTest() {
        Long id = productService.jdbcAdd();
        TestCase.assertNotNull(id);
        TestCase.assertNotNull(productService.getByID(id));
        RollbackEntity rollbackEntity = rbs.createQuery().addFilter("txBranchId", transactionManger.getLastBranchId()).unique();
        KryoObjectSerializer serializer = new KryoObjectSerializer();
        RollbackInfo rollbackInfo = serializer.deserialize(rollbackEntity.getRollbackInfo(), RollbackInfo.class);
        TestCase.assertNotNull(rollbackInfo);
        // 开始回滚
        TransactionMessageHandler handler = new TransactionMessageHandler();
        handler.rollback(transactionManger.getApplicationName(), transactionManger.getLastBranchId() - 1, transactionManger.getLastBranchId());
        TestCase.assertNull(productService.getByID(id));

    }

    @Test
    public void multiAddTest() {
        long before = rbs.createQuery().count();
        int count = 5;
        productService.multiAdd(count);
        TestCase.assertEquals(rbs.createQuery().count() - before, count);
    }

    /**
     * 由于productService.isolationErrorTest没有加事务注解，又使用了读提交隔离级别，所以会抛异常
     * @author xiaoqianbin
     * @date 2019/12/23
     **/
    @Test
    public void isolationErrorTest() {
        try {
            Product product = new Product();
            product.setId(10099L);
            //更新对象
            product.setName("ZD-0015");
            product.setAddr("CD1");
            productService.isolationErrorTest(product);
            throw new RuntimeException("");
        } catch (Exception e) {
            TestCase.assertEquals(IsolationException.class, e.getClass());
        }
    }


}
