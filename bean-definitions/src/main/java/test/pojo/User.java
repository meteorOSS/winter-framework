package test.pojo;

import com.meteor.winter.annotation.Component;

import javax.annotation.PostConstruct;

@Component(value = "User")
public class User {

    @PostConstruct
    public void post(){
    }

}
