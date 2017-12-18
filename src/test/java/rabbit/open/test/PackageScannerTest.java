package rabbit.open.test;

import java.util.Set;

import javax.sql.DataSource;

import junit.framework.TestCase;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import rabbit.open.orm.annotation.Entity;
import rabbit.open.orm.ddl.PackageScanner;

@RunWith(JUnit4.class)
public class PackageScannerTest {

    @Test
    public void scanInterfaceTest(){
        Set<String> clzes = PackageScanner.filterByInterface(new String[]{"rabbit"}, DataSource.class);
        System.out.println(clzes);
        TestCase.assertTrue(clzes.size() > 1);
    }

    @Test
    public void scanAnnotationsTest(){
        Set<String> clzes = PackageScanner.filterByAnnotation(new String[]{"rabbit"}, Entity.class);
        System.out.println(clzes);
        TestCase.assertTrue(clzes.size() > 1);
    }
}
