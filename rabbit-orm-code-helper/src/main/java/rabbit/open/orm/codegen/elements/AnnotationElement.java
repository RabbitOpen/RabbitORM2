package rabbit.open.orm.codegen.elements;

import java.util.ArrayList;
import java.util.List;

/**
 * <b>@description 注释描述 </b>
 */
public class AnnotationElement {

	// 注解名字比如Entity
	private String annoName;

	// 注解括号内的内容，比如'(value = "T_USER")'
	private String content;
	
	// 需要导入的包
	private List<String> imports = new ArrayList<>();
	
	public AnnotationElement(String annoName, String content,
			String annoFullName) {
		super();
		this.annoName = annoName;
		this.content = content;
		this.imports.add(annoFullName);
	}

	public void setContent(String content) {
		this.content = content;
	}

	public String getContent() {
		return content;
	}

	@Override
	public String toString() {
		return annoName + "(" + content + ")\n";
	}

	public List<String> getImports() {
		return imports;
	}

	public void addImport(String imp) {
		this.imports.add(imp);
	}
}
