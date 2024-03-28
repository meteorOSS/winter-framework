package test.pojo;

import com.meteor.winter.annotation.Component;
import com.meteor.winter.annotation.Value;

@Component(value = "Student")
public class Student {
    private String app;

    public Student(@Value(value = "app.title") String app){
        this.app = app;
    }

    public String getApp() {
        return app;
    }
}
