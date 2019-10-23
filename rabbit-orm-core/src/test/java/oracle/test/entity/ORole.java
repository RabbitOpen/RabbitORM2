package oracle.test.entity;

import java.util.List;

import rabbit.open.orm.common.dml.Policy;
import rabbit.open.orm.core.annotation.Column;
import rabbit.open.orm.core.annotation.Entity;
import rabbit.open.orm.core.annotation.ManyToMany;
import rabbit.open.orm.core.annotation.PrimaryKey;

@Entity("TO_ROLE")
public class ORole {

    @PrimaryKey(policy=Policy.SEQUENCE, sequence="MYSEQ")
	@Column("ID")
	private Integer id;
	
	@Column("ROLENAME")
	private String roleName;
	
	@ManyToMany(id="ID", policy=Policy.SEQUENCE, sequence="MYSEQ",
            joinTable="T_ROLE_RESOURCE", 
            joinColumn="ROLE_ID", reverseJoinColumn="RESOURCE_ID")
    private List<OResources> resources;
	
	@Column("ZONE_ID")
	private OZone zone;

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

	public ORole() {
	}

	public ORole(int id) {
		super();
		this.id = id;
	}

	public ORole(String roleName) {
		super();
		this.roleName = roleName;
	}

    @Override
    public String toString() {
        return "Role [id=" + id + ", roleName=" + roleName + ", resources="
                + resources + ", zone=" + zone + "]";
    }

    public void setResources(List<OResources> resources) {
        this.resources = resources;
    }
    
    public List<OResources> getResources() {
        return resources;
    }

    public OZone getZone() {
        return zone;
    }

    public void setZone(OZone zone) {
        this.zone = zone;
    }

    
}
