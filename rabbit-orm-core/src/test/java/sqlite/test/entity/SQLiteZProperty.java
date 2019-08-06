package sqlite.test.entity;

import rabbit.open.common.annotation.Column;
import rabbit.open.common.annotation.Entity;
import rabbit.open.common.annotation.PrimaryKey;
import rabbit.open.common.dml.Policy;

@Entity("T_ZPROPERTY")
public class SQLiteZProperty {

    @Column("Z_ID")
    private SQLiteZone zone;
    
    @PrimaryKey(policy=Policy.AUTOINCREMENT)
    @Column("ID")
    private Long id;
    
    @Column("NAME")
    private String pname;

    public SQLiteZProperty(Long zId, String pname) {
        super();
        this.zone = new SQLiteZone();
        zone.setId(zId);
        this.pname = pname;
    }

    public SQLiteZProperty() {
        super();
    }


    @Override
    public String toString() {
        return "ZProperty [zone=" + zone + ", id=" + id + ", pname=" + pname
                + "]";
    }

    
}
