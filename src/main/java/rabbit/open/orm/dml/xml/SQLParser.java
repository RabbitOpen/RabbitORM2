package rabbit.open.orm.dml.xml;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import rabbit.open.orm.exception.MappingFileParsingException;
import rabbit.open.orm.exception.RabbitDMLException;
import rabbit.open.orm.exception.WrongMappingFilePathException;

public class SQLParser {

	private static final String JDBC = "jdbc";

	private static final String SELECT = "select";

	private Logger logger = Logger.getLogger(getClass());
	
	private String sqlPath;
	
	//缓存的命名查询对象
	static Map<Class<?>, Map<String, NameQuery>> nameQueries = new ConcurrentHashMap<>();

	public SQLParser(String path) {
		super();
		this.sqlPath = path;
	}
	
	/**
	 * 
	 * <b>Description:	根据查询的名字和类信息获取命名查询对象</b><br>
	 * @param name		定义的查询名字	
	 * @param clz		namespace对应的class	
	 * @return	
	 * 
	 */
	public static NameQuery getNamedQuery(String name, Class<?> clz){
		Map<String, NameQuery> map = nameQueries.get(clz);
		if(null == map){
			throw new RabbitDMLException("no namequery is declared for class[" + clz.getName() + "]");
		}
		NameQuery nameQuery = map.get(name);
		if(null == nameQuery){
			throw new RabbitDMLException("no NameQuery[" + name + "] is declared!");
		}
		if(!nameQuery.isNameQuery()){
			throw new RabbitDMLException("Query[" + name + "] is declared as a jdbc query!");
		}
		return nameQuery;
	}

	/**
	 * 
	 * <b>Description:	根据查询的名字和类信息获取jdbc命名查询对象</b><br>
	 * @param name
	 * @param clz
	 * @return	
	 * 
	 */
	public static NameQuery getNamedJdbcQuery(String name, Class<?> clz){
		Map<String, NameQuery> map = nameQueries.get(clz);
		if(null == map){
			throw new RabbitDMLException("no namequery is declared for class[" + clz.getName() + "]");
		}
		NameQuery nameQuery = map.get(name);
		if(null == nameQuery){
			throw new RabbitDMLException("no NameQuery[" + name + "] is declared!");
		}
		if(nameQuery.isNameQuery()){
			throw new RabbitDMLException("Query[" + name + "] is declared as a named query!");
		}
		return nameQuery;
	}
	
	/**
	 * 
	 * <b>Description:	解析文件</b><br>	
	 * 
	 */
	public void doXmlParsing(){
		List<String> xmls = readFiles();
		if(xmls.isEmpty()){
			return;
		}
		readXmls(xmls);
	}

	private void readXmls(List<String> xmls) {
		for(String xml : xmls){
			logger.info("parsing " + xml);
			parseOneByOne(xml);   
		}
	}

	@SuppressWarnings("unchecked")
	private void parseOneByOne(String xml) {
		File file = new File(xml);
		SAXReader reader = new SAXReader();   
		Document doc;
		try {
			doc = reader.read(file);
			Element root = doc.getRootElement(); 
			String clzName = root.attributeValue("class");
			Class<?> clz = checkClassName(xml, clzName);
			Iterator<Element> iterator = root.elementIterator(SELECT);
			while(iterator.hasNext()) {
				addNameQuery(clz, iterator.next(), SELECT);
			}
			iterator = root.elementIterator(JDBC);
			while(iterator.hasNext()) {
				addNameQuery(clz, iterator.next(), JDBC);
			}
		} catch (DocumentException e) {
			throw new MappingFileParsingException(e.getMessage());
		}
	}

	private void addNameQuery(Class<?> namespaceClz, Element select, String type){
		String queryName = select.attributeValue("name");
		String sql = select.getText();
		checkNameQuery(namespaceClz, queryName, sql, type);
		nameQueries.get(namespaceClz).put(queryName.trim(), new NameQuery(sql, type, queryName));
	}

	private void checkNameQuery(Class<?> namespaceClz, String queryName, String sql, String type) {
		if(isEmpty(queryName)){
			throw new MappingFileParsingException("empty query name[" + queryName + "] is found!");
		}
		if(nameQueries.get(namespaceClz).containsKey(queryName)){
			throw new MappingFileParsingException("repeated query name[" + queryName + "] is found!");
		}
		if(isEmpty(sql)){
			throw new MappingFileParsingException("empty sql is found in [" + queryName + "]");
		}
		if(!JDBC.equalsIgnoreCase(type) && !sql.trim().toLowerCase().startsWith("from")){
			throw new MappingFileParsingException("name query[" + queryName + "] must be started with keyword[FROM]");
		}
	}

	private Class<?> checkClassName(String xml, String className) {
		if(isEmpty(className)){
			throw new MappingFileParsingException("invalid class name[" + className + "] for " + xml);
		}
		Class<?> clz = null;
		try {
			clz = Class.forName(className);
		} catch (ClassNotFoundException e) {
			throw new MappingFileParsingException("invalid class name[" + className + "] for " + xml);
		}
		if(nameQueries.containsKey(clz)){
			throw new MappingFileParsingException("repeated class name[" + className + "] is found!");
		}else{
			nameQueries.put(clz, new ConcurrentHashMap<String, NameQuery>());
		}
		return clz;
	}

	private boolean isEmpty(String namespace) {
		return null == namespace || "".equals(namespace.trim());
	}
	
	/**
	 * 
	 * <b>Description:	读取sql定义文件</b><br>
	 * @return	
	 * 
	 */
	private List<String> readFiles(){
		URL url = getClass().getClassLoader().getResource(sqlPath);
		if(null == url){
			throw new WrongMappingFilePathException("mapping file path[" + sqlPath + "] is not found!");
		}
		List<String> xmls = new ArrayList<>();
		scanMappingPath(url.getPath(), xmls);
		scanMappingPath(url.getPath().replace("test-classes", "classes"), xmls);
		return xmls;
	}

	private void scanMappingPath(String url, List<String> xmls) {
		File path = new File(url);
		if(!path.isDirectory()){
			logger.warn("mapping file path[" + sqlPath + "] is not a directory!");
			return; 
		}
		for(String f : path.list()){
			String fileName = url + "/" + f;
			if(!isXmlFile(fileName)){
				continue;
			}
			xmls.add(fileName);
		}
	}

	/**
	 * 
	 * <b>Description:	检查是否是xml文件</b><br>
	 * @param fileName
	 * @return	
	 * 
	 */
	private boolean isXmlFile(String fileName) {
		File file = new File(fileName);
		return file.exists() && file.isFile() && fileName.endsWith(".xml");
	}
	
}
