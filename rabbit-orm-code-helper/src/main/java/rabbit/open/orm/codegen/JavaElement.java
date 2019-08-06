package rabbit.open.orm.codegen;

public class JavaElement {

	/**
	 * <b>@description 引入该元素的java文件需要导入的包字符串，比如java.sql.Date </b>
	 * @return
	 */
	protected String getImportPackageString() {
		return null;
	}
	
	/**
	 * <b>@description 判断字符串是否为空 </b>
	 * @param str
	 * @return
	 */
	public static boolean isEmptyStr(String str) {
		return null == str || "".equals(str.trim());
	}
	
	/**
	 * <b>@description 强制大写首字母 </b>
	 * @param str
	 * @return
	 */
	public static String upperFirstLetter(String str) {
		return str.substring(0, 1).toUpperCase()
				+ str.substring(1, str.length());
	}
	
}
