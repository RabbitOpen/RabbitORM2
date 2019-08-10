package test.mapper;

import rabbit.open.orm.common.annotation.FieldMapper;
import rabbit.open.orm.common.annotation.Mapper;
import rabbit.open.orm.common.annotation.SQLMapper;
import test.mapper.entity.MappingUser;

@Mapper(MappingUser.class)
public interface UserMapper {

	@SQLMapper("getUserByNameAndId")
	public MappingUser getUser(@FieldMapper("userId")long userId, @FieldMapper("username")String username);
	
	/**
	 * <b>@description 根据id删除 </b>
	 * @param userId
	 * @return
	 */
	public long namedDelete(@FieldMapper("userId")long userId);
	
	/**
	 * <b>@description 根据id更新name </b>
	 * @param userId
	 * @param name
	 * @return
	 */
	public long updateNameById(@FieldMapper("userId")long userId, @FieldMapper("name")String name);
	
	
}
