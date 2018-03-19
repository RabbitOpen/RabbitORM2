package sqlite.test.entity;

import rabbit.open.orm.annotation.Column;
import rabbit.open.orm.annotation.Entity;
import rabbit.open.orm.annotation.PrimaryKey;
import rabbit.open.orm.dml.policy.Policy;

@Entity("T_CAR")
public class SQLiteCar {

    @PrimaryKey(policy=Policy.AUTOINCREMENT)
    @Column("ID")
    private Integer id;
    
    @Column("CAR_NO")
    private String carNo;
    
    @Column("USER_ID")
    private SQLiteUser user;

    @Column("TEAM_ID")
    private SQLiteTeam team;

    @Column("ZONE_ID")
    private SQLiteZone zone;
    
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

    public SQLiteCar(String carNo) {
        super();
        this.carNo = carNo;
    }

    public SQLiteCar() {
        super();
    }

    public SQLiteUser getUser() {
        return user;
    }

    public void setUser(SQLiteUser user) {
        this.user = user;
    }

    public SQLiteCar(String carNo, SQLiteUser user) {
        super();
        this.carNo = carNo;
        this.user = user;
    }

    public SQLiteCar(String carNo, SQLiteTeam team) {
        super();
        this.carNo = carNo;
        this.team = team;
    }

    @Override
    public String toString() {
        return "Car [id=" + id + ", carNo=" + carNo + ", user=" + user + "]";
    }

    public SQLiteTeam getTeam() {
        return team;
    }

    public void setTeam(SQLiteTeam team) {
        this.team = team;
    }

    public SQLiteZone getZone() {
        return zone;
    }

    public void setZone(SQLiteZone zone) {
        this.zone = zone;
    } 
    
    
}
