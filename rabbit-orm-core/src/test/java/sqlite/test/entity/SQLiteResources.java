package sqlite.test.entity;

import rabbit.open.orm.common.dml.Policy;
import rabbit.open.orm.core.annotation.Column;
import rabbit.open.orm.core.annotation.Entity;
import rabbit.open.orm.core.annotation.PrimaryKey;

@Entity("T_RESOURCE_SQLITE")
public class SQLiteResources {
    
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

	public SQLiteResources() {
	}

	public SQLiteResources(int id) {
		super();
		this.id = id;
	}

	public SQLiteResources(String url) {
		super();
		this.url = url;
	}

	public SQLiteResources(int id, String url) {
		super();
		this.id = id;
		this.url = url;
	}

	@Override
	public String toString() {
		return "Resource [id=" + id + ", url=" + url + "]";
	}
	
	
}
