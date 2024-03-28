import com.meteor.winter.io.PropertyResolver;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.junit.Test;

import java.util.Properties;

public class TestProperty {

    @Data
    @AllArgsConstructor
    public class Student{
        private String name;
    }

    @Test
    public void test(){
        Properties properties = System.getProperties();
        properties.put("name","zsh");
        properties.put("hobby","code");
        properties.put("student",new Student("zsh"));
        PropertyResolver resolver = new PropertyResolver(properties);

        resolver.registerConvert(Student.class,(s)->{
            return new Student(s);
        });

        Student student = resolver.getProperty("student", Student.class);

        System.out.println(student.getName());

        System.out.println(resolver.getProperty("name"));
        // 嵌套查询
        System.out.println(resolver.getProperty("${wife:${girl_friend:lyf}}"));
    }

}
