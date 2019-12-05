package rabbit.open.dtx.client.test.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rabbit.open.dtx.client.enhance.ext.DistributedTransaction;
import rabbit.open.dtx.client.test.entity.Product;

/**
 * @author xiaoqianbin
 * @date 2019/12/4
 **/
@Service
public class ProductService extends GenericService<Product> {

    @DistributedTransaction
    @Transactional
    public void addProduct(Product product) {
        add(product);
    }

    @DistributedTransaction
    @Transactional
    public void updateProduct(Product product) {
        updateByID(product);
    }

    @DistributedTransaction(timeoutSeconds = 5)
    @Transactional
    public void deleteProduct(String id) {
        deleteByID(id);
    }

    @DistributedTransaction(timeoutSeconds = 5)
    @Transactional
    public void multiAdd(int count) {
        for (int i = 0; i < count; i++) {
            Product prd = new Product();
            prd.setName("zxs-" + i);
            prd.setAddr("CD-" + i);
            add(prd);
        }
    }
}
