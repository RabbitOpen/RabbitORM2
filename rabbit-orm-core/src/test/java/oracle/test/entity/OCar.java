package oracle.test.entity;

import rabbit.open.orm.common.dml.Policy;
import rabbit.open.orm.core.annotation.Column;
import rabbit.open.orm.core.annotation.Entity;
import rabbit.open.orm.core.annotation.PrimaryKey;

@Entity("OT_CAR")
public class OCar {

    @PrimaryKey(policy=Policy.SEQUENCE, sequence="MYSEQ")
    @Column("ID")
    private Integer id;
    
    @Column("CAR_NO")
    private String carNo;
    
    @Column("USER_ID")
    private OUser user;

    @Column("TEAM_ID")
    private OTeam team;

    @Column("ZONE_ID")
    private OZone zone;
    
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getCarNo() {
        return carNo;
    }

    public void setCarNo(String carNo) {
        this.carNo = carNo;
    }

    public OCar(String carNo) {
        super();
        this.carNo = carNo;
    }

    public OCar() {
        super();
    }

    public OUser getUser() {
        return user;
    }

    public void setUser(OUser user) {
        this.user = user;
    }

    public OCar(String carNo, OUser user) {
        super();
        this.carNo = carNo;
        this.user = user;
    }

    public OCar(String carNo, OTeam team) {
        super();
        this.carNo = carNo;
        this.team = team;
    }

    @Override
    public String toString() {
        return "Car [id=" + id + ", carNo=" + carNo + ", user=" + user + "]";
    }

    public OTeam getTeam() {
        return team;
    }

    public void setTeam(OTeam team) {
        this.team = team;
    }

    public OZone getZone() {
        return zone;
    }

    public void setZone(OZone zone) {
        this.zone = zone;
    } 
    
    
}
