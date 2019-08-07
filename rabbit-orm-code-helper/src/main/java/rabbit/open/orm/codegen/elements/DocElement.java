package rabbit.open.orm.codegen.elements;

import java.util.ArrayList;
import java.util.List;

/**
 * <b>@description 注释元素 </b>
 */
public class DocElement {

	private StringBuilder text = new StringBuilder();

	private List<String> lines = new ArrayList<>();
	
	// 每行的前缀
	private String linePrefix = "";

	public DocElement(String... contents) {
		for (String c : contents) {
			addDocLine(c);
		}
	}

	/**
	 * <b>@description 添加注释 </b>
	 * @param line
	 */
	public void addDocLine(String line) {
		lines.add(" * " + line + "\n");
	}

	@Override
	public String toString() {
		text.append(linePrefix + "/**\n");
		text.append(linePrefix + " *\n");
		for (String str : lines) {
			text.append(linePrefix + str);
		}
		text.append(linePrefix + " *\n");
		text.append(linePrefix + " */\n");
		return text.toString();
	}

	public void setLinePrefix(String linePrefix) {
		this.linePrefix = linePrefix;
	}

	public String getLinePrefix() {
		return linePrefix;
	}
}
