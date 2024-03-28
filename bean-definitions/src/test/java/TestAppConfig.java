import com.meteor.winter.annotation.ComponentScan;
import com.meteor.winter.context.AnnotationConfigApplicationContext;
import com.meteor.winter.context.BeanDefinition;
import test.AppConfig;

public class TestAppConfig {
    public static void main(String[] args) {
        AnnotationConfigApplicationContext annotationConfigApplicationContext = new AnnotationConfigApplicationContext(AppConfig.class);
        annotationConfigApplicationContext.getBeans().values().forEach(System.out::println);
    }
}
