package rabbit.open.orm.ddl;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.apache.log4j.Logger;

import rabbit.open.orm.exception.RabbitDDLException;

@SuppressWarnings("serial")
public class PackageScanner implements Serializable{
	static Logger logger = Logger.getLogger(PackageScanner.class);
	/**
	 * 
	 * 过滤包含特定注解的类
	 * @param annotation
	 * @return
	 * 
	 */
	public static Set<String> filterByAnnotation(String[] roots, 
			Class<? extends Annotation> annotation){
		HashSet<String> targets = new HashSet<>();
		for(String root : roots){
			List<String> list = scanPackage(root);
			for(String name : list){
				try{
					Class<?> clz = Class.forName(name);
					if(null != clz.getAnnotation(annotation)){
						targets.add(name);
					}
				}catch(Exception e){
				    logger.error(e.getMessage());
				}
			}
		}
		targets.addAll(scanJarFileByAnnotation(roots, annotation));
		return targets;
	}

	/**
	 * 
	 * 过滤实现了特定接口的类
	 * @param interfaceClz
	 * @return
	 * 
	 */
	public static Set<String> filterByInterface(String[] roots, Class<?> interfaceClz){
		HashSet<String> targets = new HashSet<>();
		for(String root : roots){
			List<String> list = scanPackage(root);
			for(String name : list){
				try{
					Class<?> clz = Class.forName(name);
					if(interfaceClz.isAssignableFrom(clz)){
						targets.add(name);
					}
				}catch(Exception e){
					logger.error(e.getMessage(), e);
				}
			}
		}
		targets.addAll(scanJarFileByInterface(roots, interfaceClz));
		return targets;
	}

	/**
	 * 
	 * 扫描包下所有java文件
	 * @return
	 * 
	 */
	private static List<String> scanPackage(String rootPath){
		List<String> files = new ArrayList<>();
		URL base = PackageScanner.class.getClassLoader().getResource("");
		files.addAll(scanURI(rootPath.trim(), base));
		if(base.getPath().contains("test-classes")){
		    try {
                files.addAll(scanURI(rootPath.trim(), 
                        new URL("file:" + base.getPath().replace("test-classes", "classes"))));
            } catch (MalformedURLException e) {
                logger.error(e.getMessage(), e);
            }
		}
		return files;
	}

    private static List<String> scanURI(String rootPath, URL base) {
        List<String> files = new ArrayList<>();
        try {
            URL url = new URL(base, rootPath.replaceAll("\\.", "/"));
			File root = new File(url.toURI());
			if(root.exists() && root.isDirectory()){
				logger.info("scan path: " + root.getPath().replaceAll("\\\\", "/"));
				for(File f : root.listFiles()){
					files.addAll(scanFile(rootPath + "." + f.getName(), f));
				}
			}
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
        return files;
    }
	
	private static List<String> scanFile(String parent, File file) throws Exception {
		while(parent.startsWith(".")){
			parent = parent.substring(1, parent.length());
		}
		List<String> files = new ArrayList<>();
		if(file.isFile()){
			if(parent.endsWith(".class")){
				files.add(parent.substring(0, parent.length() - 6));
			}
		}else if(file.isDirectory()){
			for(File f : file.listFiles()){
				files.addAll(scanFile(parent + "." + f.getName(), f));
			}
		}
		return files;
	}

	/**
	 * 
	 * <b>Description:    扫描依赖的jar</b><br>.
	 * @param roots
	 * @param interfaceClz
	 * @return	
	 * 
	 */
	private static HashSet<String> scanJarFileByInterface(String[] roots, Class<?> interfaceClz){
		HashSet<String> files = new HashSet<>();
		try {
			List<String> jars = getClassPathJars();
			jars.addAll(getLibJarFiles());
			for(String jar : jars){
				if(!jar.endsWith("jar")){
					continue;
				}
				files.addAll(scanJarFile4Interfaces(roots, interfaceClz, jar));
			}
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
		return files;
	}

    private static HashSet<String> scanJarFile4Interfaces(String[] roots,
            Class<?> interfaceClz, String jarFileName){
        JarFile jf = null;
        try {
            jf = new JarFile(jarFileName);
            HashSet<String> files = new HashSet<>();
            Enumeration<JarEntry> entries = jf.entries();
            while(entries.hasMoreElements()){
                JarEntry entry = entries.nextElement();
                if(!entry.getName().endsWith("class")){
                    continue;
                }
                for(String root : roots){
                    if(!isTargetInterfaceClass(root, interfaceClz, entry.getName())){
                        continue;
                    }
                    files.add(entry.getName());
                }
            }
            return files;
        } catch (Exception e) {
            throw new RabbitDDLException(e);
        } finally {
            closeJarFile(jf);
        }
    }

    /**
     * 
     * <b>Description: 判断是否是指定接口的基类 </b><br>.
     * @param root
     * @param interfaceClz
     * @param fileName
     * @return	
     * 
     */
    private static boolean isTargetInterfaceClass(String root, Class<?> interfaceClz, String fileName) {
        String className = fileName.substring(0, fileName.length() - 6).replaceAll("/", ".");
        if(className.startsWith(root.trim())){
            try{
                Class<?> clz = Class.forName(className);
                if(interfaceClz.isAssignableFrom(clz)){
                    return true;
                }
            }catch(Exception t){
                return false;
            }
        }
        return false;
    }

    private static void closeJarFile(JarFile jf) {
        if(null == jf){
            return;
        }
        try {
            jf.close();
        } catch (IOException e) {
            throw new RabbitDDLException(e);
        }
    }

	private static HashSet<String> scanJarFileByAnnotation(String[] roots, Class<? extends Annotation> anno){
		HashSet<String> files = new HashSet<>();
		try {
			List<String> jars = getClassPathJars();
			jars.addAll(getLibJarFiles());
			for(String jar : jars){
				if(!jar.endsWith("jar")){
					continue;
				}
				files.addAll(scanJars4Annotation(roots, anno, jar));
			}
		} catch (Exception e) {
		    logger.error(e.getMessage(), e);
		}
		return files;
	}

    private static HashSet<String> scanJars4Annotation(String[] roots,
            Class<? extends Annotation> anno, String jarFileName) {
        JarFile jf = null;
        try{
            jf = new JarFile(jarFileName);
            HashSet<String> files = new HashSet<>();
            Enumeration<JarEntry> entries = jf.entries();
            while(entries.hasMoreElements()){
                JarEntry entry = entries.nextElement();
                String name = entry.getName();
                if(!name.endsWith("class")){
                    continue;
                }
                name = name.substring(0, name.length() - 6).replaceAll("/", ".");
                for(String rootPath : roots){
                    if(!isTargetAnnoInterface(rootPath, anno, name)){
                        continue;
                    }
                    files.add(name);
                }
            }
            return files;
        } catch (Exception e){
            throw new RabbitDDLException(e);
        } finally {
            closeJarFile(jf);
        }
    }

    /**
     * 
     * <b>Description:    判断类{fileName}是否含有注解{anno}</b><br>.
     * @param rootPath
     * @param anno
     * @param fileName	
     * 
     */
    private static boolean isTargetAnnoInterface(String rootPath,
            Class<? extends Annotation> anno, String fileName) {
        if(fileName.startsWith(rootPath.trim())){
            try{
                Class<?> clz = Class.forName(fileName);
                if(null != clz.getAnnotation(anno)){
                    return true;
                }
            }catch(Exception t){
                return false;
            }
        }
        return false;
    }

	private static List<String> getClassPathJars() {
		String jars = System.getProperty("java.class.path");
		ArrayList<String> list = new ArrayList<>();
		if(null == jars || "".equals(jars.trim())){
			return list;
		}
		for(String f : jars.split(";")){
			list.add(f);
		}
		return list;
	}

	/**
	 * 
	 * 获取web项目的lib目录下jar包
	 * @return
	 * 
	 */
	private static List<String> getLibJarFiles() {
		try{
			String path = PackageScanner.class.getResource("/").getPath();
			File f = new File(path);
			f = f.getParentFile();
			for(File file : f.listFiles()){
				if("lib".equals(file.getName())){
					ArrayList<String> list = new ArrayList<>();
					String[] files = file.list();
					for(String fn : files){
						list.add(file.getAbsolutePath() + File.separator + fn);
					}
					return list;
				}
			}
		}catch(Exception e){
			
		}
		return new ArrayList<>();
	}
	
}
 