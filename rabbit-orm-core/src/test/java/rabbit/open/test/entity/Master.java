package rabbit.open.test.entity;

import rabbit.open.orm.common.dml.Policy;
import rabbit.open.orm.core.annotation.Column;
import rabbit.open.orm.core.annotation.Entity;
import rabbit.open.orm.core.annotation.ManyToMany;
import rabbit.open.orm.core.annotation.PrimaryKey;

import java.util.List;

@Entity("T_MASTER")
public class Master {

    @PrimaryKey(policy = Policy.AUTOINCREMENT)
    @Column("ID")
    private Long id;

    //名字
    @Column("NAME")
    private String name;

    @ManyToMany(id="ID", policy= Policy.AUTOINCREMENT,
            joinTable="T_MASTER_SLAVE",
            joinColumn="MASTER_ID", reverseJoinColumn="SLAVE_ID", filterColumn = "TYPE")
    private List<Slave> slaves;


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

    public List<Slave> getSlaves() {
        return slaves;
    }

    public void setSlaves(List<Slave> slaves) {
        this.slaves = slaves;
    }
}
