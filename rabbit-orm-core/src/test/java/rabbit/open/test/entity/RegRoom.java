package rabbit.open.test.entity;

import java.util.Date;

import rabbit.open.common.annotation.Column;
import rabbit.open.common.annotation.Entity;
import rabbit.open.common.annotation.PrimaryKey;
import rabbit.open.common.dml.Policy;

@Entity("REG_ROOM")
public class RegRoom {
	
	@PrimaryKey(policy = Policy.AUTOINCREMENT)
	@Column("ID")
	private Integer id;
	
	@Column("USER_ID")
	private RegUser user;
	
	@Column("START_")
	private Date start;

	@Column("END_")
	private Date end;

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public RegUser getUser() {
		return user;
	}

	public void setUser(RegUser user) {
		this.user = user;
	}

	public Date getStart() {
		return start;
	}

	public void setStart(Date start) {
		this.start = start;
	}

	public Date getEnd() {
		return end;
	}

	public void setEnd(Date end) {
		this.end = end;
	}
	
	
}
