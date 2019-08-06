package oracle.test.entity;

import rabbit.open.common.annotation.Column;
import rabbit.open.common.annotation.Entity;
import rabbit.open.common.annotation.PrimaryKey;
import rabbit.open.common.dml.Policy;

@Entity("T_RESOURCE")
public class Resources {
    
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

	public Resources() {
	}

	public Resources(int id) {
		super();
		this.id = id;
	}

	public Resources(String url) {
		super();
		this.url = url;
	}

	public Resources(int id, String url) {
		super();
		this.id = id;
		this.url = url;
	}

	@Override
	public String toString() {
		return "Resource [id=" + id + ", url=" + url + "]";
	}
	
	
}
