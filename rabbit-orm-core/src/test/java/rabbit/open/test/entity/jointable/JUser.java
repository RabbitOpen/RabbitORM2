package rabbit.open.test.entity.jointable;

import rabbit.open.orm.common.dml.Policy;
import rabbit.open.orm.core.annotation.Column;
import rabbit.open.orm.core.annotation.Entity;
import rabbit.open.orm.core.annotation.ManyToMany;
import rabbit.open.orm.core.annotation.PrimaryKey;

import java.util.List;

@Entity("Jt_USER")
public class JUser {

	@PrimaryKey(policy = Policy.AUTOINCREMENT)
	@Column("ID")
	private Integer id;
	
	@Column("NAME")
	private String name;
	
	@ManyToMany(id="ID", policy=Policy.AUTOINCREMENT, 
			joinTable="JTJ_USER_ROLE", 
			joinColumn="USER_ID", reverseJoinColumn="ROLE_ID")
	private List<JRole> roles;
	
	@ManyToMany(id="ID", policy=Policy.AUTOINCREMENT, 
			joinTable="JTJ_USER_CAR", 
			joinColumn="USER_ID", reverseJoinColumn="CAR_ID")
	private List<JCar> cars;

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

	public List<JRole> getRoles() {
		return roles;
	}

	public void setRoles(List<JRole> roles) {
		this.roles = roles;
	}

	public List<JCar> getCars() {
		return cars;
	}

	public void setCars(List<JCar> cars) {
		this.cars = cars;
	}
	
}
