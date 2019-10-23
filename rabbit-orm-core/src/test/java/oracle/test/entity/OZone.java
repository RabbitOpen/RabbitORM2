package oracle.test.entity;

import java.util.List;

import rabbit.open.orm.common.dml.Policy;
import rabbit.open.orm.core.annotation.Column;
import rabbit.open.orm.core.annotation.Entity;
import rabbit.open.orm.core.annotation.OneToMany;
import rabbit.open.orm.core.annotation.PrimaryKey;

@Entity("TO_ZONE")
public class OZone {

    @PrimaryKey(policy=Policy.SEQUENCE, sequence="MYSEQ")
    @Column("ID")
    private Long id;

    @Column("NAME")
    private String name;
    
    @OneToMany(joinColumn="Z_ID")
    List<OZProperty> props;

    /**
     * @param name
     */
    public OZone(String name) {
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

    public List<OZProperty> getProps() {
        return props;
    }

    public void setProps(List<OZProperty> props) {
        this.props = props;
    }

    @Override
    public String toString() {
        return "Zone [id=" + id + ", name=" + name + ", props=" + props + "]";
    }

    public OZone() {
        super();
    }
    
    
}
