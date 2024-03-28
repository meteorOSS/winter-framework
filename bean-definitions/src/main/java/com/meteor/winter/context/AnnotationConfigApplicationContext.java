package com.meteor.winter.context;

import com.meteor.winter.annotation.*;
import com.meteor.winter.exception.BeanDefinitionException;
import com.meteor.winter.exception.NoUniqueBeanDefinitionException;
import com.meteor.winter.io.PropertyResolver;
import com.meteor.winter.io.Resource;
import com.meteor.winter.io.ResourceResolver;
import com.meteor.winter.util.ClassUtil;
import lombok.Getter;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.stream.Collectors;

public class AnnotationConfigApplicationContext implements ConfigurableApplicationContext{

    @Getter private Map<String,BeanDefinition> beans;


    public AnnotationConfigApplicationContext(Class<?> configClass){
        Set<String> classNameSet = scanForClassName(configClass);
        this.beans = createBeanDefinitions(classNameSet);
    }

    /**
     * 这里暂时用System.out充当日志，后续再统计更换
     */
    void logInfo(String log){
        System.out.println(log);
    }

    /**
     * 根据ClassName集合创建BeanDefinition字典
     * @param classNames
     * @return
     */
    Map<String,BeanDefinition> createBeanDefinitions(Set<String> classNames){

        Map<String,BeanDefinition> beanDefinitionMap = new HashMap<>();
        for (String className : classNames) {
            logInfo("扫描到:"+className);
            try {
                Class<?> aClass = Class.forName(className);
                Component component = ClassUtil.findAnnotation(aClass, Component.class);
                if(component!=null){
                    // bean的唯一名称
                    String beanName = ClassUtil.getBeanName(aClass);
                    BeanDefinition beanDefinition = new BeanDefinition(beanName, aClass, null, getConstructor(aClass),
                            // 工厂方法
                            null, null,
                            getOrder(aClass), aClass.isAssignableFrom(Primary.class),
                            null, null,
                            // init/destroy
                            ClassUtil.getInitMethod(aClass), ClassUtil.getDestroyMethod(aClass)
                    );
                    beanDefinitionMap.put(beanName,beanDefinition);

                    Configuration configuration = ClassUtil.findAnnotation(aClass, Configuration.class);
                    if(configuration!=null){

                        // 如果有@Configuration注解的话，被@Bean注解的将是工厂方法
                        // 将他们也扫描进来
                        for (Method declaredMethod : aClass.getDeclaredMethods()) {
                            Bean annotation = declaredMethod.getAnnotation(Bean.class);
                            if(annotation!=null){
                                int modifiers = declaredMethod.getModifiers();
                                // 只处理public修饰的方法
                                if(Modifier.isPublic(modifiers)){
                                    Class<?> returnType = declaredMethod.getReturnType();
                                    BeanDefinition beanDefinitionFactory = new BeanDefinition(ClassUtil.getBeanName(declaredMethod), returnType,
                                            null, null, null,
                                            declaredMethod, getOrder(declaredMethod),
                                            declaredMethod.isAnnotationPresent(Primary.class),
                                            annotation.initMethod(), annotation.destroyMethod(),
                                            null, null);
                                    beanDefinitionMap.put(beanDefinitionFactory.getName(),beanDefinitionFactory);
                                }
                            }
                        }
                    }

                }
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }

        return beanDefinitionMap;

    }

    int getOrder(Method method) {
        Order order = method.getAnnotation(Order.class);
        return order == null ? Integer.MAX_VALUE : order.value();
    }

    int getOrder(Class<?> clazz) {
        Order order = clazz.getAnnotation(Order.class);
        return order == null ? Integer.MAX_VALUE : order.value();
    }

    Constructor<?> getConstructor(Class<?> aClass){
        Constructor<?>[] constructors = aClass.getConstructors();

        // 如果没有公开的构造方法，则调取私有的构造方法
        if(constructors.length == 0){
            constructors = aClass.getDeclaredConstructors();
        }

        return constructors[0];

    }

    Set<String> scanForClassName(Class<?> configClass){
        // 获取@ComponentsScan注解
        ComponentScan annotation = ClassUtil.findAnnotation(configClass, ComponentScan.class);
        String[] packages = annotation.value();
        Set<String> classNameSet = new HashSet<>();
        for (String aPackage : packages) {
            logInfo("开始扫描包: "+aPackage);
            ResourceResolver resolver = new ResourceResolver(aPackage);
            List<String> scan = resolver.scan(resource -> {
                String name = resource.getName();
                if (name.endsWith(".class")) {
                    return name.replace("\\", ".")
                            .substring(0, name.length() - 6);
                }
                return null;
            });
            classNameSet.addAll(scan);
        }

        /**
         * 像Spring中的import注解一样，导入其他Class文件的注解
         */
        Optional.ofNullable(configClass.getAnnotation(Import.class)).ifPresent(anImport -> {
            Class<?>[] value = anImport.value();
            for (Class<?> aClass : value) {
                classNameSet.add(aClass.getName());
            }
        });
        return classNameSet;
    }



    @Override
    public boolean containsBean(String name) {
        return beans.containsKey(name);
    }

    @Override
    public <T> T getBean(String name) {
        BeanDefinition beanDefinition = this.beans.get(name);
        return (T) beanDefinition.getInstance();
    }

    @Override
    public <T> T getBean(String name, Class<T> tClass) {
//        return findBeanDefinition(name,tClass);
        return null;
    }

    @Override
    public <T> T getBean(Class<T> tClass) {
        return null;
    }

    @Override
    public <T> List<T> getBeans(Class<T> tClass) {
        return null;
    }

    @Override
    public void close() {

    }

    @Override
    public List<BeanDefinition> findBeanDefinitions(Class<?> type) {
        return beans.values().stream()
                .filter(beanDefinition -> type.isAssignableFrom(beanDefinition.getClass()))
                .collect(Collectors.toList());
    }

    @Override
    public BeanDefinition findBeanDefinition(Class<?> type) throws NoUniqueBeanDefinitionException {
        List<BeanDefinition> beanDefinitions = findBeanDefinitions(type);
        if(beanDefinitions.isEmpty()) return null;
        else if(beanDefinitions.size() == 1) return beanDefinitions.get(0);

        // 多个Bean时寻找@Primary注解
        List<BeanDefinition> primaryDefs = beanDefinitions.stream()
                .filter(beanDefinition -> beanDefinition.isPrimary()).collect(Collectors.toList());

        if(primaryDefs.isEmpty()){
            throw new NoUniqueBeanDefinitionException(String.format("不存在 bean: %s",type.getClass().getName()));
        }else if(primaryDefs.size()>1){
            throw new NoUniqueBeanDefinitionException(String.format("不存在全局唯一的bean: %s",type.getClass().getName()));
        }
        return primaryDefs.get(0);
    }

    @Override
    public BeanDefinition findBeanDefinition(String name) {
        return beans.get(name);
    }

    @Override
    public BeanDefinition findBeanDefinition(String name, Class<?> type) {
        return null;
    }

    @Override
    public Object createBean(BeanDefinition beanDefinition) {
        return null;
    }
}
