package com.meteor.winter.io;

import com.sun.istack.internal.Nullable;

import java.time.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.function.Function;

public class PropertyResolver {
    private Map<String,String> properties
            = new HashMap<>();


    // 这个map用于类型强转
    private Map<Class<?>, Function<String,Object>> classFunctionMap = new HashMap<>();


    public PropertyResolver(Properties pros){
        // 存入环境变量
        this.properties.putAll(System.getenv());
        // 存入Properties
        pros.stringPropertyNames()
                .stream().forEach(s -> properties.put(s,pros.getProperty(s)));

        classFunctionMap.put(String.class,s->s);
        classFunctionMap.put(Boolean.class,s->Boolean.parseBoolean(s));

        classFunctionMap.put(Boolean.class, s -> Boolean.valueOf(s));

        classFunctionMap.put(byte.class, s -> Byte.parseByte(s));
        classFunctionMap.put(Byte.class, s -> Byte.valueOf(s));

        classFunctionMap.put(short.class, s -> Short.parseShort(s));
        classFunctionMap.put(Short.class, s -> Short.valueOf(s));

        classFunctionMap.put(int.class, s -> Integer.parseInt(s));
        classFunctionMap.put(Integer.class, s -> Integer.valueOf(s));

        classFunctionMap.put(long.class, s -> Long.parseLong(s));
        classFunctionMap.put(Long.class, s -> Long.valueOf(s));

        classFunctionMap.put(float.class, s -> Float.parseFloat(s));
        classFunctionMap.put(Float.class, s -> Float.valueOf(s));

        classFunctionMap.put(double.class, s -> Double.parseDouble(s));
        classFunctionMap.put(Double.class, s -> Double.valueOf(s));

        classFunctionMap.put(LocalDate.class, s -> LocalDate.parse(s));
        classFunctionMap.put(LocalTime.class, s -> LocalTime.parse(s));
        classFunctionMap.put(LocalDateTime.class, s -> LocalDateTime.parse(s));
        classFunctionMap.put(ZonedDateTime.class, s -> ZonedDateTime.parse(s));
        classFunctionMap.put(Duration.class, s -> Duration.parse(s));
        classFunctionMap.put(ZoneId.class, s -> ZoneId.of(s));

    }

    @Nullable
    public String getProperty(String key){
        PropertyExpr propertyExpr = parsePropertyExpr(key);
        if(propertyExpr!=null){
            if(propertyExpr.getDefaultValue()!=null) {
                return getProperty(propertyExpr.getKey(),propertyExpr.getDefaultValue());
            }else {
                return getRequiredProperty(propertyExpr.getKey());
            }
        }
        String value = this.properties.get(key);
        if(value!=null) {
            return parseValue(value);
        }
        return null;
    }

    @Nullable
    public <T> T getProperty(String key,Class<T> castType){
        String value = getProperty(key);
        return convert(castType,value);
    }

    // 强转类型
    <T> T convert(Class<T> castType,String value){
        Function<String, Object> stringObjectFunction = this.classFunctionMap.get(castType);
        if(stringObjectFunction == null) {
            throw new IllegalStateException(String.format("不支持 %s 类型的强转",castType.getName()));
        }
        return (T) stringObjectFunction.apply(value);
    }

    // 对外提供接口，注册Convert函数
    public void registerConvert(Class<?> classType,Function<String,Object> function){
        this.classFunctionMap.put(classType,function);
    }

    // 递归打印值
    private String parseValue(String value){

        PropertyExpr propertyExpr = parsePropertyExpr(value);
        if(propertyExpr!=null){
            if(propertyExpr.getDefaultValue()!=null) {
                return getProperty(propertyExpr.getKey(),propertyExpr.getDefaultValue());
            }else {
                return getRequiredProperty(propertyExpr.getKey());
            }
        }

        return value;

    }

    public String getRequiredProperty(String key){
        String value = getProperty(key);
        return Objects.requireNonNull(value,String.format("Property %s not found",key));
    }

    public String getProperty(String key,String defaultValue){
        String value = getProperty(key);
        return value !=null ? value : parseValue(defaultValue);
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
