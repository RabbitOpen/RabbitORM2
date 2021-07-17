package rabbit.open.orm.codegen.elements;

import rabbit.open.orm.codegen.JavaElement;

import java.util.ArrayList;
import java.util.List;

/**
 * <b>@description 字段元素  </b>
 */
public class FieldElement extends JavaElement {

	private List<AnnotationElement> annos = new ArrayList<>();
	
	private DocElement doc;

	// 常量字段
	private ConstantFieldElement constantFieldElement;
	
	// 需要导入的包信息
	private String packageString;
	
	// 字段类型 比如 String
	private String fieldType;
	
	// getter方法
	private MethodElement getter;
	
	// setter方法
	private MethodElement setter;
	
	// 变量名
	private String varName;
	
	public FieldElement(AnnotationElement anno, DocElement doc,
			String packageString, String fieldType, String varName, ConstantFieldElement constantFieldElement) {
		super();
		this.annos.add(anno);
		this.doc = doc;
		this.packageString = packageString;
		this.fieldType = fieldType;
		this.varName = varName;
		this.constantFieldElement = constantFieldElement;
		this.getter = new MethodElement(varName, fieldType, fieldType, true);
		this.setter = new MethodElement(varName, "void", fieldType, false);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("\n");
		String linePrefix = "\t";
		if (null != doc) {
			doc.setLinePrefix(linePrefix);
			sb.append(doc.toString());
		}
		for (AnnotationElement anno : this.annos) {
			sb.append(linePrefix + anno.toString());
		}
		sb.append(linePrefix + "private " + fieldType + " " + varName + ";\n\n");
		return sb.toString();
	}
	
	@Override
	public String getImportPackageString() {
		return getPackageString();
	}

	public String getPackageString() {
		return packageString;
	}

	public MethodElement getGetter() {
		return getter;
	}

	public MethodElement getSetter() {
		return setter;
	}
	
	public List<AnnotationElement> getAnnotationElements() {
		return annos;
	}
	
	public void addAnnotationElement(AnnotationElement anno) {
		this.annos.add(0, anno);
	}

	public ConstantFieldElement getConstantFieldElement() {
		return constantFieldElement;
	}
}
