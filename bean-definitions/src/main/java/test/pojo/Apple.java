package test.pojo;

import com.meteor.winter.annotation.Component;
import com.meteor.winter.annotation.Value;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Component(value = "Apple")
@Getter
@Setter
@ToString
public class Apple {
    private String name;
    private String price;
    private String description;
    public Apple(@Value(value = "name") String name,@Value(value = "price") String price
    ,@Value(value = "description")String description){
        this.name = name;
        this.price = price;
        this.description = description;
    }


}
