package rabbit.open.test.entity.custom;

import java.util.List;

import rabbit.open.orm.common.annotation.Column;
import rabbit.open.orm.common.annotation.Entity;
import rabbit.open.orm.common.annotation.ManyToMany;
import rabbit.open.orm.common.annotation.OneToMany;
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

	@Column("age")
	private Integer age;
	
	@ManyToMany(id = "ID", policy = Policy.AUTOINCREMENT, 
			joinTable = "T_CUSTOM_USER_ROLE", 
			masterFieldName = "name",
			slaveFieldName = "name", 
			joinColumn = "USER_NAME", 
			reverseJoinColumn = "ROLE_NAME")
	private List<CustomRole> roles;

	@OneToMany(joinColumn = "OWNER", masterFieldName = "name")
	private List<CustomCar> cars;

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

	public Integer getAge() {
		return age;
	}

	public void setAge(Integer age) {
		this.age = age;
	}

	public List<CustomRole> getRoles() {
		return roles;
	}

	public void setRoles(List<CustomRole> roles) {
		this.roles = roles;
	}

	public List<CustomCar> getCars() {
		return cars;
	}

	public void setCars(List<CustomCar> cars) {
		this.cars = cars;
	}

}
