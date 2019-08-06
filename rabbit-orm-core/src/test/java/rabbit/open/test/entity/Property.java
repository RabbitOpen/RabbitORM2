package rabbit.open.test.entity;

import rabbit.open.common.annotation.Column;
import rabbit.open.common.annotation.Entity;
import rabbit.open.common.annotation.PrimaryKey;
import rabbit.open.common.dml.Policy;

@Entity("T_PROPERTY")
public class Property {

    @Column("ORG_ID")
    private Organization org;
    
    @PrimaryKey(policy=Policy.AUTOINCREMENT)
    @Column("ID")
    private Long id;
    
    @Column("NAME")
    private String pname;

    /**
     * @param org
     * @param id
     * @param pname
     */
    public Property(Long orgId, String pname) {
        super();
        Organization organization = new Organization();
        organization.setId(orgId);
        this.org = organization;
        this.pname = pname;
    }
    

    /**
     * 
     */
    public Property() {
        super();
    }



    @Override
    public String toString() {
        return "Property [org=" + org + ", id=" + id + ", pname=" + pname + "]";
    }
    
}
