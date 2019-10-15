package rabbit.open.orm.core.utils;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rabbit.open.orm.common.exception.RabbitDDLException;

/**
 * <b>@description 包扫描工具 </b>
 */
@SuppressWarnings("serial")
public class PackageScanner implements Serializable {

    private static Logger logger = LoggerFactory.getLogger(PackageScanner.class);

    /**
     * 过滤包含特定注解的类
     * @param anno
     * @return
     */
    public static Set<String> filterByAnnotation(String[] roots,
                 Class<? extends Annotation> anno, boolean scanJar) {
        URL base = PackageScanner.class.getClassLoader().getResource("");
        Set<String> targets = scanDirectory(roots, anno, base);
        if (scanJar) {
            targets.addAll(scanJarFileByAnnotation(roots, anno));
        }
        return targets;
    }

    private static boolean hasAnnotation(Class<? extends Annotation> annotation,
                                         String name) {
        try {
            Class<?> clz = Class.forName(name);
            if (null != clz.getAnnotation(annotation)) {
                return true;
            }
        } catch (Exception | NoClassDefFoundError e) {
            logger.error(e.getMessage());
        }
        return false;
    }

    /**
     * 过滤实现了特定接口的类
     * @param interfaceClz
     * @return
     */
    public static Set<String> filterByInterface(String[] roots,
                                                Class<?> interfaceClz, boolean scanJar) {
        URL base = PackageScanner.class.getClassLoader().getResource("");
        Set<String> targets = scanDirectory(roots, base, interfaceClz);
        if (scanJar) {
            targets.addAll(scanJarFileByInterface(roots, interfaceClz));
        }
        return targets;
    }

    private static boolean isTargetClass(Class<?> interfaceClz, String name) {
        try {
            Class<?> clz = Class.forName(name);
            if (interfaceClz.isAssignableFrom(clz)) {
                return true;
            }
        } catch (Exception | NoClassDefFoundError e) {
            logger.error(e.getMessage());
        }
        return false;
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
            if (url.getFile().contains(".war!") || url.getFile().contains(".jar!")) {
                String feature = ".war";
                if (url.getFile().contains(".jar!")) {
                    feature = ".jar";
                }
                String warName = url.getFile().substring(0, url.getFile().indexOf(feature)) + feature;
                if (warName.startsWith("file:")) {
                    if (System.getProperty("os.name").toLowerCase().startsWith("win")) {
                        warName = warName.substring(6, warName.length());
                    } else {
                        warName = warName.substring(5, warName.length());
                    }
                }
                files.addAll(getClassesFromWar(warName));
            } else {
                File root = new File(url.toURI());
                if (root.exists() && root.isDirectory()) {
                    logger.info("scan path: " + root.getPath().replaceAll("\\\\", "/"));
                    for (File f : root.listFiles()) {
                        files.addAll(scanFile(rootPath + "." + f.getName(), f));
                    }
                }
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return files;
    }

    /**
     * <b>Description  读取war中/WEB-INF/classes/目录下的class文件</b>
     * @param war
     * @throws IOException
     */
    private static List<String> getClassesFromWar(String war) throws IOException {
        JarFile jar = new JarFile(war);
        Enumeration<JarEntry> entries = jar.entries();
        List<String> classes = new ArrayList<>();
        //遍历条目。 
        String[] prefixs = new String[]{"WEB-INF/classes/", "BOOT-INF/classes/"};
        while (entries.hasMoreElements()) {
            JarEntry ele = entries.nextElement();
            for (String prefix : prefixs) {
                if (ele.getName().startsWith(prefix) && ele.getName().endsWith(".class")) {
                    String clzName = ele.getName().substring(prefix.length(), ele.getName().length() - 6).replaceAll("/", ".");
                    classes.add(clzName);
                }
            }
        }
        jar.close();
        return classes;
    }

    private static List<String> scanFile(String parent, File file)
            throws Exception {
        while (parent.startsWith(".")) {
            parent = parent.substring(1, parent.length());
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
     * <b>Description:    扫描依赖的jar</b><br>.
     * @param roots
     * @param interfaceClz
     * @return
     */
    private static HashSet<String> scanJarFileByInterface(String[] roots, Class<?> interfaceClz) {
        HashSet<String> files = new HashSet<>();
        try {
            List<String> jars = getClassPathJars();
            jars.addAll(getLibJarFiles());
            for (String jar : jars) {
                if (!jar.endsWith("jar")) {
                    if (new File(jar).isDirectory()) {
                        URL base = new URL(new File(jar).toURI().toString());
                        logger.info("scan directory [" + jar + "] for interface");
                        files.addAll(scanDirectory(roots, base, interfaceClz));
                    }
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
                  Class<?> interfaceClz, String jarFileName) throws IOException {
        JarFile jf = null;
        try {
            jf = new JarFile(jarFileName);
            HashSet<String> files = new HashSet<>();
            Enumeration<JarEntry> entries = jf.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                files.addAll(scanInterfacesByEntry(roots, interfaceClz, entry));
            }
            return files;
        } finally {
            closeJarFile(jf);
        }
    }

    private static HashSet<String> scanInterfacesByEntry(String[] roots,
                     Class<?> interfaceClz, JarEntry entry) {
        HashSet<String> files = new HashSet<>();
        if (!entry.getName().endsWith("class")) {
            return files;
        }
        for (String root : roots) {
            if (!isTargetInterfaceClass(root, interfaceClz,
                    entry.getName())) {
                continue;
            }
            files.add(entry.getName()
                    .substring(0, entry.getName().length() - 6)
                    .replaceAll("/", "."));
        }
        return files;
    }

    /**
     * <b>Description: 判断是否是指定接口的基类 </b><br>.
     * @param root
     * @param interfaceClz
     * @param fileName
     * @return
     */
    private static boolean isTargetInterfaceClass(String root, Class<?> interfaceClz, String fileName) {
        String className = fileName.substring(0, fileName.length() - 6)
                .replaceAll("/", ".");
        if (className.startsWith(root.trim())) {
            try {
                Class<?> clz = Class.forName(className);
                if (interfaceClz.isAssignableFrom(clz)) {
                    return true;
                }
            } catch (NoClassDefFoundError | ClassNotFoundException e) {
                return false;
            }
        }
        return false;
    }

    public static void closeJarFile(JarFile jf) {
        if (null == jf) {
            return;
        }
        try {
            jf.close();
        } catch (IOException e) {
            throw new RabbitDDLException(e);
        }
    }

    private static HashSet<String> scanJarFileByAnnotation(String[] roots, Class<? extends Annotation> anno) {
        HashSet<String> files = new HashSet<>();
        try {
            List<String> jars = getClassPathJars();
            jars.addAll(getLibJarFiles());
            for (String jar : jars) {
                if (!jar.endsWith("jar")) {
                    if (new File(jar).isDirectory()) {
                        URL base = new URL(new File(jar).toURI().toString());
                        logger.info("scan directory " + jar);
                        files.addAll(scanDirectory(roots, anno, base));
                    }
                    continue;
                }
                files.addAll(scanJars4Annotation(roots, anno, jar));
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
     * 扫描包下的接口
     * @param roots
     * @param base
     * @param interfaceClz
     * @return
     */
    private static Set<String> scanDirectory(String[] roots, URL base, Class<?> interfaceClz) {
        Set<String> files = new HashSet<>();
        for (String root : roots) {
            List<String> classes = scanURI(root, base);
            for (String name : classes) {
                if (isTargetClass(interfaceClz, name)) {
                    files.add(name);
                }
            }
        }
        return files;
    }

    private static HashSet<String> scanJars4Annotation(String[] roots,
                   Class<? extends Annotation> anno, String jarFileName) throws IOException {
        JarFile jf = null;
        try {
            jf = new JarFile(jarFileName);
            HashSet<String> files = new HashSet<>();
            Enumeration<JarEntry> entries = jf.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                files.addAll(scanAnnotationByEntry(roots, anno, entry));
            }
            return files;
        } finally {
            closeJarFile(jf);
        }
    }

    private static HashSet<String> scanAnnotationByEntry(String[] roots,
                         Class<? extends Annotation> anno, JarEntry entry) {
        HashSet<String> files = new HashSet<>();
        String name = entry.getName();
        if (!name.endsWith("class")) {
            return files;
        }
        name = name.substring(0, name.length() - 6)
                .replaceAll("/", ".");
        for (String rootPath : roots) {
            if (!isTargetAnnoInterface(rootPath, anno, name)) {
                continue;
            }
            files.add(name);
        }
        return files;
    }

    /**
     * <b>Description:    判断类{fileName}是否含有注解{anno}</b><br>.
     *
     * @param rootPath
     * @param anno
     * @param fileName
     */
    private static boolean isTargetAnnoInterface(String rootPath,
                     Class<? extends Annotation> anno, String fileName) {
        if (fileName.startsWith(rootPath.trim())) {
            try {
                Class<?> clz = Class.forName(fileName);
                if (null != clz.getAnnotation(anno)) {
                    return true;
                }
            } catch (NoClassDefFoundError | ClassNotFoundException e) {
                return false;
            }
        }
        return false;
    }

    /**
     * 扫描项目的依赖包 即java.class.path的值
     * @return
     */
    public static List<String> getClassPathJars() {
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

    /**
     * 获取web项目的lib目录下jar包
     *
     * @return
     */
    private static List<String> getLibJarFiles() {
        String path = PackageScanner.class.getResource("/").getPath();
        File f = new File(path);
        f = f.getParentFile();
        if (null == f.listFiles()) {
            return new ArrayList<>();
        }
        for (File file : f.listFiles()) {
            if ("lib".equals(file.getName())) {
                ArrayList<String> list = new ArrayList<>();
                String[] files = file.list();
                for (String fn : files) {
                    list.add(file.getAbsolutePath() + File.separator + fn);
                }
                return list;
            }
        }
        return new ArrayList<>();
    }

}
 