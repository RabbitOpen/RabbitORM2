package rabbit.open.test;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import rabbit.open.orm.codegen.DBFieldDescriptor;
import rabbit.open.orm.codegen.MappingRegistry;
import rabbit.open.orm.codegen.filter.GeneratorFilter;
import rabbit.open.orm.codegen.generator.CodeGenerator;

@RunWith(JUnit4.class)
public class CodeGeneratorTest {

	@Test	
	public void genCodeTest() {
		CodeGenerator cg = new CodeGenerator(
				"jdbc:mysql://localhost:3306/cas?useUnicode=true&characterEncoding=UTF-8&useServerPrepStmts=true",
				"com.mysql.jdbc.Driver", "root", "123456",
				"C:/Users/xiaoqianbin/Desktop/java", "com.org");
		
		MappingRegistry.regist("VARCHAR", new DBFieldDescriptor(String.class, true));
		
		cg.setFilter(new GeneratorFilter(){
			
			@Override
			public boolean filterColumn(String columnName) {
				return columnName.length() >= 1;
			}
			
			@Override
			public boolean filterTable(String tableName) {
				if (tableName.equals("hello")) {
					// 过滤掉hello这张表
					return false;
				}
				System.out.println("generate code for table[" + tableName + "]");
				return true;
			}
		});
		try {
			cg.generateClassFile();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
