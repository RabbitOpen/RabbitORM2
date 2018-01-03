package rabbit.open.test.entity;

import java.util.List;

import rabbit.open.orm.annotation.Column;
import rabbit.open.orm.annotation.Entity;
import rabbit.open.orm.annotation.ManyToMany;
import rabbit.open.orm.annotation.PrimaryKey;
import rabbit.open.orm.dml.policy.Policy;

@Entity("T_ROLE")
public class Role {

	@PrimaryKey(policy=Policy.AUTOINCREMENT)
	@Column("ID")
	private Integer id;
	
	@Column("ROLENAME")
	private String roleName;
	
	@ManyToMany(id="ID", policy=Policy.AUTOINCREMENT, 
            joinTable="T_ROLE_RESOURCE", 
            joinColumn="ROLE_ID", reverseJoinColumn="RESOURCE_ID")
    private List<Resources> resources;

	public Integer getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getRoleName() {
		return roleName;
	}

	public void setRoleName(String roleName) {
		this.roleName = roleName;
	}

	public Role() {
	}

	public Role(int id) {
		super();
		this.id = id;
	}

	public Role(String roleName) {
		super();
		this.roleName = roleName;
	}

    @Override
    public String toString() {
        return "Role [id=" + id + ", roleName=" + roleName + ", resources="
                + resources + "]";
    }

    public void setResources(List<Resources> resources) {
        this.resources = resources;
    }
	
}
