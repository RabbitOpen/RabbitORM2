package rabbit.open.test.entity.custom;

import rabbit.open.orm.common.annotation.Column;
import rabbit.open.orm.common.annotation.Entity;
import rabbit.open.orm.common.annotation.PrimaryKey;
import rabbit.open.orm.common.dml.Policy;

@Entity("CUSTOMER_CAR")
public class CustomCar {

	@PrimaryKey(policy = Policy.AUTOINCREMENT)
	@Column("ID")
	private Integer id;

	@Column(value = "CAR_NO")
	private String carNo;

	@Column(value = "OWNER", keyWord = true, joinFieldName = "name")
	private CustomUser owner;

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getCarNo() {
		return carNo;
	}

	public void setCarNo(String carNo) {
		this.carNo = carNo;
	}

	public CustomUser getOwner() {
		return owner;
	}

	public void setOwner(CustomUser owner) {
		this.owner = owner;
	}

}
