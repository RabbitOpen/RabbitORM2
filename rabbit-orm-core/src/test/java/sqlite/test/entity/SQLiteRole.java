package sqlite.test.entity;

import rabbit.open.orm.common.dml.Policy;
import rabbit.open.orm.core.annotation.Column;
import rabbit.open.orm.core.annotation.Entity;
import rabbit.open.orm.core.annotation.ManyToMany;
import rabbit.open.orm.core.annotation.PrimaryKey;

import java.util.List;

@Entity("T_ROLE_SQLITE")
public class SQLiteRole {

	@PrimaryKey(policy=Policy.AUTOINCREMENT)
	@Column("ID")
	private Integer id;
	
	@Column("ROLENAME")
	private String roleName;
	
	@ManyToMany(id="ID", policy=Policy.AUTOINCREMENT, 
            joinTable="T_ROLE_RESOURCE", 
            joinColumn="ROLE_ID", reverseJoinColumn="RESOURCE_ID")
    private List<SQLiteResources> resources;
	
	@Column("ZONE_ID")
	private SQLiteZone zone;

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

	public SQLiteRole() {
	}

	public SQLiteRole(int id) {
		super();
		this.id = id;
	}

	public SQLiteRole(String roleName) {
		super();
		this.roleName = roleName;
	}

    @Override
    public String toString() {
        return "Role [id=" + id + ", roleName=" + roleName + ", resources="
                + resources + ", zone=" + zone + "]";
    }

    public void setResources(List<SQLiteResources> resources) {
        this.resources = resources;
    }
    
    public List<SQLiteResources> getResources() {
        return resources;
    }

    public SQLiteZone getZone() {
        return zone;
    }

    public void setZone(SQLiteZone zone) {
        this.zone = zone;
    }

    
}
