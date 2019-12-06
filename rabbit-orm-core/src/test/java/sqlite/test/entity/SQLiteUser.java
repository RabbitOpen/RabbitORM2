package sqlite.test.entity;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import rabbit.open.orm.common.dml.Policy;
import rabbit.open.orm.core.annotation.Column;
import rabbit.open.orm.core.annotation.Entity;
import rabbit.open.orm.core.annotation.ManyToMany;
import rabbit.open.orm.core.annotation.OneToMany;
import rabbit.open.orm.core.annotation.PrimaryKey;

@Entity("T_USER_SQLITE")
public class SQLiteUser {

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public SQLiteUser() {
        super();
    }

    public SQLiteUser(String name, Integer age, Date birth) {
        super();
        this.name = name;
        this.age = age;
        this.birth = birth;
    }

    public SQLiteUser(String name, Integer age, Date birth,
            SQLiteOrganization org) {
        super();
        this.name = name;
        this.age = age;
        this.birth = birth;
        this.org = org;
    }
    
    @PrimaryKey(policy=Policy.AUTOINCREMENT)
	@Column("ID")
	private Long id;
	
    @Column(value = "BYTES_DATA", comment = "BLOB类型")
	private byte[] bytes;
    
    public byte[] getBytes() {
		return bytes;
	}

	public void setBytes(byte[] bytes) {
		this.bytes = bytes;
	}

	public SQLiteZone getZone() {
		return zone;
	}

	public void setZone(SQLiteZone zone) {
		this.zone = zone;
	}

	//名字
	@Column("NAME")
	private String name;
	
	//年龄
	@Column(value="AGE", comment = "年龄")
	private Integer age;
	
	@Column(value="SHORT_FIELD")
	private Short shortField;

	@Column(value="FLOAT_FIELD")
	private Float floatField;

	@Column(value="DOUBLE_FIELD")
	private Double doubleField;

	@Column(value="BIG_FIELD")
	private BigDecimal bigField;
	
	//生日
	@Column(value="BIRTH_DAY", pattern="yyyy-MM-dd HH:mm:ss")
	private Date birth;

	//描述，关键字
	@Column(value="DESC", keyWord=true)
	private String desc;

	/**
	 * 角色列表
	 */
	@ManyToMany(id="ID", policy=Policy.AUTOINCREMENT, 
			joinTable="T_USER_ROLE", 
			joinColumn="USER_ID", reverseJoinColumn="ROLE_ID")
	private List<SQLiteRole> roles;
	
	/**
	 * 车辆清单
	 */
	@OneToMany(joinColumn="USER_ID")
	private List<SQLiteCar> cars;

	@Column(value="ORG_ID")
	private SQLiteOrganization org;
	
	@Column("ZONE_ID")
    private SQLiteZone zone;

    public Long getId() {
        return id;
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

    public List<SQLiteRole> getRoles() {
        return roles;
    }

    public void setRoles(List<SQLiteRole> roles) {
        this.roles = roles;
    }

    public List<SQLiteCar> getCars() {
        return cars;
    }

    public void setCars(List<SQLiteCar> cars) {
        this.cars = cars;
    }

    public SQLiteOrganization getOrg() {
        return org;
    }

    public void setOrg(SQLiteOrganization org) {
        this.org = org;
    }

}
