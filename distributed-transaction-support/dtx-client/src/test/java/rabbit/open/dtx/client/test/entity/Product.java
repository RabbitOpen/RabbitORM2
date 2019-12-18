package rabbit.open.dtx.client.test.entity;

import rabbit.open.orm.common.dml.Policy;
import rabbit.open.orm.core.annotation.Column;
import rabbit.open.orm.core.annotation.Entity;
import rabbit.open.orm.core.annotation.PrimaryKey;

import java.util.Date;

/**
 * @author xiaoqianbin
 * @date 2019/12/3
 **/
@Entity("T_PRODUCT_INFO")
public class Product {

    @PrimaryKey(policy= Policy.AUTOINCREMENT)
    @Column("ID")
    private Long id;

    @Column(value = "ENTERPRISE_NAME", comment = "名字")
    private String name;

    @Column(value = "ADDR", comment = "地址")
    private String addr;

    @Column(value = "OWNER", comment = "拥有者")
    private String owner;

    @Column(value = "X_DATE")
    private Date date;

    @Column(value = "GENDER")
    private Gender gender;

    @Column(value = "floatField")
    private Float floatField;

    @Column(value = "doubleField")
    private Double doubleField;

    @Column("bytes_value")
    private byte[] bytes;

    public enum Gender {
        MALE, FEMALE
    }

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

    public String getAddr() {
        return addr;
    }

    public void setAddr(String addr) {
        this.addr = addr;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public Gender getGender() {
        return gender;
    }

    public void setGender(Gender gender) {
        this.gender = gender;
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

    public byte[] getBytes() {
        return bytes;
    }

    public void setBytes(byte[] bytes) {
        this.bytes = bytes;
    }
}
