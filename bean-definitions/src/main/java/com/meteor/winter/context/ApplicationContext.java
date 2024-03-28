package com.meteor.winter.context;

import java.util.List;

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
