package rabbit.open.test.entity;

import rabbit.open.orm.common.annotation.Column;
import rabbit.open.orm.common.annotation.Entity;
import rabbit.open.orm.common.annotation.PrimaryKey;
import rabbit.open.orm.common.dml.Policy;

/**
 * master 和slave 的中间表
 */
@Entity("T_MASTER_SLAVE")
public class MasterSlave {

    @PrimaryKey(policy = Policy.AUTOINCREMENT)
    @Column("ID")
    public Integer id;

    @Column("MASTER_ID")
    private Integer masterId;

    @Column("SLAVE_ID")
    private Integer slaveId;

    @Column("TYPE")
    private String type;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getMasterId() {
        return masterId;
    }

    public void setMasterId(Integer masterId) {
        this.masterId = masterId;
    }

    public Integer getSlaveId() {
        return slaveId;
    }

    public void setSlaveId(Integer slaveId) {
        this.slaveId = slaveId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
