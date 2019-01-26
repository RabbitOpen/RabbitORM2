package transaction.entity;

import rabbit.open.orm.annotation.Column;
import rabbit.open.orm.annotation.Entity;
import rabbit.open.orm.annotation.PrimaryKey;
import rabbit.open.orm.dml.policy.Policy;

@Entity("XUSER")
public class TUser {

	@PrimaryKey(policy=Policy.AUTOINCREMENT)
	@Column("ID")
	private Long id;
	
    //名字
	@Column("NAME")
	private String name;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	
	public TUser() {
		// TODO Auto-generated constructor stub
	}

	public TUser(String name) {
		super();
		this.name = name;
	}
	
}
