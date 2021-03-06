package rabbit.open.dtx.client.test.entity;

import rabbit.open.orm.common.dml.Policy;
import rabbit.open.orm.core.annotation.Column;
import rabbit.open.orm.core.annotation.Entity;
import rabbit.open.orm.core.annotation.PrimaryKey;

/**
 * @author xiaoqianbin
 * @date 2019/12/3
 **/
@Entity("T_ENTERPRISE")
public class Enterprise {

    @PrimaryKey(policy= Policy.AUTOINCREMENT)
    @Column("ID")
    private Long id;

    @Column(value = "ENTERPRISE_NAME", comment = "名字")
    private String name;

    @Column(value = "ADDR", comment = "地址")
    private String addr;

    @Column(value = "OWNER", comment = "拥有者")
    private String owner;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
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
