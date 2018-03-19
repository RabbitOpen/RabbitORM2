package sqlite.test.entity;

import java.util.List;

import rabbit.open.orm.annotation.Column;
import rabbit.open.orm.annotation.Entity;
import rabbit.open.orm.annotation.OneToMany;
import rabbit.open.orm.annotation.PrimaryKey;
import rabbit.open.orm.dml.policy.Policy;

@Entity("T_ZONE")
public class SQLiteZone {

    @PrimaryKey(policy=Policy.AUTOINCREMENT)
    @Column("ID")
    private Long id;

    @Column("NAME")
    private String name;
    
    @OneToMany(joinColumn="Z_ID")
    List<SQLiteZProperty> props;

    /**
     * @param name
     */
    public SQLiteZone(String name) {
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

    public List<SQLiteZProperty> getProps() {
        return props;
    }

    public void setProps(List<SQLiteZProperty> props) {
        this.props = props;
    }

    @Override
    public String toString() {
        return "Zone [id=" + id + ", name=" + name + ", props=" + props + "]";
    }

    public SQLiteZone() {
        super();
    }
    
    
}
