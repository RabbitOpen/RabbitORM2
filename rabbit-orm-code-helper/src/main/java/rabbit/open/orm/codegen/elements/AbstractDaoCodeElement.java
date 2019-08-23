package rabbit.open.orm.codegen.elements;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import rabbit.open.orm.codegen.generator.CodeGenerator;
import rabbit.open.orm.core.dml.SessionFactory;
import rabbit.open.orm.core.spring.SpringDaoAdapter;

/**
 * <b>@description 抽象dao代码 </b>
 */
public class AbstractDaoCodeElement {

	public static final String IMPORT = "import ";

	protected String packageName;
	
	// 通用dao基类名字
	public static final String PRIMARY_DAO_CLASS_NAME = "PrimaryDao";
	
	public AbstractDaoCodeElement(String packageName) {
		super();
		this.packageName = packageName;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("package " + packageName + ";\n\n");
		sb.append(IMPORT + PostConstruct.class.getName() + ";\n");
		sb.append(IMPORT + Resource.class.getName() + ";\n");
		sb.append(IMPORT + SessionFactory.class.getName() + ";\n");
		sb.append(IMPORT + SpringDaoAdapter.class.getName() + ";\n\n");
		sb.append(new DocElement(CodeGenerator.COMMON_MSG, "@desc:  通用Dao泛型基类"));
		sb.append("public abstract class " + PRIMARY_DAO_CLASS_NAME + "<T> extends SpringDaoAdapter<T> {\n\n");
		DocElement sfDoc = new DocElement(CodeGenerator.COMMON_MSG, 
				"@desc:  sessionFactory对象，请确保注入的名字和xml配置中一致");
		sfDoc.setLinePrefix("\t");
		sb.append(sfDoc.toString());
		sb.append("\t@Resource(name = \"sessionFactory\")\n");
		sb.append("\tprotected SessionFactory factory;\n\n");
		sb.append("\t@PostConstruct\n");
		sb.append("\tpublic void setUp() {\n");
		sb.append("\t\tsetSessionFactory(factory);\n");
		sb.append("\t}\n\n");
		sb.append("}\n");
		return sb.toString();
	}
	
}
