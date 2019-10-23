package oracle.test.entity;

import rabbit.open.orm.common.dml.Policy;
import rabbit.open.orm.core.annotation.Column;
import rabbit.open.orm.core.annotation.Entity;
import rabbit.open.orm.core.annotation.PrimaryKey;

@Entity("OT_DEPARTMENT")
public class ODepartment {

    @PrimaryKey(policy=Policy.SEQUENCE, sequence="MYSEQ")
    @Column("ID")
    private Long id;

    @Column("NAME")
    private String name;
    
    @Column("TEAM_ID")
    private OTeam team;

    /**
     * @param name
     * @param team
     */
    public ODepartment(String name, OTeam team) {
        super();
        this.name = name;
        this.team = team;
    }

    public ODepartment() {
        super();
    }

    @Override
    public String toString() {
        return "Department [id=" + id + ", name=" + name + ", team=" + team
                + "]";
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

    public OTeam getTeam() {
        return team;
    }

    public void setTeam(OTeam team) {
        this.team = team;
    }
    
}
