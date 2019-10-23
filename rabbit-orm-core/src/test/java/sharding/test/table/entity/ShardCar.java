package sharding.test.table.entity;

import rabbit.open.orm.core.annotation.Column;
import rabbit.open.orm.core.annotation.Entity;
import rabbit.open.orm.core.annotation.PrimaryKey;

/**
 * <b>Description  车辆表</b>
 */
@Entity("T_SHARD_CAR")
public class ShardCar {

    @PrimaryKey
    @Column("CAR_ID")
    private String id;
    
    @Column("CAR_NO")
    private String carNo;
    
    @Column("USER_ID")
    private Long userId;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCarNo() {
        return carNo;
    }

    public void setCarNo(String carNo) {
        this.carNo = carNo;
    }

    public ShardCar(String id, String carNo, Long userId) {
        super();
        this.id = id;
        this.carNo = carNo;
        this.userId = userId;
    }

    public ShardCar() {
        super();
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }
    
    

    
}
