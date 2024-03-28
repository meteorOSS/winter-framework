package com.meteor.winter.context;

import com.meteor.winter.exception.NoUniqueBeanDefinitionException;

import java.util.List;

public interface ConfigurableApplicationContext extends ApplicationContext {
    List<BeanDefinition> findBeanDefinitions(Class<?> type);

    BeanDefinition findBeanDefinition(Class<?> type) throws NoUniqueBeanDefinitionException;

    BeanDefinition findBeanDefinition(String name);

    BeanDefinition findBeanDefinition(String name,Class<?> type);

    Object createBean(BeanDefinition beanDefinition);
}
