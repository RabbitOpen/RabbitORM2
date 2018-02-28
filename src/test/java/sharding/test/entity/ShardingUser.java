package sharding.test.entity;

import rabbit.open.orm.annotation.Column;
import rabbit.open.orm.annotation.Entity;
import rabbit.open.orm.annotation.PrimaryKey;
import sharding.test.policy.DemoShardingPolicy;

@Entity(value = "T_SHARD_USER", policy=DemoShardingPolicy.class)
public class ShardingUser {

    @PrimaryKey()
    @Column("ID")
    private Long id;
    
    //名字
    @Column("NAME")
    private String name;

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
    
    
}
