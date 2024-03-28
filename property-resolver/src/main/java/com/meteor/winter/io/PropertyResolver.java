package com.meteor.winter.io;

import com.sun.istack.internal.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class PropertyResolver {
    private Map<String,String> properties
            = new HashMap<>();

    public PropertyResolver(Properties pros){
        // 存入环境变量
        this.properties.putAll(System.getenv());
        // 存入Properties
        pros.stringPropertyNames()
                .stream().forEach(s -> properties.put(s,pros.getProperty(s)));
    }

    @Nullable
    public String getProperty(String key){

        PropertyExpr propertyExpr = parsePropertyExpr(key);
        if(propertyExpr == null) return properties.get(key);

        return properties.get(key);
    }

    // 格式化表达式
    private PropertyExpr parsePropertyExpr(String key){
        if(key.startsWith("${")&&key.endsWith("}")){
            // 是否存在默认值
            int def = key.indexOf(":");
            if(def==-1) {
                String k = key.substring(2,key.length()-1);
                return new PropertyExpr(k,null);
            }else {
                String k = key.substring(2,def);
                return new PropertyExpr(k,key.substring(def+1,key.length()-1));
            }
        }
        return null;
    }
}
