package rabbit.open.orm.codegen.elements;

/**
 * @description 字段常量元素
 */
public class ConstantFieldElement {

    // 字段常量的注释
    private DocElement docElement;

    // 常量名
    private String varName;

    // 常量值
    private String value;

    public ConstantFieldElement(DocElement docElement, String varName, String value) {
        this.docElement = docElement;
        this.docElement.setLinePrefix("\t");
        this.varName = varName;
        this.value = value;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(docElement.toString());
        sb.append(docElement.getLinePrefix() + "public static final String " + varName + " = \"" + value + "\";\n\n");
        return sb.toString();
    }
}
