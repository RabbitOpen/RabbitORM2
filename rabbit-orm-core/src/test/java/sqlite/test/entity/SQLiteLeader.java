package sqlite.test.entity;

import rabbit.open.common.annotation.Column;
import rabbit.open.common.annotation.Entity;
import rabbit.open.common.annotation.PrimaryKey;
import rabbit.open.common.dml.Policy;

@Entity("T_LEADER")
public class SQLiteLeader {

    @PrimaryKey(policy=Policy.AUTOINCREMENT)
    @Column("ID")
    private Long id;

    @Column("NAME")
    private String name;
    
    /**
     * @param name
     */
    public SQLiteLeader(String name) {
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

    public SQLiteLeader() {
        super();
    }
    
    
}
