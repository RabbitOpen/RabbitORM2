package oracle.test.entity;

import rabbit.open.orm.common.annotation.Column;
import rabbit.open.orm.common.annotation.Entity;
import rabbit.open.orm.common.annotation.PrimaryKey;
import rabbit.open.orm.common.dml.Policy;


@Entity("CUSTOMER_ORG")
public class CustomOrg {

	@PrimaryKey(policy=Policy.SEQUENCE, sequence="MYSEQ")
	@Column("ID")
	private Integer id;

	@Column(value = "NAME", keyWord = true)
	private String name;
	
	public static final String NAME = "name";

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
