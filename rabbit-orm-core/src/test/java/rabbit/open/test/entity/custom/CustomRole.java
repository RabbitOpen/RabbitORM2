package rabbit.open.test.entity.custom;

import rabbit.open.orm.common.annotation.Column;
import rabbit.open.orm.common.annotation.Entity;
import rabbit.open.orm.common.annotation.PrimaryKey;
import rabbit.open.orm.common.dml.Policy;


@Entity("CUSTOMER_ROLE")
public class CustomRole {

	@PrimaryKey(policy = Policy.AUTOINCREMENT)
	@Column("ID")
	private Integer id;

	@Column(value = "NAME", keyWord = true)
	private String name;

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	
	
}
