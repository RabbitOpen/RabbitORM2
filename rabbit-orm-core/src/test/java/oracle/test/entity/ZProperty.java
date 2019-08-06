package oracle.test.entity;

import rabbit.open.orm.common.annotation.Column;
import rabbit.open.orm.common.annotation.Entity;
import rabbit.open.orm.common.annotation.PrimaryKey;
import rabbit.open.orm.common.dml.Policy;

@Entity("T_ZPROPERTY")
public class ZProperty {

    @Column("Z_ID")
    private Zone zone;
    
    @PrimaryKey(policy=Policy.SEQUENCE, sequence="MYSEQ")
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
