仿Spring的小玩具，目前具备了

> IOC容器

winter自带了一个IOC容器模块，它的使用基本与Spring相同

接口声明

``` java
public interface ApplicationContext extends AutoCloseable{
    /**
     * 是否存在指定name的Bean
     */
    boolean containsBean(String name);

    /**
     * 根据name返回唯一的Bean
     */
    <T> T getBean(String name);

    /**
     * 寻找指定类型的唯一Bean
     * @param name
     * @param tClass
     * @return
     * @param <T>
     */
    <T> T getBean(String name,Class<T> tClass);

    /**
     * 根据类型查找Bean
     * @param tClass
     * @return
     * @param <T>
     */
    <T> T getBean(Class<T> tClass);

    /**
     * 根据Type返回一组Bean
     */
    <T> List<T> getBeans(Class<T> tClass);

    /**
     * 关闭并执行所有bean的destroy方法
     */
    void close();
}
```

`AnnotationConfigApplicationContext` 是它的实现类，目前只支持对Bean的单例管理

## 待办:

> 实现AOP
JDBC和事务管理
WebMVC


