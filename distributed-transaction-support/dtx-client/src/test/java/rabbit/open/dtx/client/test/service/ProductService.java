package rabbit.open.dtx.client.test.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rabbit.open.dtx.client.context.DistributedTransactionContext;
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
        product.setTxId(DistributedTransactionContext.getDistributedTransactionObject().getTxId());
    }

    @DistributedTransaction
    public void updateProduct(Product product) {
        updateByID(product);
        product.setTxId(DistributedTransactionContext.getDistributedTransactionObject().getTxId());
    }

    @DistributedTransaction
    public long deleteProduct(String id) {
        deleteByID(id);
        return DistributedTransactionContext.getDistributedTransactionObject().getTxId();
    }

}
