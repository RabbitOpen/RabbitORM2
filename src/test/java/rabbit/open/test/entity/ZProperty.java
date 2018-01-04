package rabbit.open.test.entity;

import rabbit.open.orm.annotation.Column;
import rabbit.open.orm.annotation.Entity;
import rabbit.open.orm.annotation.PrimaryKey;
import rabbit.open.orm.dml.policy.Policy;

@Entity("T_ZPROPERTY")
public class ZProperty {

    @Column("Z_ID")
    private Zone zone;
    
    @PrimaryKey(policy=Policy.AUTOINCREMENT)
    @Column("ID")
    private Long id;
    
    @Column("NAME")
    private String pname;

    public ZProperty(Long zId, String pname) {
        super();
        this.zone = new Zone();
        zone.setId(zId);
        this.pname = pname;
    }

    public ZProperty() {
        super();
    }


    @Override
    public String toString() {
        return "ZProperty [zone=" + zone + ", id=" + id + ", pname=" + pname
                + "]";
    }

    
}
