package rabbit.open.test.entity;

import java.util.List;

import rabbit.open.orm.common.annotation.Column;
import rabbit.open.orm.common.annotation.Entity;
import rabbit.open.orm.common.annotation.OneToMany;
import rabbit.open.orm.common.annotation.PrimaryKey;
import rabbit.open.orm.common.dml.Policy;

@Entity("T_ZONE")
public class Zone {

    @PrimaryKey(policy=Policy.AUTOINCREMENT)
    @Column("ID")
    private Long id;

    @Column("NAME")
    private String name;
    
    @OneToMany(joinColumn="Z_ID")
    List<ZProperty> props;

    public Zone() { }
    
    public Zone(String name) {
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

    public List<ZProperty> getProps() {
        return props;
    }

    public void setProps(List<ZProperty> props) {
        this.props = props;
    }

}