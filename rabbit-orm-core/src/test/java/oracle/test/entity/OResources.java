package oracle.test.entity;

import rabbit.open.orm.common.dml.Policy;
import rabbit.open.orm.core.annotation.Column;
import rabbit.open.orm.core.annotation.Entity;
import rabbit.open.orm.core.annotation.PrimaryKey;

@Entity("TO_RESOURCE")
public class OResources {
    
    @PrimaryKey(policy=Policy.SEQUENCE, sequence="MYSEQ")
    @Column("ID")
    private Integer id;
    
    @Column("URL_")
    private String url;
    
    public Integer getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;   
    }

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public OResources() {
	}

	public OResources(int id) {
		super();
		this.id = id;
	}

	public OResources(String url) {
		super();
		this.url = url;
	}

	public OResources(int id, String url) {
		super();
		this.id = id;
		this.url = url;
	}

	@Override
	public String toString() {
		return "Resource [id=" + id + ", url=" + url + "]";
	}
	
	
}
