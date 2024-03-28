package com.meteor.winter.util;

import com.meteor.winter.annotation.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 一些反射工具
 */
public class ClassUtil {


    /**
     * 注意，spring中如@Service等注解也被@Component二次注解
     * 那么在查找时，要递归查找当前候选注解是否被其他注解标注 (有点绕)
     * @param target
     * @param annotation
     * @return
     * @param <A>
     */
    public static <A extends Annotation> A findAnnotation(Class<?> target,
                                                          Class<A> annotation){
        A a = target.getAnnotation(annotation);
        for (Annotation targetAnnotation : target.getAnnotations()) {
            Class<? extends Annotation> aClass = targetAnnotation.annotationType();
            if(!aClass.getPackage().getName().equals("java.lang.annotation")){
                A res = (A) findAnnotation(aClass,annotation);
                if(res !=null){
                    a = res;
                }
            }
        }
        return a;
    }


    public static String getBeanName(Method method){
        return method.getName();
    }

    public static String getBeanName(Class<?> aClass){
        String name = null;
        Component annotation = aClass.getAnnotation(Component.class);
        if(annotation!=null) return annotation.value();
        else {
            // 如果未找到@Component，则继续再其他注解中查找
            for (Annotation aClassAnnotation : aClass.getAnnotations()) {

                if(findAnnotation(aClassAnnotation.annotationType(), Component.class)!=null){
                    try {
                        name = (String)aClassAnnotation.getClass().getMethod("value").invoke(aClassAnnotation);
                    } catch (IllegalAccessException e) {
                        throw new RuntimeException(e);
                    } catch (InvocationTargetException e) {
                        throw new RuntimeException(e);
                    } catch (NoSuchMethodException e) {
                        throw new RuntimeException(e);
                    }
                }

            }
        }
        if(name == null) {
            name = aClass.getSimpleName();
        }
        return name;
    }

    private static Method findAnnotationMethod(Class<?> aClass,Class<? extends Annotation> annotation){
        List<Method> collect = Arrays.stream(aClass.getDeclaredMethods())
                .filter(method -> method.isAnnotationPresent(annotation))
                .collect(Collectors.toList());

        if(collect.isEmpty()) return null;

        return collect.get(0);
    }

    /**
     * 获取init方法
     */
    public static Method getInitMethod(Class<?> aClass){
        return findAnnotationMethod(aClass, PostConstruct.class);
    }

    /**
     * 获取Destroy方法
     */
    public static Method getDestroyMethod(Class<?> aCalss){
        return findAnnotationMethod(aCalss, PreDestroy.class);
    }
}
