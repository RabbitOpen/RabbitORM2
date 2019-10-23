package oracle.test.entity;

import java.util.Date;

import rabbit.open.orm.common.dml.Policy;
import rabbit.open.orm.core.annotation.Column;
import rabbit.open.orm.core.annotation.Entity;
import rabbit.open.orm.core.annotation.PrimaryKey;

@Entity("REG_ROOM_ORACLE")
public class ORegRoom {
	
	@PrimaryKey(policy = Policy.SEQUENCE, sequence="MYSEQ")
	@Column("ID")
	private Integer id;
	
	@Column("USER_ID")
	private ORegUser user;
	
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

	public ORegUser getUser() {
		return user;
	}

	public void setUser(ORegUser user) {
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
