package rabbit.open.test.entity;

import rabbit.open.orm.core.annotation.Column;
import rabbit.open.orm.core.annotation.Entity;
import rabbit.open.orm.core.annotation.PrimaryKey;

@Entity("T_ENUM_ROLE")
public class EnumRole {

	@PrimaryKey
	@Column(value = "Role_CODE", comment = "主键")
	private EnumRoleEnum roleCode;

	public enum EnumRoleEnum {
		Hello, World;
	}
	
	@Column("AGE")
	private Integer age;

	public EnumRoleEnum getRoleCode() {
		return roleCode;
	}

	public void setRoleCode(EnumRoleEnum roleCode) {
		this.roleCode = roleCode;
	}

	public Integer getAge() {
		return age;
	}

	public void setAge(Integer age) {
		this.age = age;
	}
	
}
