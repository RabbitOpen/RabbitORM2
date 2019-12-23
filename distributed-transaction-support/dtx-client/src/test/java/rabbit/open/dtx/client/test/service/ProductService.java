package rabbit.open.dtx.client.test.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rabbit.open.dtx.client.test.entity.Product;
import rabbit.open.dtx.common.nio.client.annotation.DistributedTransaction;
import rabbit.open.dtx.common.nio.client.annotation.Isolation;
import rabbit.open.orm.core.dml.meta.MetaData;

import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

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
        product.setId(null);
        add(product);
    }


    @DistributedTransaction
    public Long jdbcAdd() {
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet rs = null;
        try {
            connection = sessionFactory.getConnection();
            connection.setAutoCommit(false);
            statement = connection.prepareStatement("insert into " + MetaData.getMetaByClass(Product.class).getTableName()
                    + "(ADDR, OWNER) VALUES (?, ?)");
            statement.setString(1, "CDX");
            statement.setString(2, "CDX");
            statement.executeUpdate();
            rs = statement.getGeneratedKeys();
            if (rs.next()) {
                BigDecimal id = rs.getBigDecimal(1);
                return id.longValue();
            }
            rs.close();
            statement.close();
            connection.commit();
        } catch (Exception e) {
            try {
                connection.rollback();
            } catch (SQLException e1) {
            }
        } finally {
            close(rs);
            close(statement);
            close(connection);
        }
        return null;
    }

    private void close(AutoCloseable connection) {
        try {
            if (null != connection) {
                connection.close();
            }
        } catch (Exception e) {

        }
    }

    @DistributedTransaction(isolation = Isolation.READ_COMMITTED)
    @Transactional
    public void updateProduct(Product product) {
        updateByID(product);
    }

    @DistributedTransaction(transactionTimeoutSeconds = 5)
    @Transactional
    public void deleteProduct(Serializable id) {
        deleteByID(id);
    }

    @DistributedTransaction(transactionTimeoutSeconds = 5)
    @Transactional
    public void multiAdd(int count) {
        for (int i = 0; i < count; i++) {
            Product prd = new Product();
            prd.setName("zxs-" + i);
            prd.setAddr("CD-" + i);
            add(prd);
        }
    }

    @DistributedTransaction(isolation = Isolation.READ_COMMITTED)
    public void isolationErrorTest(Product product) {
        updateByID(product);
    }
}
