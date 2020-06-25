package rabbit.open.orm.core.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;

/**
 * <b>@description 包扫描工具 </b>
 */
@SuppressWarnings("serial")
public class PackageScanner implements Serializable {

    private static Logger logger = LoggerFactory.getLogger(PackageScanner.class);

    /**
     * 扫描含注解｛anno｝的类
     * @param	roots
     * @param	anno
     * @author  xiaoqianbin
     * @date    2020/6/24
     **/
    public static Set<String> filterByAnnotation(String[] roots, Class<? extends Annotation> anno) {
        Set<String> files = new HashSet<>();
        ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        try {
            Resource[] jarResources = resolver.getResources("classpath*:/**/*.jar");
            for (Resource jarResource : jarResources) {
                JarInputStream jis = new JarInputStream(jarResource.getInputStream());
                files.addAll(scanJarsByAnnotation(roots, anno, jis));
                jis.close();
            }
            List<String> classPathFiles = getClassPathFiles();
            for (String file : classPathFiles) {
                if (file.endsWith(".jar")) {
                    JarInputStream jis = new JarInputStream(new FileInputStream(new File(file)));
                    files.addAll(scanJarsByAnnotation(roots, anno, jis));
                    jis.close();
                } else if (new File(file).isDirectory()) {
                    URL base = new URL(new File(file).toURI().toString());
                    logger.info("scan directory {}", file);
                    files.addAll(scanDirectory(roots, anno, base));
                }
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return files;
    }

    private static Set<String> scanDirectory(String[] roots, Class<? extends Annotation> anno, URL base) {
        Set<String> files = new HashSet<>();
        for (String root : roots) {
            List<String> classes = scanURI(root.trim(), base);
            for (String name : classes) {
                if (hasAnnotation(anno, name)) {
                    files.add(name);
                }
            }
        }
        return files;
    }

    /**
     * 扫描包下所有class文件
     * @param rootPath  包路径
     * @param base      class文件路径
     * @return
     */
    private static List<String> scanURI(String rootPath, URL base) {
        List<String> files = new ArrayList<>();
        try {
            URL url = new URL(base, rootPath.replaceAll("\\.", "/"));
            File root = new File(url.toURI());
            if (root.exists() && root.isDirectory()) {
                for (File f : root.listFiles()) {
                    files.addAll(scanFile(rootPath + "." + f.getName(), f));
                }
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return files;
    }

    private static List<String> scanFile(String parent, File file) {
        while (parent.startsWith(".")) {
            parent = parent.substring(1);
        }
        List<String> files = new ArrayList<>();
        if (file.isFile()) {
            if (parent.endsWith(".class")) {
                files.add(parent.substring(0, parent.length() - 6));
            }
        } else if (file.isDirectory()) {
            for (File f : file.listFiles()) {
                files.addAll(scanFile(parent + "." + f.getName(), f));
            }
        }
        return files;
    }

    /**
     * 扫描jar中包含的class
     * @param	roots
     * @param	anno
     * @param	jf
     * @author  xiaoqianbin
     * @date    2020/6/24
     **/
    private static Set<String> scanJarsByAnnotation(String[] roots, Class<? extends Annotation> anno, JarInputStream jf) throws IOException {
        Set<String> files = new HashSet<>();
        String bootClassPath = "";
        Manifest manifest = jf.getManifest();
        String springBootClassPath = "Spring-Boot-Classes";
        if (null != manifest && null != manifest.getMainAttributes().getValue(springBootClassPath)) {
            bootClassPath = manifest.getMainAttributes().getValue(springBootClassPath);
        }
        while (true) {
            JarEntry entry = jf.getNextJarEntry();
            if (null == entry) {
                break;
            }
            for (String root : roots) {
                String rootPath = root.trim().replaceAll("\\.", "/");
                rootPath = bootClassPath + rootPath;
                String entryName = entry.getName();
                if (entryName.startsWith(rootPath) && entryName.endsWith(".class") && -1 == entryName.indexOf("$")) {
                    entryName = entryName.replaceAll("/", ".");
                    if (!"".equals(bootClassPath)) {
                        entryName = entryName.substring(bootClassPath.length());
                    }
                    entryName = entryName.substring(0, entryName.length() - 6);
                    if (hasAnnotation(anno, entryName)) {
                        files.add(entryName);
                    }
                }
            }
        }
        return files;
    }

    private static boolean hasAnnotation(Class<? extends Annotation> annotation, String name) {
        try {
            Class<?> clz = Class.forName(name);
            if (null != clz.getAnnotation(annotation)) {
                return true;
            }
        } catch (Exception | NoClassDefFoundError e) {
            //TO DO ignore
        }
        return false;
    }

    /**
     * 扫描项目的依赖包 即java.class.path的值
     * @return
     */
    public static List<String> getClassPathFiles() {
        String jars = System.getProperty("java.class.path");
        ArrayList<String> list = new ArrayList<>();
        if (null == jars || "".equals(jars.trim())) {
            return list;
        }
        for (String f : jars.split(";")) {
            list.add(f);
        }
        return list;
    }
}
 