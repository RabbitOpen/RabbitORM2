package rabbit.open.test.entity.dmlfilter;

import java.util.List;

import rabbit.open.orm.common.dml.Policy;
import rabbit.open.orm.core.annotation.Column;
import rabbit.open.orm.core.annotation.Entity;
import rabbit.open.orm.core.annotation.ManyToMany;
import rabbit.open.orm.core.annotation.PrimaryKey;

@Entity("DML_USER")
public class DMLUser {

	@PrimaryKey(policy = Policy.AUTOINCREMENT)
	@Column("ID")
	private Integer id;

	@Column(value = "NAME", keyWord = true)
	private String name;
	
	@ManyToMany(id="ID", policy=Policy.AUTOINCREMENT, 
			joinTable="DML_USER_ROLE", 
			joinColumn="USER_ID", reverseJoinColumn="ROLE_ID")
	private List<DMLRole> roles;
	
	@ManyToMany(id="ID", policy=Policy.AUTOINCREMENT, 
			joinTable="DML_USER_TEAM", 
			joinColumn="USER_ID", reverseJoinColumn="TEAM_ID")
	private List<DMLTeam> teams;
	
	@Column("HOME")
	private DMLHome home;

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

	public List<DMLRole> getRoles() {
		return roles;
	}

	public void setRoles(List<DMLRole> roles) {
		this.roles = roles;
	}

	public List<DMLTeam> getTeams() {
		return teams;
	}

	public void setTeams(List<DMLTeam> teams) {
		this.teams = teams;
	}

	public DMLHome getHome() {
		return home;
	}

	public void setHome(DMLHome home) {
		this.home = home;
	}
	
	
}
