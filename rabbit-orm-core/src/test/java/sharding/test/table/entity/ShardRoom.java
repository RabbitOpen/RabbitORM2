package sharding.test.table.entity;

import rabbit.open.orm.common.annotation.Column;
import rabbit.open.orm.common.annotation.Entity;
import rabbit.open.orm.common.annotation.PrimaryKey;

/**
 * <b>Description  房屋表</b>
 */
@Entity("T_SHARD_ROOM")
public class ShardRoom {

    @PrimaryKey
    @Column("ROOM_ID")
    private String id;
    
    @Column("ROOM_NO")
    private String roomNo;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getRoomNo() {
        return roomNo;
    }

    public void setRoomNo(String roomNo) {
        this.roomNo = roomNo;
    }

    public ShardRoom(String id, String roomNo) {
        super();
        this.id = id;
        this.roomNo = roomNo;
    }

    public ShardRoom() {
        super();
    }
    
}
