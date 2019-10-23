package rabbit.open.test.entity;

import rabbit.open.orm.common.dml.Policy;
import rabbit.open.orm.core.annotation.Column;
import rabbit.open.orm.core.annotation.Entity;
import rabbit.open.orm.core.annotation.PrimaryKey;

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
    
    
    @Column(value="ORG_ID")
    private Organization org;
    
    @Column("ZONE_ID")
    private Zone zone;

    public UUIDPolicyEntity(String name) {
        this.name = name;
    }

    public UUIDPolicyEntity() {
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

    public Organization getOrg() {
        return org;
    }

    public void setOrg(Organization org) {
        this.org = org;
    }

    public Zone getZone() {
        return zone;
    }

    public void setZone(Zone zone) {
        this.zone = zone;
    }
    
    
}
