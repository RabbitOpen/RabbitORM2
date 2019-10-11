package rabbit.open.orm.codegen.elements;

import org.springframework.stereotype.Repository;

import rabbit.open.orm.codegen.generator.CodeGenerator;

/**
 * <b>@description 普通dao的代码元素</b>
 */
public class CommonDaoCodeElement extends AbstractDaoCodeElement {
	
	// 对应的实体类的全路径名
	private String entityName;

	// 对应的实体类名
	private String entitySimpleName;

	public CommonDaoCodeElement(String packageName, String entityName, String entitySimpleName) {
		super(packageName);
		this.entityName = entityName;
		this.entitySimpleName = entitySimpleName;
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("package " + packageName + ";\n\n");
		sb.append(AbstractDaoCodeElement.IMPORT + entityName + ";\n");
		sb.append(AbstractDaoCodeElement.IMPORT + Repository.class.getName() + ";\n");
		sb.append(AbstractDaoCodeElement.IMPORT + entityName.substring(0, entityName.lastIndexOf('.') + 1) + "dao.base."
				+ AbstractDaoCodeElement.GENERIC_DAO_CLASS_NAME + ";\n");
		sb.append(new DocElement(CodeGenerator.COMMON_MSG, "@desc:  " + entitySimpleName + " dao 实现类"));
		sb.append(new AnnotationElement("@Repository", "", Repository.class.getName()));
		sb.append("public class " + entitySimpleName + "Dao" + " extends " + 
				AbstractDaoCodeElement.GENERIC_DAO_CLASS_NAME + "<" + entitySimpleName + "> {\n\n}\n");
		return sb.toString();
	}

}
