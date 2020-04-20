package rabbit.open.test.entity.dmlfilter;

import rabbit.open.orm.common.dml.Policy;
import rabbit.open.orm.core.annotation.Column;
import rabbit.open.orm.core.annotation.Entity;
import rabbit.open.orm.core.annotation.ManyToMany;
import rabbit.open.orm.core.annotation.PrimaryKey;

import java.util.List;

@Entity("DML_ROLE")
public class DMLRole {

	@PrimaryKey(policy = Policy.AUTOINCREMENT)
	@Column("ID")
	private Integer id;

	@Column(value = "NAME", keyWord = true)
	private String name;

	@ManyToMany(id="ID", policy=Policy.AUTOINCREMENT, 
			joinTable="DML_ROLE_RESOURCE", 
			joinColumn="ROLE_ID", reverseJoinColumn="RESOURCE_ID")
	private List<DMLResource> resources;
	
	@Column("ORG")
	private DMLOrg org;
	
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

	public List<DMLResource> getResources() {
		return resources;
	}

	public void setResources(List<DMLResource> resources) {
		this.resources = resources;
	}

	public DMLOrg getOrg() {
		return org;
	}

	public void setOrg(DMLOrg org) {
		this.org = org;
	}

}
