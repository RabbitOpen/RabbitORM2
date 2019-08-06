package mybatis.test.mybatis.dao;

public interface UserDao {

	public void add(String name);
	
	public int count(String name);
	
	public void clear(String name);
}
