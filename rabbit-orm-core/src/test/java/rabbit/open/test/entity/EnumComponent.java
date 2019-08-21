package rabbit.open.test.entity;

import rabbit.open.orm.common.annotation.Column;
import rabbit.open.orm.common.annotation.Entity;
import rabbit.open.orm.common.annotation.PrimaryKey;

@Entity("T_ENUM")
public class EnumComponent {

	@PrimaryKey
	@Column(value = "COMPONENT_CODE", comment = "主键", length = 20)
	private ComponentCodeEnum componentCode;

	public enum ComponentCodeEnum {
		Hello, World;
	}
	
	@Column("ROLE_ID")
	private EnumRole role;

	public ComponentCodeEnum getComponentCode() {
		return componentCode;
	}

	public void setComponentCode(ComponentCodeEnum componentCode) {
		this.componentCode = componentCode;
	}

	public EnumRole getRole() {
		return role;
	}

	public void setRole(EnumRole role) {
		this.role = role;
	}
	
}
