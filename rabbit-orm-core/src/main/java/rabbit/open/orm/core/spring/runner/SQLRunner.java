package rabbit.open.orm.core.spring.runner;

import java.util.HashMap;
import java.util.Map;

import rabbit.open.orm.core.dml.SessionFactory;
import rabbit.open.orm.core.spring.runner.impl.NamedDeleteRunner;
import rabbit.open.orm.core.spring.runner.impl.NamedJdbcRunner;
import rabbit.open.orm.core.spring.runner.impl.NamedQueryRunner;
import rabbit.open.orm.core.spring.runner.impl.NamedUpdateRunner;
import rabbit.open.orm.core.utils.XmlMapperParser;

public abstract class SQLRunner {

	private static Map<String, SQLRunner> runnerMapping = new HashMap<>();
	
	/**
	 * <b>@description 运行sql object </b>
	 * @param args			接口方法参数
	 * @param mapping		映射关系
	 * @param namespaceClz	命名空间对应的实体
	 * @param factory		factory
	 * @return
	 */
	public abstract Object run(Object[] args, MethodMapping mapping, 
			Class<?> namespaceClz, SessionFactory factory);
	
	
	static {
		regist();
	}

	private static void regist() {
		runnerMapping.put(XmlMapperParser.SELECT, new NamedQueryRunner());
		runnerMapping.put(XmlMapperParser.UPDATE, new NamedUpdateRunner());
		runnerMapping.put(XmlMapperParser.DELETE, new NamedDeleteRunner());
		runnerMapping.put(XmlMapperParser.JDBC, new NamedJdbcRunner());
	}
	
	public static SQLRunner getRunner(String type) {
		return runnerMapping.get(type);
	}
	
}
