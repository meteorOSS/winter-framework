import com.meteor.winter.io.PropertyResolver;
import com.meteor.winter.util.YamlUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.junit.Test;

import java.util.Map;
import java.util.Properties;

public class TestProperty {

    @Data
    @AllArgsConstructor
    public class Student{
        private String name;
    }


    @Test
    public void testYamlLoad(){
        Map<String, Object> objectMap = YamlUtil.loadYamlAsPlainMap("test.yml");
        objectMap.keySet().forEach(System.out::println);
        objectMap.values().forEach(System.out::println);
    }

    @Test
    public void testYaml(){
        Map<String, Object> objectMap = YamlUtil.loadYamlAsPlainMap("test.yml");
        Properties properties = new Properties();
        properties.putAll(objectMap);
        PropertyResolver resolver = new PropertyResolver(properties);
        String property = resolver.getProperty("zsh.name");
        System.out.println(property);
        System.out.println(resolver.getProperty("info.wife"));
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
