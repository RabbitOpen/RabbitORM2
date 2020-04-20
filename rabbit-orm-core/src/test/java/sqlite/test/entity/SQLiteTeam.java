package sqlite.test.entity;

import rabbit.open.orm.common.dml.Policy;
import rabbit.open.orm.core.annotation.*;

import java.util.List;

@Entity("T_TEAM_SQLITE")
public class SQLiteTeam {
    
    /**
     * 角色列表
     */
    @ManyToMany(id="ID", policy=Policy.AUTOINCREMENT, sequence="MYSEQ",
            joinTable="T_TEAM_ROLE", 
            joinColumn="TEAM_ID", reverseJoinColumn="ROLE_ID")
    private List<SQLiteRole> roles;
    
    /**
     * 车辆清单
     */
    @OneToMany(joinColumn="TEAM_ID")
    private List<SQLiteCar> cars;
    

    @Column(value="ORG_ID")
    private SQLiteOrganization org;
    
    
    public List<SQLiteRole> getRoles() {
        return roles;
    }

    public void setRoles(List<SQLiteRole> roles) {
        this.roles = roles;
    }

    public SQLiteOrganization getOrg() {
        return org;
    }

    public void setOrg(SQLiteOrganization org) {
        this.org = org;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public SQLiteUser getLeader() {
        return leader;
    }

    public void setLeader(SQLiteUser leader) {
        this.leader = leader;
    }

    public SQLiteUser getFollower() {
        return follower;
    }

    public void setFollower(SQLiteUser follower) {
        this.follower = follower;
    }

    @PrimaryKey(policy=Policy.AUTOINCREMENT)
    @Column("ID")
    private Long id;

    @Column("NAME")
    private String name;
    
    @Column("LEADER")
    private SQLiteUser leader;

    @Column("FOLLOWER")
    private SQLiteUser follower;

    public SQLiteTeam(String name, SQLiteUser leader, SQLiteUser follower) {
        super();
        this.name = name;
        this.leader = leader;
        this.follower = follower;
    }

    public SQLiteTeam() { }

    public List<SQLiteCar> getCars() {
        return cars;
    }

    public void setCars(List<SQLiteCar> cars) {
        this.cars = cars;
    }
    
}
