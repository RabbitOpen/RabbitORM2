package rabbit.open.orm.core.dml.name;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.xml.sax.SAXException;

import rabbit.open.orm.common.ddl.PackageScanner;
import rabbit.open.orm.common.exception.MappingFileParsingException;
import rabbit.open.orm.common.exception.NoNamedSQLDefinedException;
import rabbit.open.orm.common.exception.UnExistedNamedSQLException;
import rabbit.open.orm.common.exception.WrongMappingFilePathException;
import rabbit.open.orm.core.dml.SessionFactory;

public class SQLParser {

	private static final String JDBC = "jdbc";

	private static final String SELECT = "select";

	private Logger logger = Logger.getLogger(getClass());
	
	private String sqlPath;
	
	//缓存的命名查询对象
	private Map<Class<?>, Map<String, SQLObject>> nameQueries = new ConcurrentHashMap<>();

	public SQLParser(String path) {
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
	public SQLObject getQueryByNameAndClass(String name, Class<?> clz) {
		Map<String, SQLObject> map = nameQueries.get(clz);
        if (null == map) {
            throw new NoNamedSQLDefinedException(clz);
        }
        SQLObject namedSql = map.get(name);
        if (null == namedSql) {
            throw new UnExistedNamedSQLException(name);
        }
		return namedSql;
	}

	/**
	 * 
	 * <b>Description:	根据查询的名字和类信息获取jdbc命名查询对象</b><br>
	 * @param name
	 * @param clz
	 * @return	
	 * 
	 */
	public SQLObject getNamedJdbcQuery(String name, Class<?> clz) {
		Map<String, SQLObject> map = nameQueries.get(clz);
        if (null == map) {
            throw new NoNamedSQLDefinedException(clz);
		}
		SQLObject namedSql = map.get(name);
        if (null == namedSql) {
			throw new UnExistedNamedSQLException(name);
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

	@SuppressWarnings("unchecked")
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
			Iterator<Element> iterator = root.elementIterator(SELECT);
            while (iterator.hasNext()) {
                Element select = iterator.next();
                String name = select.attributeValue("name");
                String sql = select.getText();
                checkNameQuery(clz, name, sql);
		        nameQueries.get(clz).put(name.trim(), new NamedSQL(sql, name, select));
			}
			iterator = root.elementIterator(JDBC);
            while (iterator.hasNext()) {
			    Element select = iterator.next();
			    String name = select.attributeValue("name");
			    String sql = select.getText();
			    checkNameQuery(clz, name, sql);
			    nameQueries.get(clz).put(name.trim(), new SQLObject(sql, name));
			}
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
			nameQueries.put(clz, new ConcurrentHashMap<String, SQLObject>());
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
		URL url = getClass().getClassLoader().getResource(sqlPath);
        if (null == url) {
			throw new WrongMappingFilePathException("mapping file path[" + sqlPath + "] is not found!");
		}
		List<String> xmls = new ArrayList<>();
		xmls.addAll(scanMappingPath(url.getPath()));
		if (url.getPath().contains("/test-classes")) {
		    xmls.addAll(scanMappingPath(url.getPath().replace("test-classes", "classes")));
		}
		return xmls;
	}

	private List<String> scanMappingPath(String url) {
	    List<String> xmls = new ArrayList<>();
		File path = new File(url);
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
