package sharding.test.shardquery.entity;

import rabbit.open.orm.core.annotation.Column;
import rabbit.open.orm.core.annotation.Entity;
import rabbit.open.orm.core.annotation.PrimaryKey;
import rabbit.open.orm.core.dml.policy.PagePolicy;
import rabbit.open.orm.core.dml.shard.impl.PrimaryKeyModShardingPolicy;

@Entity(value = "T_BALL_X", shardingPolicy = PrimaryKeyModShardingPolicy.class, 
	pagePolicy = PagePolicy.UNIQUE_INDEX_ORDERED, orderIndexFieldName = "number")
public class Ball {

	@PrimaryKey
	@Column("id")
	private Integer id;
	
	@Column(value = "number", keyWord = true)
	private Integer number;

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

	public Integer getNumber() {
		return number;
	}

	public void setNumber(Integer number) {
		this.number = number;
	}

	
}
