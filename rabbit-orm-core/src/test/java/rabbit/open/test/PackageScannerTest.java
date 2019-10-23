package rabbit.open.test;

import java.util.Set;

import javax.sql.DataSource;

import junit.framework.TestCase;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import rabbit.open.orm.core.annotation.Entity;
import rabbit.open.orm.core.utils.PackageScanner;

@RunWith(JUnit4.class)
public class PackageScannerTest {

    @Test
    public void scanInterfaceTest() {
        Set<String> clzes = PackageScanner.filterByInterface(
                new String[] { "rabbit", "com.alibaba" }, DataSource.class, true);
        System.out.println(clzes);
        TestCase.assertTrue(clzes.size() > 1);
    }

    @Test
    public void scanAnnotationsTest() {
        Set<String> clzes = PackageScanner.filterByAnnotation(
                new String[] { "rabbit", "com.alibaba" }, Entity.class, false);
        System.out.println(clzes);
        TestCase.assertTrue(clzes.size() > 1);
    }
}
