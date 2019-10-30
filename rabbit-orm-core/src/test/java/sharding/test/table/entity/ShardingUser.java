package sharding.test.table.entity;

import java.util.List;

import rabbit.open.orm.common.dml.Policy;
import rabbit.open.orm.core.annotation.Column;
import rabbit.open.orm.core.annotation.Entity;
import rabbit.open.orm.core.annotation.ManyToMany;
import rabbit.open.orm.core.annotation.OneToMany;
import rabbit.open.orm.core.annotation.PrimaryKey;
import sharding.test.table.policy.DemoShardingPolicy;

@Entity(value = "T_SHARD_USER", shardingPolicy=DemoShardingPolicy.class)
public class ShardingUser {

    public List<ShardRoom> getRooms() {
        return rooms;
    }

    public void setRooms(List<ShardRoom> rooms) {
        this.rooms = rooms;
    }

    public List<ShardCar> getCars() {
        return cars;
    }

    public void setCars(List<ShardCar> cars) {
        this.cars = cars;
    }

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

    @ManyToMany(id="ID", policy=Policy.AUTOINCREMENT, 
            joinTable="T_SHARD_USER_ROOM", 
            joinColumn="USER_ID", reverseJoinColumn="ROOM_ID")
    private List<ShardRoom> rooms;
    
    @OneToMany(joinColumn="USER_ID")
    private List<ShardCar> cars;
    
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
