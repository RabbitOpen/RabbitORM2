package oracle.test.entity;

import rabbit.open.orm.common.annotation.Column;
import rabbit.open.orm.common.annotation.Entity;
import rabbit.open.orm.common.annotation.PrimaryKey;
import rabbit.open.orm.common.dml.Policy;

@Entity("T_LEADER")
public class Leader {

    @PrimaryKey(policy=Policy.SEQUENCE, sequence="MYSEQ")
    @Column("ID")
    private Long id;

    @Column("NAME")
    private String name;
    
    /**
     * @param name
     */
    public Leader(String name) {
        super();
        this.name = name;
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

    @Override
    public String toString() {
        return "Zone [id=" + id + ", name=" + name + "]";
    }

    public Leader() {
        super();
    }
    
    
}
