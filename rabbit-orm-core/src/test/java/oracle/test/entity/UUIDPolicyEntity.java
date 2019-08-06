package oracle.test.entity;

import rabbit.open.common.annotation.Column;
import rabbit.open.common.annotation.Entity;
import rabbit.open.common.annotation.PrimaryKey;
import rabbit.open.common.dml.Policy;

/**
 * <b>Description:  uui策略测试实体 </b>.
 * <b>@author</b>    肖乾斌
 * 
 */
@Entity("UUID_TABLE")
public class UUIDPolicyEntity {
   
    @PrimaryKey(policy=Policy.UUID)
    @Column("ID")
    private String id;
    
    @Column("NAME")
    private String name;

    public UUIDPolicyEntity(String name) {
        this.name = name;
    }

    public UUIDPolicyEntity() {
    }

    @Override
    public String toString() {
        return "UUIDPolicyEntity [id=" + id + ", name=" + name + "]";
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
}
