package rabbit.open.test.entity;

import rabbit.open.orm.common.annotation.Column;
import rabbit.open.orm.common.annotation.Entity;
import rabbit.open.orm.common.annotation.PrimaryKey;
import rabbit.open.orm.common.dml.Policy;

@Entity("T_RESOURCE")
public class Resources {
    
    @PrimaryKey(policy=Policy.AUTOINCREMENT)
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
