package oracle.test.entity;

import java.util.List;

import rabbit.open.common.annotation.Column;
import rabbit.open.common.annotation.Entity;
import rabbit.open.common.annotation.ManyToMany;
import rabbit.open.common.annotation.OneToMany;
import rabbit.open.common.annotation.PrimaryKey;
import rabbit.open.common.dml.Policy;

@Entity("T_TEAM")
public class Team {
    
    /**
     * 角色列表
     */
    @ManyToMany(id="ID", policy=Policy.SEQUENCE, sequence="MYSEQ",
            joinTable="T_TEAM_ROLE", 
            joinColumn="TEAM_ID", reverseJoinColumn="ROLE_ID")
    private List<Role> roles;
    
    /**
     * 车辆清单
     */
    @OneToMany(joinColumn="TEAM_ID")
    private List<Car> cars;
    

    @Column(value="ORG_ID")
    private Organization org;
    
    
    public List<Role> getRoles() {
        return roles;
    }

    public void setRoles(List<Role> roles) {
        this.roles = roles;
    }

    public Organization getOrg() {
        return org;
    }

    public void setOrg(Organization org) {
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

    public User getLeader() {
        return leader;
    }

    public void setLeader(User leader) {
        this.leader = leader;
    }

    public User getFollower() {
        return follower;
    }

    public void setFollower(User follower) {
        this.follower = follower;
    }

    @PrimaryKey(policy=Policy.SEQUENCE, sequence="MYSEQ")
    @Column("ID")
    private Long id;

    @Column("NAME")
    private String name;
    
    @Column("LEADER")
    private User leader;

    @Column("FOLLOWER")
    private User follower;

    public Team(String name, User leader, User follower) {
        super();
        this.name = name;
        this.leader = leader;
        this.follower = follower;
    }

    public Team() { }

    public List<Car> getCars() {
        return cars;
    }

    public void setCars(List<Car> cars) {
        this.cars = cars;
    }
    
}
