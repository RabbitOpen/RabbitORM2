package oracle.test.entity;

import java.util.List;

import rabbit.open.orm.common.dml.Policy;
import rabbit.open.orm.core.annotation.Column;
import rabbit.open.orm.core.annotation.Entity;
import rabbit.open.orm.core.annotation.OneToMany;
import rabbit.open.orm.core.annotation.PrimaryKey;

@Entity("TO_ORG")
public class OracleOrganization {
    
    public List<OProperty> getProps() {
        return props;
    }

    public void setProps(List<OProperty> props) {
        this.props = props;
    }

    @PrimaryKey(policy=Policy.SEQUENCE, sequence="MYSEQ")
    @Column("ID")
    private Long id;

	@Column("ORG_CODE")
	private String orgCode;
	
	@Column("NAME")
	private String name;
	
	@Column("ZONE_ID")
	private OZone zone;

	@Column("LEADER_ID")
	private OLeader leader;

	@OneToMany(joinColumn="ORG_ID")
	List<OProperty> props;
	
	@Column("TEAM_ID")
	private OTeam team;
	
	public OTeam getTeam() {
        return team;
    }

    public void setTeam(OTeam team) {
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

	public OracleOrganization() {
		super();
	}

    public OracleOrganization(String orgCode, String name) {
        super();
        this.orgCode = orgCode;
        this.name = name;
    }

    public OracleOrganization(String orgCode, String name, OZone zone) {
        super();
        this.orgCode = orgCode;
        this.name = name;
        this.zone = zone;
    }

    public OracleOrganization(String orgCode, String name, OZone zone, OLeader leader) {
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

    public OZone getZone() {
        return zone;
    }

    public void setZone(OZone zone) {
        this.zone = zone;
    }

    @Override
    public String toString() {
        return "Organization [id=" + id + ", orgCode=" + orgCode + ", name="
                + name + ", zone=" + zone + ", leader=" + leader + ", props="
                + props + "]";
    }

    public OLeader getLeader() {
        return leader;
    }
    
}
