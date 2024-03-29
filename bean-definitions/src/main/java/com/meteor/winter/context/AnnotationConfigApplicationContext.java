package com.meteor.winter.context;

import com.meteor.winter.annotation.*;
import com.meteor.winter.exception.NoUniqueBeanDefinitionException;
import com.meteor.winter.io.PropertyResolver;
import com.meteor.winter.io.ResourceResolver;
import com.meteor.winter.util.ClassUtil;
import lombok.Getter;
import lombok.SneakyThrows;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.*;
import java.util.stream.Collectors;

public class AnnotationConfigApplicationContext implements ConfigurableApplicationContext{

    @Getter private Map<String,BeanDefinition> beans;

    // 这个字段的作用是解决强注入依赖循环问题
    // 如对于以下两个bean
    // ``` java
    // class A{ B b; A(B b) {this.b = b}}
    // class B{ A b; B(A a) {this.a = a}}
    // ```
    // 这是无论如何都无法解决的，所以我们要在创建Bean的时候将他们检查出来
    Set<String> creatingBeanNames;


    PropertyResolver propertyResolver;

    public AnnotationConfigApplicationContext(Class<?> configClass,PropertyResolver propertyResolver){
        Set<String> classNameSet = scanForClassName(configClass);
        this.beans = createBeanDefinitions(classNameSet);
        this.propertyResolver = propertyResolver;
        this.creatingBeanNames = new HashSet<>();

        Comparator<BeanDefinition> comparator = new Comparator<BeanDefinition>() {
            @Override
            public int compare(BeanDefinition o1, BeanDefinition o2) {
                return o2.getOrder() - o1.getOrder();
            }
        };


        // @Configuration注解的必须先创建出来（因为其中包含了Bean)
        this.beans.values().stream().filter(this::isConfigurationDef)
                .sorted(comparator)
                .forEach(beanDefinition -> createBeanForce(beanDefinition));


        // 其他的Bean
        this.beans.values().stream().filter(beanDefinition -> beanDefinition.getInstance()==null) // 过滤出所有实例还为空的BeanDef
                .sorted(comparator)
                .collect(Collectors.toList())
                .forEach(beanDefinition -> createBeanForce(beanDefinition));

        // 注入依赖
        this.beans.values().stream().forEach(beanDefinition -> injectProperties(beanDefinition,beanDefinition.getAClass(),beanDefinition.getInstance()));

        // 此时所有工作已经完成，调用bean的init钩子
        this.beans.values().stream().forEach(beanDefinition -> initBean(beanDefinition));

    }


    void destroyBean(BeanDefinition beanDefinition){


        if(beanDefinition.getDestroyMethod() == null) return;

        try {
            beanDefinition.getDestroyMethod().invoke(beanDefinition.getInstance());
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        }

    }

    void initBean(BeanDefinition beanDefinition){

        if(beanDefinition.getInitMethod() == null) return;

        try {
            beanDefinition.getInitMethod().invoke(beanDefinition.getInstance());
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean isConfigurationDef(BeanDefinition beanDefinition){
        return beanDefinition.getAClass().getAnnotation(Configuration.class)!=null;
    }

    /**
     * 这里处理工厂方法，构造器两种强注入
     * @param beanDefinition
     * @return
     */
    @SneakyThrows
    public Object createBeanForce(BeanDefinition beanDefinition){
        // 检测到重复创建导致的依赖循环
        if(!this.creatingBeanNames.add(beanDefinition.getName())){
            throw new Exception(String.format("%s 出现依赖注入循环",beanDefinition.getName()));
        }

        Executable executable = null;

        // 如果Bean实例以工厂方法创建的话
        if(beanDefinition.getFactoryMethod() !=null ){
            executable = beanDefinition.getFactoryMethod(); // 取得工厂方法
        }else executable = beanDefinition.getConstructor(); // 否则取得构造方法

        Parameter[] parameters = executable.getParameters();
        Annotation[][] parameterAnnotations = executable.getParameterAnnotations();
        // 将要填充的具体值
        Object[] args = new Object[parameters.length];
        for (int i = 0; i < parameters.length; i++) {
            Parameter parameter = parameters[i];
            Annotation[] parameterAnnotation = parameterAnnotations[i];

            Autowired autowired = ClassUtil.getAnnotation(parameterAnnotation, Autowired.class);
            Value value  = ClassUtil.getAnnotation(parameterAnnotation, Value.class);

            Class<?> type = parameter.getType();

            // 填充@Value注解的值
            if(value!=null){
                args[i] = propertyResolver.getProperty(value.value(),type);
            }else {
                // 如果是@AutoWried注入
                String name = autowired.name();

                // 判断name是否为空，如果是的话，根据类型查找依赖Bean
                BeanDefinition requiredBeanDef = name.isEmpty() ? findBeanDef(type)
                        :findBeanDef(name,type);

                Object instance = requiredBeanDef.getInstance();

                // 如果依赖的Bean未创建的话，递归调用本方法创建
                if(!isConfigurationDef(requiredBeanDef)&&instance == null){
                    instance = createBean(requiredBeanDef);
                }
                // 赋值
                args[i] = instance;
            }
        }

        Object beanInstance = null;

        if(beanDefinition.getFactoryMethod()!=null){
            Object bean = getBean(beanDefinition.getFactoryName());
            beanInstance = beanDefinition.getFactoryMethod().invoke(bean,args);
        }else beanInstance = beanDefinition.getConstructor().newInstance(args);

        String name = beanDefinition.getName();

        // 到了这里才装配实例
        // 事实上spring中做了更多（例如三级缓存）
        beanDefinition.setInstance(beanInstance);
        return beanInstance;
    }

    /**
     * 这里暂时用System.out充当日志，后续再统计更换
     */
    void logInfo(String log){
        System.out.println(log);
    }


    /**
     * 对字段和set方法进行注入
     * @param beanDefinition
     * @param aClass
     */
    void injectProperties(BeanDefinition beanDefinition,Class<?> aClass,Object bean){

        for (Field declaredField : aClass.getDeclaredFields()) {
            tryInject(beanDefinition,aClass,bean,declaredField);
        }

        for (Method declaredMethod : aClass.getDeclaredMethods()) {
            tryInject(beanDefinition,aClass,bean,declaredMethod);
        }

        // 在父类继续递归处理
        Class<?> superclass = aClass.getSuperclass();
        injectProperties(beanDefinition,superclass,bean);
    }

    /**
     * 尝试对某个字段进行注入
     * @param beanDefinition
     * @param aClass
     * @param bean
     * @param accessibleObject
     */
    void tryInject(BeanDefinition beanDefinition,Class<?> aClass,Object bean,AccessibleObject accessibleObject){

        Autowired autowired = accessibleObject.getAnnotation(Autowired.class);
        Value value = accessibleObject.getAnnotation(Value.class);

        if(autowired==null&&value==null) return;

        accessibleObject.setAccessible(true);

        Field field = null;
        Method method = null;

        if(accessibleObject instanceof Field) field = (Field) accessibleObject;
        else if(accessibleObject instanceof Method) method = (Method) accessibleObject;

        String beanName = field!=null ? field.getName() : method.getName();
        Class<?> type = field!=null ? field.getType() : method.getParameterTypes()[0];

        // 其实不应该用这么大作用域的try包裹，不过谁在乎呢
        try {

            if(field!=null){
                if(value!=null){
                    field.set(bean,propertyResolver.getProperty(value.value(),type));
                }
                if(autowired!=null){
                    String name = autowired.name();
                    Object depends = name.isEmpty() ? findBean(type) : findBean(name, type);
                    if(depends!=null){
                        field.set(bean,depends);
                    }
                }
            }
            if(method!=null){
                if(value!=null){
                    method.invoke(bean,propertyResolver.getProperty(value.value(),type));
                }
                if(autowired!=null){
                    String name = autowired.name();
                    Object depends = name.isEmpty() ? findBeans(type) : findBean(name,type);
                    method.invoke(bean,depends);
                }
            }
        }catch (Exception exception){

        }




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
                    Method initMethod = ClassUtil.getInitMethod(aClass);
                    Method destroyMethod = ClassUtil.getDestroyMethod(aClass);
                    BeanDefinition beanDefinition = new BeanDefinition(beanName, aClass, null, getConstructor(aClass),
                            // 工厂方法
                            null, null,
                            getOrder(aClass), aClass.isAssignableFrom(Primary.class),
                            initMethod==null ? null : initMethod.getName(), destroyMethod== null?null:destroyMethod.getName(),
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
                            // 扫描Bean的工厂方法
                            if(annotation!=null){
                                int modifiers = declaredMethod.getModifiers();
                                // 只处理public修饰的方法
                                if(Modifier.isPublic(modifiers)){
                                    Class<?> returnType = declaredMethod.getReturnType();
                                    BeanDefinition beanDefinitionFactory = new BeanDefinition(ClassUtil.getBeanName(declaredMethod), returnType,
                                            null, null, declaredMethod.getName(),
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
        return findBean(name,tClass);
    }

    @Override
    public <T> T getBean(Class<T> tClass) {
        BeanDefinition def = findBeanDef(tClass);
        return tClass.cast(def.getInstance());
    }

    @Override
    public <T> List<T> getBeans(Class<T> tClass) {
        return null;
    }

    @Override
    public void close() {

    }

    @Override
    public <T> List<T> findBeans(Class<T> type) {
        return beans.values().stream()
                .filter(beanDefinition -> type.isAssignableFrom(beanDefinition.getClass()))
                .map(beanDefinition -> type.cast(beanDefinition.getInstance()))
                .collect(Collectors.toList());
    }

    @Override
    public <T> T findBean(Class<T> type) throws NoUniqueBeanDefinitionException {
        List<BeanDefinition> beanDefinitions = findBeanDefs(type);
        if(beanDefinitions.isEmpty()) return null;
        else if(beanDefinitions.size() == 1) return type.cast(beanDefinitions.get(0));

        // 多个Bean时寻找@Primary注解
        List<BeanDefinition> primaryDefs = beanDefinitions.stream()
                .filter(beanDefinition -> beanDefinition.isPrimary()).collect(Collectors.toList());

        if(primaryDefs.isEmpty()){
            throw new NoUniqueBeanDefinitionException(String.format("不存在 bean: %s",type.getClass().getName()));
        }else if(primaryDefs.size()>1){
            throw new NoUniqueBeanDefinitionException(String.format("不存在全局唯一的bean: %s",type.getClass().getName()));
        }
        return type.cast(primaryDefs.get(0).getInstance());
    }

    @Override
    public Object findBean(String name) {
        return findBeanDef(name).getInstance();
    }


    private List<BeanDefinition> findBeanDefs(Class<?> type){
        return beans.values().stream().filter(beanDefinition -> type.isAssignableFrom(beanDefinition.getAClass()))
                .collect(Collectors.toList());
    }

    private BeanDefinition findBeanDef(Class<?> type){
        return findBeanDefs(type).get(0);
    }

    private BeanDefinition findBeanDef(String name){
        return beans.get(name);
    }

    private BeanDefinition findBeanDef(String name,Class<?> type){

        BeanDefinition beanDefinition = beans.get(name);
        if(beanDefinition == null) return null;

        if(beanDefinition.getAClass().isAssignableFrom(type)) return beanDefinition;

        return null;
    }


    @Override
    public <T> T findBean(String name, Class<T> type) {

        BeanDefinition beanDefinition = findBeanDef(name);
        if(beanDefinition == null)return null;
        if(type.isAssignableFrom(beanDefinition.getAClass())) return type.cast(beanDefinition.getInstance());

        return null;
    }

    @Override
    public Object createBean(BeanDefinition beanDefinition) {
        return null;
    }


}
