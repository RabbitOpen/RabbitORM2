package rabbit.open.codegen.elements;

import rabbit.open.codegen.JavaElement;

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

	public String getVarName() {
		return varName;
	}

	public void setVarName(String varName) {
		this.varName = varName;
	}

	public String getReturnType() {
		return returnType;
	}

	public void setReturnType(String returnType) {
		this.returnType = returnType;
	}

	public String getVarType() {
		return varType;
	}

	public void setVarType(String varType) {
		this.varType = varType;
	}

	public boolean isGet() {
		return get;
	}

	public void setGet(boolean get) {
		this.get = get;
	}
	
}
