package sqlite.test.entity;

import rabbit.open.orm.common.dml.Policy;
import rabbit.open.orm.core.annotation.Column;
import rabbit.open.orm.core.annotation.Entity;
import rabbit.open.orm.core.annotation.PrimaryKey;

/**
 * <b>Description:  uui策略测试实体 </b>.
 * <b>@author</b>    肖乾斌
 * 
 */
@Entity("UUID_TABLE_SQLITE")
public class SQLiteUUIDPolicyEntity {
   
    @PrimaryKey(policy=Policy.UUID)
    @Column("ID")
    private String id;
    
    @Column("NAME")
    private String name;

    public SQLiteUUIDPolicyEntity(String name) {
        this.name = name;
    }

    public SQLiteUUIDPolicyEntity() {
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
