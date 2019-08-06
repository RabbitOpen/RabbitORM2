package sharding.test.table.entity;

import java.util.List;

import rabbit.open.orm.common.annotation.Column;
import rabbit.open.orm.common.annotation.Entity;
import rabbit.open.orm.common.annotation.ManyToMany;
import rabbit.open.orm.common.annotation.PrimaryKey;
import rabbit.open.orm.common.dml.Policy;

@Entity("T_SHAR_DEPT")
public class ShardDept {

    @PrimaryKey()
    @Column("ID")
    private Long id;
    
    @ManyToMany(id="ID", policy=Policy.AUTOINCREMENT, 
            joinTable="T_SHARD_DEPT_USER", 
            joinColumn="DEPT_ID", reverseJoinColumn="USER_ID")
    private List<ShardingUser> users;
    
    @Column("USER_ID")
    private ShardingUser user;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public List<ShardingUser> getUsers() {
        return users;
    }

    public void setUsers(List<ShardingUser> users) {
        this.users = users;
    }

    public ShardingUser getUser() {
        return user;
    }

    public void setUser(ShardingUser user) {
        this.user = user;
    }

    /**
     * @param id
     * @param user
     */
    public ShardDept(Long id, ShardingUser user) {
        super();
        this.id = id;
        this.user = user;
    }

    /**
     * 
     */
    public ShardDept() {
        super();
    }
    
}
