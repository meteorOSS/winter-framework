package com.meteor.winter.exception;

public class NoUniqueBeanDefinitionException
extends BeanDefinitionException{
    public NoUniqueBeanDefinitionException(String mes) {
        super(mes);
    }
}
