package rabbit.open.codegen.generator;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;

import rabbit.open.codegen.DBFieldDescriptor;
import rabbit.open.codegen.JavaElement;
import rabbit.open.codegen.MappingRegistry;
import rabbit.open.codegen.elements.AbstractDaoCodeElement;
import rabbit.open.codegen.elements.AnnotationElement;
import rabbit.open.codegen.elements.CommonDaoCodeElement;
import rabbit.open.codegen.elements.DocElement;
import rabbit.open.codegen.elements.DomainClassElement;
import rabbit.open.codegen.elements.FieldElement;
import rabbit.open.codegen.elements.ServiceCodeElement;
import rabbit.open.codegen.filter.GeneratorFilter;
import rabbit.open.orm.annotation.Column;
import rabbit.open.orm.annotation.Entity;
import rabbit.open.orm.annotation.PrimaryKey;
import rabbit.open.orm.dml.policy.Policy;
import rabbit.open.orm.exception.RabbitDMLException;

public class CodeGenerator {

	// 数据库连接
	private String url;

	// 驱动
	private String driverName;

	// DB 用户名
	private String username;

	// db 密码
	private String password;

	// 生产文件的根路径
	private String fileRootPath;
	
	// 文件的包名
	private String basePackageName;
	
	public static final String COMMON_MSG = "this is created by rabbit orm code generator";

	// dao文件路径
	private File daoFile;

	// 通用dao文件路径
	private File genericalDaoFile;

	// 服务文件路径
	private File serviceFile;
	
	// 过滤器
	private GeneratorFilter filter = new GeneratorFilter() {
		
		@Override
		public boolean filterColumn(String columnName) {
			return true;
		}
		
		@Override
		public boolean filterTable(String tableName) {
			return true;
		}
	};
	
	/**
	 * @param url
	 * @param driverName
	 * @param username
	 * @param password
	 * @param fileRootPath
	 * @param basePackageName	
	 */
	public CodeGenerator(String url, String driverName, String username,
			String password, String fileRootPath, String basePackageName) {
		super();
		this.url = url;
		this.driverName = driverName;
		this.username = username;
		this.password = password;
		this.fileRootPath = fileRootPath;
		this.basePackageName = basePackageName;
		if (JavaElement.isEmptyStr(basePackageName)) {
			throw new RabbitDMLException("基础包[basePackageName]名不能为空");
		} 
		createDirs();
	}

	/**
	 * <b>@description 根据驼峰命令规则将字段名转成java类名 </b>
	 * 
	 * @param dbName
	 * @return
	 */
	private String convertDbName2Java(String dbName) {
		StringBuilder name = new StringBuilder(dbName.toLowerCase());
		int index = name.indexOf("_");
		while (-1 != index) {
			try {
				String k = name.substring(index + 1, index + 2);
				name.replace(index + 1, index + 2, k.toUpperCase());
				index = name.indexOf("_", index + 1);
			} catch (Exception e) {
				break;
			}
		}
		return name.toString().replaceAll("_", "");
	}
	
	/**
	 * <b>@description 生成类代码 </b>
	 * @throws IOException 
	 * @throws SQLException 
	 * @throws ClassNotFoundException 
	 */
	public void generateClassFile() throws ClassNotFoundException, SQLException, IOException {
		Class.forName(driverName);
		Connection conn = DriverManager.getConnection(this.url, this.username,
				this.password);
		try {
			generateGenericalDaoClassFile();
			ResultSet tables = conn.getMetaData().getTables(null, null, null, null);
			while (tables.next()) {
				String tableName = "TABLE_NAME";
				if (!"TABLE".equalsIgnoreCase(tables.getString("TABLE_TYPE"))
						|| !filter.filterTable(tables.getString(tableName))) {
					continue;
				}
				generateEntityClassFile(tables.getString(tableName), conn);
				generateDaoClassFile(tables.getString(tableName));
				generateServiceClassFile(tables.getString(tableName));
			}
			tables.close();
		} finally {
			conn.close();
		}
	}

	/**
	 * <b>@description 根据表名生成类文件 </b>
	 * 
	 * @param tableName
	 * @param conn
	 * @throws SQLException 
	 * @throws IOException 
	 */
	private void generateEntityClassFile(String tableName, Connection conn)
			throws SQLException, IOException {
		ResultSet columns = conn.getMetaData().getColumns(null, null,
				tableName, null);
		String className = JavaElement.upperFirstLetter(convertDbName2Java(tableName));
		String filePath = fileRootPath;
		if (!JavaElement.isEmptyStr(basePackageName)) {
			filePath += "/" + basePackageName.replaceAll("\\.", "/");
		}
		String annoValueStartStr = "value = \"";
		DomainClassElement ce = new DomainClassElement(className, Arrays.asList(
				new AnnotationElement("@Entity", annoValueStartStr + tableName + "\"", Entity.class.getName())));
		
		// 添加类注释
		ce.setDoc(new DocElement(COMMON_MSG));
		
		// 添加包名
		if (!JavaElement.isEmptyStr(basePackageName)) {
			ce.setPackageName(basePackageName);
		}
		String pkName = getPrimaryKeyName(tableName, conn);
		while (columns.next()) {
			String columnName = columns.getString("COLUMN_NAME");
			if (!filter.filterColumn(columnName)) {
				continue;
			}
			String type = columns.getString("TYPE_NAME").toUpperCase();
			String size = columns.getString("COLUMN_SIZE");
			boolean isIncrement = "YES".equalsIgnoreCase(columns.getString("IS_AUTOINCREMENT"));
			String remark = columns.getString("REMARKS");
			String name = convertDbName2Java(columnName);
			
			DBFieldDescriptor fieldDescriptor = MappingRegistry.getFieldDescriptor(type);
			if (null == fieldDescriptor) {
				throw new RabbitDMLException("没有找到字段类型[" + type + "]的注册信息");
			}
			
			// 字段注解
			AnnotationElement an = new AnnotationElement("@Column", "", Column.class.getName());
			if (fieldDescriptor.isLengthSensitive()) {
				an.setContent(annoValueStartStr + columnName + "\", length = " + size);
			} else {
				an.setContent(annoValueStartStr + columnName + "\"");
			}
			
			DocElement doc = new DocElement(COMMON_MSG, "@desc:  " + remark);
			
			FieldElement fe = new FieldElement(an, doc, 
					// java.lang的就不用导入了
					fieldDescriptor.getJavaType().getName().startsWith("java.lang") ? "" : fieldDescriptor.getJavaType().getName(), 
					fieldDescriptor.getJavaType().getSimpleName(), name);
			
			if (columnName.equals(pkName)) {
				String content = isIncrement ? "policy = Policy.AUTOINCREMENT" : "";
				AnnotationElement pkAnno = new AnnotationElement("@PrimaryKey", content, PrimaryKey.class.getName());
				pkAnno.addImport(Policy.class.getName());
				fe.addAnnotationElement(pkAnno);
			}
			
			ce.addFieldElement(fe);
		}
		FileWriter fw = new FileWriter(filePath + "\\" + className + ".java");
		try {
			fw.write(ce.toString());
		} finally {
			fw.close();
		}
		columns.close();
	}

	/**
	 * <b>@description 获取主键字段 </b>
	 * @param tableName
	 * @param conn
	 * @return
	 * @throws SQLException
	 */
	private String getPrimaryKeyName(String tableName, Connection conn)
			throws SQLException {
		ResultSet primaryKeys = conn.getMetaData().getPrimaryKeys(null, null, tableName);
		String pkName = null;
		if (primaryKeys.next()) {
			pkName = primaryKeys.getString("COLUMN_NAME");
			primaryKeys.close();
		}
		return pkName;
	}

	public void setFilter(GeneratorFilter filter) {
		this.filter = filter;
	}
	
	private void createDirs() {
		String pathSeperator = "/";
		File entityFile = new File(fileRootPath + pathSeperator + basePackageName.replaceAll("\\.", pathSeperator));
		entityFile.mkdirs();
		daoFile = new File(fileRootPath + pathSeperator + (basePackageName + ".dao").replaceAll("\\.", pathSeperator));
		daoFile.mkdirs();
		genericalDaoFile = new File(fileRootPath + pathSeperator + (basePackageName + ".dao.base").replaceAll("\\.", pathSeperator));
		genericalDaoFile.mkdirs();
		serviceFile = new File(fileRootPath + pathSeperator + (basePackageName + ".service").replaceAll("\\.", pathSeperator));
		serviceFile.mkdirs();
	}
	
	/**
	 * <b>@description 创建dao文件 </b>
	 * @param tableName
	 * @throws IOException 
	 */
	private void generateDaoClassFile(String tableName) throws IOException { 
		String javaEntityName = JavaElement.upperFirstLetter(convertDbName2Java(tableName));
		FileWriter fw = new FileWriter(daoFile.getAbsoluteFile() + "\\"
				+ javaEntityName + "Dao.java");
		try {
			fw.write(new CommonDaoCodeElement(basePackageName + ".dao",
				basePackageName + "." + javaEntityName, javaEntityName).toString());
		} finally {
			fw.close();
		}
	}
	
	/**
	 * <b>@description 创建Service </b>
	 * 
	 * @param tableName
	 * @throws IOException
	 */
	private void generateServiceClassFile(String tableName) throws IOException { 
		String javaEntityName = JavaElement.upperFirstLetter(convertDbName2Java(tableName));
		FileWriter fw = new FileWriter(serviceFile.getAbsoluteFile() + "\\" + javaEntityName + "Service.java");
		try {
			fw.write(new ServiceCodeElement(basePackageName + ".service",
					basePackageName + "." + javaEntityName, javaEntityName).toString());
		} finally {
			fw.close();
		}
	}

	/**
	 * <b>@description 创建通用dao类 </b>
	 * @throws IOException
	 */
	private void generateGenericalDaoClassFile() throws IOException {
		FileWriter fw = new FileWriter(genericalDaoFile.getAbsoluteFile() + "\\"
				+ AbstractDaoCodeElement.GENERICAL_DAO_CLASS_NAME + ".java");
		try {
			fw.write(new AbstractDaoCodeElement(basePackageName + ".dao.base").toString());
		} finally {
			fw.close();
		}
	}

}
