package rabbit.open.dts.test.entity;

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

    //名字
    @Column("ENTERPRISE_NAME")
    private String name;

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
}
