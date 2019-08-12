package test.mapper;

import rabbit.open.orm.common.annotation.NameSpace;
import rabbit.open.orm.common.annotation.Param;
import rabbit.open.orm.common.annotation.SQLName;
import test.mapper.entity.MappingUser;

import java.util.List;

@NameSpace(MappingUser.class)
public interface UserMapper {

	@SQLName("getUserByNameAndId")
	public MappingUser getUser(@Param("userId") long userId, @Param("username")String username);

	/**
	 * <b>@description 根据id删除 </b>
	 * @param userId
	 * @return
	 */
	public long namedDelete(@Param("userId")long userId);

	/**
	 * <b>@description 根据id更新name </b>
	 * @param userId
	 * @param name
	 * @return
	 */
	public long updateNameById(@Param("userId")long userId, @Param("name")String name);

	public MappingUser getUserByJdbc(@Param("userId")long userId);

	@SQLName("getUserByJdbc")
	public List<MappingUser> getUserByJdbcs(@Param("userId")long userId);

	// update 、add、delete 测试

}
