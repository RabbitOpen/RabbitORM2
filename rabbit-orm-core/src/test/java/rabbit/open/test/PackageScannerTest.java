package rabbit.open.test;

import junit.framework.TestCase;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import rabbit.open.orm.core.annotation.Entity;
import rabbit.open.orm.core.utils.PackageScanner;

import java.util.Set;

@RunWith(JUnit4.class)
public class PackageScannerTest {


    @Test
    public void scanAnnotationsTest() {
        Set<String> clzes = PackageScanner.filterByAnnotation(
                new String[] { "rabbit", "com.alibaba" }, Entity.class);
        System.out.println(clzes);
        TestCase.assertTrue(clzes.size() > 1);
    }

}
