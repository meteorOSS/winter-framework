import com.meteor.winter.annotation.ComponentScan;
import com.meteor.winter.context.AnnotationConfigApplicationContext;
import com.meteor.winter.context.BeanDefinition;
import com.meteor.winter.io.PropertyResolver;
import com.meteor.winter.util.YamlUtil;
import test.AppConfig;
import test.pojo.Apple;

import java.util.Map;
import java.util.Properties;

public class TestAppConfig {

    static PropertyResolver createPropertyResolver() {
        Properties ps = new Properties();
        ps.put("name", "apple");
        ps.put("price", "10$");
        ps.put("description", "这是IOC容器管理的一颗苹果");
        PropertyResolver pr = new PropertyResolver(ps);
        return pr;
    }

    public static void main(String[] args) {
        info();
        Map<String, Object> objectMap = YamlUtil.loadYamlAsPlainMap("context.yml");
        Properties properties = new Properties();
        properties.putAll(objectMap);
        PropertyResolver resolver = new PropertyResolver(properties);

        AnnotationConfigApplicationContext annotationConfigApplicationContext =
                new AnnotationConfigApplicationContext(AppConfig.class, resolver);

        annotationConfigApplicationContext.getBeans().values().forEach(System.out::println);
        Apple apple = annotationConfigApplicationContext.getBean("Apple", Apple.class);
        System.out.println(apple);
    }


    static void info(){
        System.out.println("\n" +
                " __      __ .__           __                   \n" +
                "/  \\    /  \\|__|  ____  _/  |_   ____  _______ \n" +
                "\\   \\/\\/   /|  | /    \\ \\   __\\_/ __ \\ \\_  __ \\\n" +
                " \\        / |  ||   |  \\ |  |  \\  ___/  |  | \\/\n" +
                "  \\__/\\  /  |__||___|  / |__|   \\___  > |__|   \n" +
                "       \\/            \\/             \\/         \n");
    }


}
