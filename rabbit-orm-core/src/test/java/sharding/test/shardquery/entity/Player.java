package sharding.test.shardquery.entity;

import rabbit.open.orm.core.annotation.Column;
import rabbit.open.orm.core.annotation.Entity;
import rabbit.open.orm.core.annotation.PrimaryKey;
import rabbit.open.orm.core.dml.policy.PagePolicy;
import rabbit.open.orm.core.dml.shard.impl.PrimaryKeyModShardingPolicy;

@Entity(value = "T_PLAYER", shardingPolicy = PrimaryKeyModShardingPolicy.class, 
	pagePolicy = PagePolicy.UNIQUE_INDEX_ORDERED)
public class Player {

	@PrimaryKey
	@Column("id")
	private Integer id;

	@Column("playerName")
	private String playerName;

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getPlayerName() {
		return playerName;
	}

	public void setPlayerName(String playerName) {
		this.playerName = playerName;
	}

}
