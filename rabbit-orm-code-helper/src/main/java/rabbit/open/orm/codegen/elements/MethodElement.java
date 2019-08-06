package rabbit.open.orm.codegen.elements;

import rabbit.open.orm.codegen.JavaElement;

/**
 * <b>@description 方法元素 </b>
 */
public class MethodElement {

	// 变量名
	private String varName;

	// 返回值
	private String returnType = "void";

	// 变量类型
	private String varType;

	// get方法
	private boolean get = true;
	
	public MethodElement(String varName, String returnType, String varType,
			boolean get) {
		super();
		this.varName = varName;
		this.returnType = returnType;
		this.varType = varType;
		this.get = get;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		String prefix = "\t";
		if (get) {
			sb.append(prefix + "public " + returnType + " get" + JavaElement.upperFirstLetter(varName) + "() {\n");
			sb.append(prefix + prefix + "return this." + varName + ";\n");
		} else {
			sb.append(prefix + "public void set" + JavaElement.upperFirstLetter(varName)  + "("
					+ varType + " " + varName + ") {\n");
			sb.append(prefix + prefix + "this." + varName + " = " + varName + ";\n");
		}
		sb.append(prefix + "}\n\n");
		return sb.toString();
	}

}
