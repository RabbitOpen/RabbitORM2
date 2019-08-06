package oracle.test.entity;

import java.util.List;

import rabbit.open.orm.common.annotation.Column;
import rabbit.open.orm.common.annotation.Entity;
import rabbit.open.orm.common.annotation.OneToMany;
import rabbit.open.orm.common.annotation.PrimaryKey;
import rabbit.open.orm.common.dml.Policy;

@Entity("T_ORG")
public class Organization {
    
    public List<Property> getProps() {
        return props;
    }

    public void setProps(List<Property> props) {
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
	private Zone zone;

	@Column("LEADER_ID")
	private Leader leader;

	@OneToMany(joinColumn="ORG_ID")
	List<Property> props;
	
	@Column("TEAM_ID")
	private Team team;
	
	public Team getTeam() {
        return team;
    }

    public void setTeam(Team team) {
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

	public Organization() {
		super();
	}

    public Organization(String orgCode, String name) {
        super();
        this.orgCode = orgCode;
        this.name = name;
    }

    public Organization(String orgCode, String name, Zone zone) {
        super();
        this.orgCode = orgCode;
        this.name = name;
        this.zone = zone;
    }

    public Organization(String orgCode, String name, Zone zone, Leader leader) {
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

    public Zone getZone() {
        return zone;
    }

    public void setZone(Zone zone) {
        this.zone = zone;
    }

    @Override
    public String toString() {
        return "Organization [id=" + id + ", orgCode=" + orgCode + ", name="
                + name + ", zone=" + zone + ", leader=" + leader + ", props="
                + props + "]";
    }

    public Leader getLeader() {
        return leader;
    }
    
}
