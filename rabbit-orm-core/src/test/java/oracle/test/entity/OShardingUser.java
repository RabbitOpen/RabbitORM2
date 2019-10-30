package oracle.test.entity;

import rabbit.open.orm.core.annotation.Column;
import rabbit.open.orm.core.annotation.Entity;
import rabbit.open.orm.core.annotation.PrimaryKey;
import sharding.test.table.policy.DemoShardingPolicy;

@Entity(value = "TO_SHARD_USER", policy = DemoShardingPolicy.class)
public class OShardingUser {

    @PrimaryKey()
    @Column("ID")
    private Long id;
    
    //名字
    @Column("NAME")
    private String name;

    @Column("GENDER")
    private String gender;
    
    @Column("AGE")
    private Long age;
    
    @Column("REGION")
    private ORegion region;

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public Long getAge() {
        return age;
    }

    public void setAge(Long age) {
        this.age = age;
    }

    public ORegion getRegion() {
        return region;
    }

    public void setRegion(ORegion region) {
        this.region = region;
    }
    
    
    
}
