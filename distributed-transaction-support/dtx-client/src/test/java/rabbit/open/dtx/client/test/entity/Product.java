package rabbit.open.dtx.client.test.entity;

import rabbit.open.orm.common.dml.Policy;
import rabbit.open.orm.core.annotation.Column;
import rabbit.open.orm.core.annotation.Entity;
import rabbit.open.orm.core.annotation.PrimaryKey;

/**
 * @author xiaoqianbin
 * @date 2019/12/3
 **/
@Entity("T_PRODUCT_INFO")
public class Product {

    @PrimaryKey(policy= Policy.UUID)
    @Column("ID")
    private String id;

    @Column(value = "ENTERPRISE_NAME", comment = "名字")
    private String name;

    @Column(value = "ADDR", comment = "地址")
    private String addr;

    @Column(value = "OWNER", comment = "拥有者")
    private String owner;

    private Long txId;

    public Long getTxId() {
        return txId;
    }

    public void setTxId(Long txId) {
        this.txId = txId;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddr() {
        return addr;
    }

    public void setAddr(String addr) {
        this.addr = addr;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }
}
