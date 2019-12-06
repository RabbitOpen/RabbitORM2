package oracle.test.entity;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import rabbit.open.orm.common.dml.Policy;
import rabbit.open.orm.core.annotation.Column;
import rabbit.open.orm.core.annotation.Entity;
import rabbit.open.orm.core.annotation.ManyToMany;
import rabbit.open.orm.core.annotation.OneToMany;
import rabbit.open.orm.core.annotation.PrimaryKey;

@Entity("TO_USER")
public class OUser {

	public OUser() {
		super();
	}

	public OUser(String name, Integer age, Date birth) {
		super();
		this.name = name;
		this.age = age;
		this.birth = birth;
	}

	public OUser(String name, Integer age, Date birth, OracleOrganization org) {
		super();
		this.name = name;
		this.age = age;
		this.birth = birth;
		this.org = org;
	}

	@PrimaryKey(policy = Policy.SEQUENCE, sequence = "MYSEQ")
	@Column("ID")
	private Long id;

	// 名字
	@Column("NAME")
	private String name;

	// 年龄
	@Column(value = "AGE")
	private Integer age;

	@Column(value = "SHORT_FIELD")
	private Short shortField;

	@Column(value = "FLOAT_FIELD", comment = "浮点型字段")
	private Float floatField;

	@Column(value = "DOUBLE_FIELD")
	private Double doubleField;

	@Column(value = "BIG_FIELD")
	private BigDecimal bigField;

	// 生日
	@Column(value = "BIRTH_DAY", pattern = "yyyy-MM-dd HH:mm:ss")
	private Date birth;

	@Column(value = "BYTES_DATA", comment = "BLOB类型")
	private byte[] bytes;

	// 描述，关键字
	@Column(value = "DESC", keyWord = true)
	private String desc;

	// 描述，关键字
	@Column(value = "index", keyWord = true)
	private String index;

	/**
	 * 角色列表
	 */
	@ManyToMany(id = "ID", policy = Policy.SEQUENCE, sequence = "MYSEQ", joinTable = "T_USER_ROLE", joinColumn = "USER_ID", reverseJoinColumn = "ROLE_ID")
	private List<ORole> roles;

	/**
	 * 车辆清单
	 */
	@OneToMany(joinColumn = "USER_ID")
	private List<OCar> cars;

	@Column(value = "ORG_ID", comment = "组织机构")
	private OracleOrganization org;

	@Column("ZONE_ID")
	private OZone zone;

	public Long getId() {
		return id;
	}

	public byte[] getBytes() {
		return bytes;
	}

	public void setBytes(byte[] bytes) {
		this.bytes = bytes;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Integer getAge() {
		return age;
	}

	public void setAge(Integer age) {
		this.age = age;
	}

	public Short getShortField() {
		return shortField;
	}

	public void setShortField(Short shortField) {
		this.shortField = shortField;
	}

	public Float getFloatField() {
		return floatField;
	}

	public void setFloatField(Float floatField) {
		this.floatField = floatField;
	}

	public Double getDoubleField() {
		return doubleField;
	}

	public void setDoubleField(Double doubleField) {
		this.doubleField = doubleField;
	}

	public BigDecimal getBigField() {
		return bigField;
	}

	public void setBigField(BigDecimal bigField) {
		this.bigField = bigField;
	}

	public Date getBirth() {
		return birth;
	}

	public void setBirth(Date birth) {
		this.birth = birth;
	}

	public List<ORole> getRoles() {
		return roles;
	}

	public void setRoles(List<ORole> roles) {
		this.roles = roles;
	}

	public List<OCar> getCars() {
		return cars;
	}

	public void setCars(List<OCar> cars) {
		this.cars = cars;
	}

	public OracleOrganization getOrg() {
		return org;
	}

	public void setOrg(OracleOrganization org) {
		this.org = org;
	}

	@Override
	public String toString() {
		return "User [id=" + id + ", name=" + name + ", age=" + age + ", shortField=" + shortField + ", floatField="
				+ floatField + ", doubleField=" + doubleField + ", bigField=" + bigField + ", birth=" + birth
				+ ", desc=" + desc + ", roles=" + roles + ", cars=" + cars + ", org=" + org + "]";
	}

	public String getIndex() {
		return index;
	}

	public void setIndex(String index) {
		this.index = index;
	}

	public String getDesc() {
		return desc;
	}

	public void setDesc(String desc) {
		this.desc = desc;
	}

}
