package rabbit.open.dtx.client.test.service;

import org.springframework.stereotype.Service;
import rabbit.open.dtx.client.enhance.ext.DistributedTransaction;
import rabbit.open.dtx.client.test.entity.Product;

/**
 * @author xiaoqianbin
 * @date 2019/12/4
 **/
@Service
public class ProductService extends GenericService<Product> {

    @DistributedTransaction
    public void addProduct(Product product) {
        add(product);
    }

    @DistributedTransaction
    public void updateProduct(Product product) {
        updateByID(product);
    }

    @DistributedTransaction
    public void deleteProduct(String id) {
        deleteByID(id);
    }

}
