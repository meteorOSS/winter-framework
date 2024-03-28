import com.meteor.winter.annotation.ComponentScan;
import com.meteor.winter.context.AnnotationConfigApplicationContext;
import com.meteor.winter.context.BeanDefinition;
import com.meteor.winter.io.PropertyResolver;
import test.AppConfig;
import test.pojo.Student;

import java.util.Properties;

public class TestAppConfig {
    public static void main(String[] args) {
        AnnotationConfigApplicationContext annotationConfigApplicationContext = new AnnotationConfigApplicationContext(AppConfig.class
        , createPropertyResolver());
        annotationConfigApplicationContext.getBeans().values().forEach(System.out::println);
        Student student = annotationConfigApplicationContext.getBean("Student", Student.class);
        System.out.println(student.getApp());
    }

    static PropertyResolver createPropertyResolver() {
        Properties ps = new Properties();
        ps.put("app.title", "Scan App");
        ps.put("app.version", "v1.0");
        ps.put("jdbc.url", "jdbc:hsqldb:file:testdb.tmp");
        ps.put("jdbc.username", "sa");
        ps.put("jdbc.password", "");
        ps.put("convert.boolean", "true");
        ps.put("convert.byte", "123");
        ps.put("convert.short", "12345");
        ps.put("convert.integer", "1234567");
        ps.put("convert.long", "123456789000");
        ps.put("convert.float", "12345.6789");
        ps.put("convert.double", "123456789.87654321");
        ps.put("convert.localdate", "2023-03-29");
        ps.put("convert.localtime", "20:45:01");
        ps.put("convert.localdatetime", "2023-03-29T20:45:01");
        ps.put("convert.zoneddatetime", "2023-03-29T20:45:01+08:00[Asia/Shanghai]");
        ps.put("convert.duration", "P2DT3H4M");
        ps.put("convert.zoneid", "Asia/Shanghai");
        PropertyResolver pr = new PropertyResolver(ps);
        return pr;
    }
}
