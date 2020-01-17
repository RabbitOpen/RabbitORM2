package rabbit.open.dtx.rpc.test.server;

/**
 * @author xiaoqianbin
 * @date 2020/1/16
 **/
public interface UserService {

    /**
     * 新增用户返回id
     * @param	name
	 * @param	age
     * @author  xiaoqianbin
     * @date    2020/1/16
     **/
    Long addUser(String name, Integer age);

    User getUserById(Long id);
}
