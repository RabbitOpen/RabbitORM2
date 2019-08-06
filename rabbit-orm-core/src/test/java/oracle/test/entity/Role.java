package oracle.test.entity;

import java.util.List;

import rabbit.open.common.annotation.Column;
import rabbit.open.common.annotation.Entity;
import rabbit.open.common.annotation.ManyToMany;
import rabbit.open.common.annotation.PrimaryKey;
import rabbit.open.common.dml.Policy;

@Entity("T_ROLE")
public class Role {

    @PrimaryKey(policy=Policy.SEQUENCE, sequence="MYSEQ")
	@Column("ID")
	private Integer id;
	
	@Column("ROLENAME")
	private String roleName;
	
	@ManyToMany(id="ID", policy=Policy.SEQUENCE, sequence="MYSEQ",
            joinTable="T_ROLE_RESOURCE", 
            joinColumn="ROLE_ID", reverseJoinColumn="RESOURCE_ID")
    private List<Resources> resources;
	
	@Column("ZONE_ID")
	private Zone zone;

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
                + resources + ", zone=" + zone + "]";
    }

    public void setResources(List<Resources> resources) {
        this.resources = resources;
    }
    
    public List<Resources> getResources() {
        return resources;
    }

    public Zone getZone() {
        return zone;
    }

    public void setZone(Zone zone) {
        this.zone = zone;
    }

    
}
