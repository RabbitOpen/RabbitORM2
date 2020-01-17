package rabbit.open.dtx.rpc.test.server;

import rabbit.open.orm.common.dml.Policy;
import rabbit.open.orm.core.annotation.Column;
import rabbit.open.orm.core.annotation.Entity;
import rabbit.open.orm.core.annotation.PrimaryKey;

import java.io.Serializable;

/**
 * @author xiaoqianbin
 * @date 2020/1/16
 **/
@Entity("DTX_USER")
@SuppressWarnings("serial")
public class User implements Serializable {

    @PrimaryKey(policy = Policy.AUTOINCREMENT)
    @Column("ID")
    private Long id;

    @Column(value = "USER_NAME", length = 20)
    private String name;

    @Column(value = "AGE")
    private Integer age;

    public User() {
    }

    public User(String name, Integer age) {
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
}
