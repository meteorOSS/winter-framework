package com.meteor.winter.context;

import com.meteor.winter.exception.NoUniqueBeanDefinitionException;

import java.util.List;

public interface ConfigurableApplicationContext extends ApplicationContext {
    <T> List<T> findBeans(Class<T> type);

    <T> T findBean(Class<T> type) throws NoUniqueBeanDefinitionException;

    <T> T findBean(String name);

    <T> T findBean(String name,Class<T> type);

    Object createBean(BeanDefinition beanDefinition);
}
