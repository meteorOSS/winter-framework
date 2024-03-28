package com.meteor.winter.context;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.ToString;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/**
 * 一个Bean的元数据拆分
 */
@Data
@AllArgsConstructor
@ToString
public class BeanDefinition {

    // 唯一的bean名
    private String name;

    // Bean的类型
    private Class<?> aClass;

    // Bean的实例
    private Object instance = null;

    // 构造方法
    private Constructor<?> constructor;
    // 工厂方法名
    private String factoryName;
    private Method factoryMethod;
    // Bean的顺序
    private int order;
    // 是否标识@Primary
    private boolean primary;

    private String initMethodName;
    private String destroyMethodName;

    private Method initMethod;
    private Method destroyMethod;
}
