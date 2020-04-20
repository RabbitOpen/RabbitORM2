package sqlite.test.entity;

import rabbit.open.orm.common.dml.Policy;
import rabbit.open.orm.core.annotation.Column;
import rabbit.open.orm.core.annotation.Entity;
import rabbit.open.orm.core.annotation.OneToMany;
import rabbit.open.orm.core.annotation.PrimaryKey;

import java.util.List;

@Entity("T_ORG_SQLITE")
public class SQLiteOrganization {
    
    public List<SQLiteProperty> getProps() {
        return props;
    }

    public void setProps(List<SQLiteProperty> props) {
        this.props = props;
    }

    @PrimaryKey(policy=Policy.AUTOINCREMENT)
    @Column("ID")
    private Long id;

	@Column("ORG_CODE")
	private String orgCode;
	
	@Column("NAME")
	private String name;
	
	@Column("ZONE_ID")
	private SQLiteZone zone;

	@Column("LEADER_ID")
	private SQLiteLeader leader;

	@OneToMany(joinColumn="ORG_ID")
	List<SQLiteProperty> props;
	
	@Column("TEAM_ID")
	private SQLiteTeam team;
	
	public SQLiteTeam getTeam() {
        return team;
    }

    public void setTeam(SQLiteTeam team) {
        this.team = team;
    }

    public String getOrgCode() {
		return orgCode;
	}

	public void setOrgCode(String orgCode) {
		this.orgCode = orgCode;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public SQLiteOrganization() {
		super();
	}

    public SQLiteOrganization(String orgCode, String name) {
        super();
        this.orgCode = orgCode;
        this.name = name;
    }

    public SQLiteOrganization(String orgCode, String name, SQLiteZone zone) {
        super();
        this.orgCode = orgCode;
        this.name = name;
        this.zone = zone;
    }

    public SQLiteOrganization(String orgCode, String name, SQLiteZone zone, SQLiteLeader leader) {
        super();
        this.orgCode = orgCode;
        this.name = name;
        this.zone = zone;
        this.leader = leader;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public SQLiteZone getZone() {
        return zone;
    }

    public void setZone(SQLiteZone zone) {
        this.zone = zone;
    }

    @Override
    public String toString() {
        return "Organization [id=" + id + ", orgCode=" + orgCode + ", name="
                + name + ", zone=" + zone + ", leader=" + leader + ", props="
                + props + "]";
    }

    public SQLiteLeader getLeader() {
        return leader;
    }
    
}
