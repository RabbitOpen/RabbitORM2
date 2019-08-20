package rabbit.open.orm.core.utils;

import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.xml.sax.SAXException;
import rabbit.open.orm.common.exception.MappingFileParsingException;
import rabbit.open.orm.common.exception.NamedSQLNotExistedException;
import rabbit.open.orm.common.exception.NoNamedSQLDefinedException;
import rabbit.open.orm.core.dml.SessionFactory;
import rabbit.open.orm.core.dml.name.NamedSQL;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * <b>@description xml文件解析器 </b>
 */
public class XmlMapperParser {

	public static final String JDBC = "jdbc";

	public static final String SELECT = "select";

	public static final String UPDATE = "update";

	public static final String DELETE = "delete";

	public static final String INSERT = "insert";

	private Logger logger = Logger.getLogger(getClass());
	
	private String sqlPath;
	
	//缓存的命名查询对象
	private Map<Class<?>, Map<String, NamedSQL>> nameQueries = new ConcurrentHashMap<>();

	public XmlMapperParser(String path) {
		super();
		this.sqlPath = path;
		if (sqlPath.startsWith("/")) {
		    sqlPath = sqlPath.substring(1);
		}
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

	private void parseOneByOne(String xml) {
		InputStream inputSteam = getClass().getResourceAsStream(xml);
		SAXReader reader = new SAXReader();   
		try {
            reader.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        } catch (SAXException e) {
        }
		Document doc;
		try {
			doc = reader.read(inputSteam);
			Element root = doc.getRootElement(); 
			String clzName = root.attributeValue("entity");
			Class<?> clz = checkClassName(xml, clzName);
			scan(root, clz, SELECT);
			scan(root, clz, UPDATE);
			scan(root, clz, DELETE);
			scan(root, clz, INSERT);
			scan(root, clz, JDBC);
		} catch (DocumentException e) {
			throw new MappingFileParsingException(e.getMessage());
		} finally {
		    try {
                inputSteam.close();
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
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
        if (nameQueries.containsKey(clz)) {
			throw new MappingFileParsingException("repeated class name[" + className + "] is found!");
        } else {
			nameQueries.put(clz, new ConcurrentHashMap<String, NamedSQL>());
		}
		return clz;
	}

	/**
	 * 
	 * <b>Description:	读取sql定义文件</b><br>
	 * @return	
	 * 
	 */
	private List<String> readFiles() {
		List<String> classPathJars = PackageScanner.getClassPathJars();
		List<String> xmls = new ArrayList<>();
		for (String jar : classPathJars) {
			if (!jar.endsWith("jar") && new File(jar).isDirectory()) {
				URL url = null;
				try {
					url = new URL(new File(jar).toURI().toString() + sqlPath);
				} catch (Exception e) {
					logger.error(e.getMessage(), e);
					continue;
				}
				xmls.addAll(scanMappingPath(url.getPath()));
			}
		}
		return xmls;
	}

	private List<String> scanMappingPath(String url) {
	    List<String> xmls = new ArrayList<>();
		File path = new File(url);
		if (!path.exists()) {
			return xmls;
		}
        if (!path.isDirectory()) {
			logger.warn("mapping file path[" + sqlPath + "] is not a directory!");
			if (url.contains(".war!") || url.contains(".jar!")) {
                String jar = getJarFileName(url);
                xmls.addAll(findXmls(jar));
			}
			return xmls; 
		}
        String seperator = "/";
        for (String f : path.list()) {
			String fileName = url + seperator + f;
            if (!isXmlFile(fileName)) {
                continue;
            }
            xmls.add(fileName.substring(fileName.indexOf(seperator + sqlPath + seperator)));
        }
		return xmls;
	}

    private List<String> findXmls(String jar) {
        List<String> xmls = new ArrayList<>();
        JarFile jf = null;
        try {
            jf = new JarFile(jar);
            Enumeration<JarEntry> entries = jf.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (entry.getName().endsWith("xml") && entry.getName().contains("classes/" + sqlPath)){
                    xmls.add(entry.getName().substring(entry.getName().indexOf("/" + sqlPath)));
                }
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        } finally {
            PackageScanner.closeJarFile(jf);
        }
        return xmls;
    }
	
    private String getJarFileName(String url) {
        String feature = ".war";
        if (url.contains(".jar!")) {
            feature = ".jar";
        }
        String fileName = url.substring(0, url.indexOf(feature)) + feature;
        if (fileName.startsWith("file:")) {
            if (System.getProperty("os.name").toLowerCase().startsWith("win")) {
                fileName = fileName.substring(6, fileName.length());
            } else {
                fileName = fileName.substring(5, fileName.length());
            }
        }
        return fileName;
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
