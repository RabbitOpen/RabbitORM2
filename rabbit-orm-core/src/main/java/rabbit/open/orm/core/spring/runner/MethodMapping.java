package rabbit.open.orm.core.spring.runner;

import java.util.ArrayList;
import java.util.List;

/**
 * <b>@description 函数与sql对象之间的映射关系 </b>
 */
public class MethodMapping {
	// 函数对应的sql名
	private String sqlName;

	private List<String> paraNames = new ArrayList<>();

	public MethodMapping(String sqlName) {
		super();
		this.sqlName = sqlName;
	}

	public String getSqlName() {
		return sqlName;
	}

	public List<String> getParaNames() {
		return paraNames;
	}

	public void addParaName(String name) {
		paraNames.add(name);
	}

}
