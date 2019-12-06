package reg.db2.entity;

import java.util.Date;
import java.util.List;

import rabbit.open.orm.common.dml.Policy;
import rabbit.open.orm.core.annotation.Column;
import rabbit.open.orm.core.annotation.Entity;
import rabbit.open.orm.core.annotation.OneToMany;
import rabbit.open.orm.core.annotation.PrimaryKey;

@Entity("REG_USER_DB2")
public class RegUser {

	@PrimaryKey(policy = Policy.AUTOINCREMENT)
	@Column("ID")
	private Integer id;

	@Column("START_")
	private Date start;

	@Column("END_")
	private Date end;

	@Column(value = "FROM_", comment = "开始")
	private Integer from;

	@Column("TO_")
	private Integer to;
	
	@Column("BYTES_X")
	private byte[] bytes;

	public byte[] getBytes() {
		return bytes;
	}

	public void setBytes(byte[] bytes) {
		this.bytes = bytes;
	}

	@OneToMany(joinColumn = "USER_ID")
	private List<RegRoom> rooms;

	// 名字
	@Column("NAME_")
	private String name;

	// 相同名字的人的个数
	@Column(value = "COUNT(1)", dynamic = true)
	private Integer countOfName;

	// 统计from的和
	@Column(value = "SUM(${from})", dynamic = true)
	private Integer sumOfFrom;

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public Date getStart() {
		return start;
	}

	public void setStart(Date start) {
		this.start = start;
	}

	public Date getEnd() {
		return end;
	}

	public void setEnd(Date end) {
		this.end = end;
	}

	public Integer getFrom() {
		return from;
	}

	public void setFrom(Integer from) {
		this.from = from;
	}

	public Integer getTo() {
		return to;
	}

	public void setTo(Integer to) {
		this.to = to;
	}

	public List<RegRoom> getRooms() {
		return rooms;
	}

	public void setRooms(List<RegRoom> rooms) {
		this.rooms = rooms;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Integer getCountOfName() {
		return countOfName;
	}

	public void setCountOfName(Integer countOfName) {
		this.countOfName = countOfName;
	}

	public Integer getSumOfFrom() {
		return sumOfFrom;
	}

	public void setSumOfFrom(Integer sumOfFrom) {
		this.sumOfFrom = sumOfFrom;
	}

	
}
