package oracle.test.entity;

import rabbit.open.orm.common.dml.Policy;
import rabbit.open.orm.core.annotation.Column;
import rabbit.open.orm.core.annotation.Entity;
import rabbit.open.orm.core.annotation.PrimaryKey;

@Entity("TO_ZPROPERTY")
public class OZProperty {

    @Column("Z_ID")
    private OZone zone;
    
    @PrimaryKey(policy=Policy.SEQUENCE, sequence="MYSEQ")
    @Column("ID")
    private Long id;
    
    @Column("NAME")
    private String pname;

    public OZProperty(Long zId, String pname) {
        super();
        this.zone = new OZone();
        zone.setId(zId);
        this.pname = pname;
    }

    public OZProperty() {
        super();
    }


    @Override
    public String toString() {
        return "ZProperty [zone=" + zone + ", id=" + id + ", pname=" + pname
                + "]";
    }

    
}
