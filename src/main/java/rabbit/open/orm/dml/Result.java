package rabbit.open.orm.dml;

import java.util.ArrayList;
import java.util.List;

/**
 * 查询结果
 * @author	肖乾斌
 * 
 */
public class Result<T>{
	
	private List<T> resultList = new ArrayList<>();
	
	public Result(List<T> resultList) {
		this.resultList = resultList;
	}

	/**
	 * 取回结果集
	 * @return
	 */
	public List<T> list() {
		return resultList;
	}
	
	/**
	 * 取回唯一的那个结果集
	 * @return
	 */
	public T unique(){
		if(resultList.isEmpty()){
			return null;
		}
		return resultList.get(0);
	}

}
