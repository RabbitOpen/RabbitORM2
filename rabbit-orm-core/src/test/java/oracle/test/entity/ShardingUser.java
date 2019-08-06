package oracle.test.entity;

import rabbit.open.orm.common.annotation.Column;
import rabbit.open.orm.common.annotation.Entity;
import rabbit.open.orm.common.annotation.PrimaryKey;
import sharding.test.table.policy.DemoShardingPolicy;

@Entity(value = "T_SHARD_USER", policy=DemoShardingPolicy.class)
public class ShardingUser {

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
    private Region region;

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

    public Region getRegion() {
        return region;
    }

    public void setRegion(Region region) {
        this.region = region;
    }
    
    
    
}
