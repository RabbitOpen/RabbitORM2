package transaction.rabbit.entity;

import rabbit.open.orm.common.dml.Policy;
import rabbit.open.orm.core.annotation.Column;
import rabbit.open.orm.core.annotation.Entity;
import rabbit.open.orm.core.annotation.PrimaryKey;

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
