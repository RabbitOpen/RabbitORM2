package mybatis.test.entity;

import rabbit.open.common.annotation.Column;
import rabbit.open.common.annotation.Entity;
import rabbit.open.common.annotation.PrimaryKey;
import rabbit.open.common.dml.Policy;

@Entity("MORG")
public class MOrg {

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
	
	public MOrg() {
		// TODO Auto-generated constructor stub
	}

	public MOrg(String name) {
		super();
		this.name = name;
	}
}
