package sharding.test.shardquery.entity;

import rabbit.open.orm.core.annotation.Column;
import rabbit.open.orm.core.annotation.Entity;
import rabbit.open.orm.core.annotation.PrimaryKey;
import rabbit.open.orm.core.dml.shard.impl.PrimaryKeyModShardingPolicy;

@Entity(value = "T_PRODUCT_ORDER", shardingPolicy = PrimaryKeyModShardingPolicy.class)
public class Order {

	@PrimaryKey
	@Column("id")
	private Integer id;
	
	@Column("username")
	private String username;

	private Integer count;

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public Integer getCount() {
		return count;
	}

	public void setCount(Integer count) {
		this.count = count;
	}
}
