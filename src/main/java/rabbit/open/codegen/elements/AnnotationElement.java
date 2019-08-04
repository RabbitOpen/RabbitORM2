package rabbit.open.codegen.elements;

import rabbit.open.codegen.JavaElement;

/**
 * <b>@description 注释描述 </b>
 */
public class AnnotationElement extends JavaElement {

	// 注解名字比如Entity
	private String annoName;

	// 注解括号内的内容，比如'(value = "T_USER")'
	private String content;
	
	// 注解包全名
	private String annoFullName;
	
	public AnnotationElement(String annoName, String content,
			String annoFullName) {
		super();
		this.annoName = annoName;
		this.content = content;
		this.annoFullName = annoFullName;
	}

	public void setContent(String content) {
		this.content = content;
	}

	@Override
	public String toString() {
		return annoName + "(" + content + ")\n";
	}

	@Override
	public String getImportPackageString() {
		return annoFullName;
	}
	
}
