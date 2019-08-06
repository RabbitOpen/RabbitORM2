package sharding.test.db.entity;

import rabbit.open.orm.common.annotation.Column;
import rabbit.open.orm.common.annotation.Entity;
import rabbit.open.orm.common.annotation.PrimaryKey;
import rabbit.open.orm.common.dml.Policy;

/**
 * <b>Description  读写分离的用户表</b>
 */
@Entity("T_USER")
public class RWUser {

    @PrimaryKey(policy=Policy.AUTOINCREMENT)
    @Column("ID")
    private Long id;
    
    //名字
    @Column("NAME")
    private String name;
    
    //年龄
    @Column(value="AGE")
    private Integer age;

    public RWUser(String name, Integer age) {
        this.name = name;
        this.age = age;
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

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public RWUser() {
        super();
    }
    
    
}
