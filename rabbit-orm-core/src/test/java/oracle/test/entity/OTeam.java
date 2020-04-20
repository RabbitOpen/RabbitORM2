package oracle.test.entity;

import rabbit.open.orm.common.dml.Policy;
import rabbit.open.orm.core.annotation.*;

import java.util.List;

@Entity("TO_TEAM")
public class OTeam {
    
    /**
     * 角色列表
     */
    @ManyToMany(id="ID", policy=Policy.SEQUENCE, sequence="MYSEQ",
            joinTable="T_TEAM_ROLE", 
            joinColumn="TEAM_ID", reverseJoinColumn="ROLE_ID")
    private List<ORole> roles;
    
    /**
     * 车辆清单
     */
    @OneToMany(joinColumn="TEAM_ID")
    private List<OCar> cars;
    

    @Column(value="ORG_ID")
    private OracleOrganization org;
    
    
    public List<ORole> getRoles() {
        return roles;
    }

    public void setRoles(List<ORole> roles) {
        this.roles = roles;
    }

    public OracleOrganization getOrg() {
        return org;
    }

    public void setOrg(OracleOrganization org) {
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

    public OUser getLeader() {
        return leader;
    }

    public void setLeader(OUser leader) {
        this.leader = leader;
    }

    public OUser getFollower() {
        return follower;
    }

    public void setFollower(OUser follower) {
        this.follower = follower;
    }

    @PrimaryKey(policy=Policy.SEQUENCE, sequence="MYSEQ")
    @Column("ID")
    private Long id;

    @Column("NAME")
    private String name;
    
    @Column("LEADER")
    private OUser leader;

    @Column("FOLLOWER")
    private OUser follower;

    public OTeam(String name, OUser leader, OUser follower) {
        super();
        this.name = name;
        this.leader = leader;
        this.follower = follower;
    }

    public OTeam() { }

    public List<OCar> getCars() {
        return cars;
    }

    public void setCars(List<OCar> cars) {
        this.cars = cars;
    }
    
}
