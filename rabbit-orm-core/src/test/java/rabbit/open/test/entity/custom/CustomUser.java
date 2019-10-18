package rabbit.open.test.entity.custom;

import rabbit.open.orm.common.annotation.Column;
import rabbit.open.orm.common.annotation.Entity;
import rabbit.open.orm.common.annotation.PrimaryKey;
import rabbit.open.orm.common.dml.Policy;


@Entity("CUSTOMER_USER")
public class CustomUser {

	@PrimaryKey(policy = Policy.AUTOINCREMENT)
	@Column("ID")
	private Integer id;

	@Column(value = "NAME", keyWord = true)
	private String name;
	
	@Column(value = "ORG_NAME", joinFieldName = CustomOrg.NAME)
	private CustomOrg org;

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

	public CustomOrg getOrg() {
		return org;
	}

	public void setOrg(CustomOrg org) {
		this.org = org;
	}
	
	
}
