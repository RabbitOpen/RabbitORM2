package rabbit.open.orm.core.utils;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.xml.sax.SAXException;
import rabbit.open.orm.common.exception.MappingFileParsingException;
import rabbit.open.orm.common.exception.NamedSQLNotExistedException;
import rabbit.open.orm.common.exception.NoNamedSQLDefinedException;
import rabbit.open.orm.core.dml.SessionFactory;
import rabbit.open.orm.core.dml.name.NamedSQL;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <b>@description xml文件解析器 </b>
 */
public class XmlMapperParser {

	public static final String JDBC = "jdbc";

	public static final String SELECT = "select";

	public static final String UPDATE = "update";

	public static final String DELETE = "delete";

	public static final String INSERT = "insert";

	private Logger logger = LoggerFactory.getLogger(getClass());
	
	private String xmlPath;
	
	//缓存的命名查询对象
	private Map<Class<?>, Map<String, NamedSQL>> nameQueries = new ConcurrentHashMap<>();

	public XmlMapperParser(String path) {
		super();
		this.xmlPath = path;
	}

	/**
	 * 
	 * <b>Description:	根据查询的名字和类信息获取命名查询对象</b><br>
	 * @param name		定义的查询名字	
	 * @param clz		namespace对应的class	
	 * @return	
	 * 
	 */
	public NamedSQL getQueryByNameAndClass(String name, Class<?> clz) {
		Map<String, NamedSQL> map = nameQueries.get(clz);
        if (null == map) {
            throw new NoNamedSQLDefinedException(clz);
        }
        NamedSQL namedSql = map.get(name);
        if (null == namedSql) {
            throw new NamedSQLNotExistedException(name);
        }
		return namedSql;
	}
	
	/**
	 * 
	 * <b>Description:	解析文件</b><br>	
	 * 
	 */
	public void doXmlParsing() {
		List<Resource> resources = readFiles();
		if (resources.isEmpty()) {
			return;
		}
		parseXmls(resources);
	}

	private void parseXmls(List<Resource> resources) {
		for (Resource resource : resources) {
			parseOneByOne(resource);
		}
	}

	private void parseOneByOne(Resource resource) {
		InputStream inputStream = null;
		SAXReader reader = new SAXReader();
		String file = null;
		Document doc;
		try {
			file = resource.getURI().toString();
			logger.info("parsing {}", file);
			inputStream = resource.getInputStream();
            reader.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
			doc = reader.read(inputStream);
			Element root = doc.getRootElement(); 
			String clzName = root.attributeValue("entity");
			Class<?> clz = checkClassName(file, clzName);
			scan(root, clz, SELECT);
			scan(root, clz, UPDATE);
			scan(root, clz, DELETE);
			scan(root, clz, INSERT);
			scan(root, clz, JDBC);
		} catch (DocumentException | SAXException | IOException e) {
			throw new MappingFileParsingException(e.getMessage());
		} finally {
		    closeStream(inputStream);
		}
	}

	protected void closeStream(InputStream inputStream) {
		try {
		    if (null != inputStream) {
		    	inputStream.close();
		    }
		} catch (Exception e) {
		    logger.error(e.getMessage(), e);
		}
	}

	/**
	 * <b>@description 扫描select/update/delete语句 </b>
	 * @param root
	 * @param clz
	 */
	@SuppressWarnings("unchecked")
	private void scan(Element root, Class<?> clz, String type) {
		Iterator<Element> iterator = root.elementIterator(type);
		while (iterator.hasNext()) {
		    Element element = iterator.next();
		    String name = element.attributeValue("name");
		    String targetTableName = element.attributeValue("targetTableName");
		    String sql = element.getText();
		    checkNameQuery(clz, name, sql);
		    nameQueries.get(clz).put(name.trim(), new NamedSQL(sql, name, element, type, targetTableName));
		}
	}

	private void checkNameQuery(Class<?> namespaceClz, String queryName, String sql) {
        if (SessionFactory.isEmpty(queryName)) {
			throw new MappingFileParsingException("empty query name is found for[" + namespaceClz + "]");
		}
        if (nameQueries.get(namespaceClz).containsKey(queryName)) {
			throw new MappingFileParsingException("repeated query name[" + queryName + "] is found!");
		}
        if (SessionFactory.isEmpty(sql)) {
			throw new MappingFileParsingException("empty sql is found in [" + queryName + "]");
		}
	}

	private Class<?> checkClassName(String xml, String className) {
        if (SessionFactory.isEmpty(className)) {
			throw new MappingFileParsingException("invalid class name[" + className + "] for " + xml);
		}
		Class<?> clz = null;
		try {
			clz = Class.forName(className);
		} catch (ClassNotFoundException e) {
			throw new MappingFileParsingException("invalid class name[" + className + "] for " + xml);
		}
        if (!nameQueries.containsKey(clz)) {
			nameQueries.put(clz, new ConcurrentHashMap<>());
		}
		return clz;
	}

	/**
	 * 
	 * <b>Description:	读取sql定义文件</b><br>
	 * @return	
	 * 
	 */
	private List<Resource> readFiles() {
		ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
		List<Resource> streamList = new ArrayList<>();
		try {
			Resource[] resources = resolver.getResources(xmlPath);
			for (Resource resource : resources) {
				streamList.add(resource);
			}
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
		}
		return streamList;
	}

}
