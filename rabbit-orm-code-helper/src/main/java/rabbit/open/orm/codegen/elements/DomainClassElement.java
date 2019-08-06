package rabbit.open.orm.codegen.elements;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import rabbit.open.orm.codegen.JavaElement;

/**
 * <b>@description 实体class 元素 </b>
 */
public class DomainClassElement {

	// 类名, 比如User
	private String className;

	// 包名, 比如 com.test
	private String packageName;

	private DocElement doc;
	
	// 生成get/set方法
	private boolean generateGetSet = true;

	// class元素的注解信息
	private List<AnnotationElement> annos = new ArrayList<>();

	// 字段信息
	private List<FieldElement> fieldElements = new ArrayList<>();

	public DomainClassElement(String className, String packageName,
			List<AnnotationElement> annos) {
		super();
		this.className = className;
		this.packageName = packageName;
		if (null != annos) {
			this.annos = annos;
		}
	}

	public DomainClassElement(String className, List<AnnotationElement> annos) {
		this(className, null, annos);
	}

	public void setPackageName(String packageName) {
		this.packageName = packageName;
	}

	public void setDoc(DocElement doc) {
		this.doc = doc;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(createPackageInfo());
		sb.append(createImportInfos());
		sb.append(createJavaDocOfClass());
		sb.append(createClassAnnotations());
		sb.append(createClassBody());
		return sb.toString();
	}

	/**
	 * <b>@description 创建类实体 </b>
	 * 
	 * @return
	 */
	private StringBuilder createClassBody() {
		StringBuilder sb = new StringBuilder();
		sb.append("public class " + className + " {\n");
		
		for (FieldElement fe : this.fieldElements) {
			sb.append(fe.toString());
		}
		
		if (isGenerateGetSet()) {
			// 生成get/set方法
			for (FieldElement fe : this.fieldElements) {
				sb.append(fe.getGetter().toString());
				sb.append(fe.getSetter().toString());
			}
		}
		
		sb.append("}\n");
		return sb;
	}

	/**
	 * <b>@description 创建类注解 </b>
	 * 
	 * @return
	 */
	private StringBuilder createClassAnnotations() {
		StringBuilder sb = new StringBuilder();
		for (AnnotationElement an : annos) {
			sb.append(an.toString());
		}
		return sb;
	}

	/**
	 * <b>@description 创建导入包信息 </b>
	 * 
	 * @return
	 */
	private StringBuilder createImportInfos() {
		StringBuilder sb = new StringBuilder();
		Set<String> imports = new HashSet<>();
		// 类注解需要导入的包
		for (AnnotationElement an : annos) {
			imports.addAll(an.getImports());
		}
		// 字段对应的包
		for (FieldElement fe : this.fieldElements) {
			if (!JavaElement.isEmptyStr(fe.getImportPackageString())) {
				imports.add(fe.getImportPackageString());
			}
			for (AnnotationElement anno : fe.getAnnotationElements()) {
				imports.addAll(anno.getImports());
			}
		}
		for (String im : imports) {
			sb.append("import " + im + ";\n");
		}
		sb.append("\n");
		return sb;
	}
	
	

	/**
	 * <b>@description 创建文件包信息 </b>
	 * 
	 * @return
	 */
	private StringBuilder createPackageInfo() {
		StringBuilder sb = new StringBuilder();
		if (!JavaElement.isEmptyStr(packageName)) {
			sb.append("package " + packageName + ";\n\n");
		}
		return sb;
	}

	/**
	 * <b>@description 创建类的javadoc信息 </b>
	 * 
	 * @return
	 */
	private StringBuilder createJavaDocOfClass() {
		StringBuilder sb = new StringBuilder();
		if (null != this.doc) {
			sb.append(doc.toString());
		}
		return sb;
	}
	
	/**
	 * <b>@description 添加字段信息 </b>
	 * @param fieldElement
	 */
	public void addFieldElement(FieldElement fieldElement) {
		this.fieldElements.add(fieldElement);
	}
	
	public void setGenerateGetSet(boolean generateGetSet) {
		this.generateGetSet = generateGetSet;
	}
	
	public boolean isGenerateGetSet() {
		return generateGetSet;
	}
}
