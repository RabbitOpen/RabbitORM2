package oracle.test.entity;

import rabbit.open.orm.annotation.Column;
import rabbit.open.orm.annotation.Entity;
import rabbit.open.orm.annotation.PrimaryKey;
import rabbit.open.orm.dml.policy.Policy;

@Entity("T_ORG")
public class Organization {
    
    @PrimaryKey(policy=Policy.SEQUENCE, sequence="MYSEQ")
    @Column("ID")
    private Long id;

	@Column("ORG_CODE")
	private String orgCode;
	
	@Column("NAME")
	private String name;

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

    @Override
    public String toString() {
        return "Organization [id=" + id + ", orgCode=" + orgCode + ", name="
                + name + "]";
    }

    public Organization(String orgCode, String name) {
        super();
        this.orgCode = orgCode;
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
	
	
}
