package rabbit.open.test.entity.jointable;

import rabbit.open.orm.common.annotation.Column;
import rabbit.open.orm.common.annotation.Entity;
import rabbit.open.orm.common.annotation.PrimaryKey;
import rabbit.open.orm.common.dml.Policy;

@Entity("JT_CAR")
public class JCar {

	@PrimaryKey(policy = Policy.AUTOINCREMENT)
	@Column("ID")
	private Integer id;
	
	@Column("NAME")
	private String name;

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	
	
}
