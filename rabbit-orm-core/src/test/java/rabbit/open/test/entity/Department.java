package rabbit.open.test.entity;

import rabbit.open.orm.common.dml.Policy;
import rabbit.open.orm.core.annotation.Column;
import rabbit.open.orm.core.annotation.Entity;
import rabbit.open.orm.core.annotation.PrimaryKey;

@Entity("T_DEPARTMENT")
public class Department {

    @PrimaryKey(policy=Policy.AUTOINCREMENT)
    @Column("ID")
    private Long id;

    @Column("NAME")
    private String name;
    
    @Column("TEAM_ID")
    private Team team;

    /**
     * @param name
     * @param team
     */
    public Department(String name, Team team) {
        super();
        this.name = name;
        this.team = team;
    }

    public Department() {
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

    public Team getTeam() {
        return team;
    }

    public void setTeam(Team team) {
        this.team = team;
    }
    
}
