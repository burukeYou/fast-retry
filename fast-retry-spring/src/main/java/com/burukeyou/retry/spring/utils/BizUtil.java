package com.burukeyou.retry.spring.utils;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

public class BizUtil {

    public static Class<?> getSuperClassParamFirstClass(Class<?> clz,Class<?> superClass){
        ParameterizedType parameterizedType = getSuperInterfacesParameterizedType(clz, superClass);
        Type[] arr = parameterizedType.getActualTypeArguments();
        return (Class<?>)arr[0];
    }
    public static ParameterizedType getSuperInterfacesParameterizedType(Class<?> clazz, Class<?> genericInterfaceClass) {
        Class<?> current = clazz;
        ParameterizedType genericClassParameterizedType = null;
        while (current != null) {
            Type[] genericInterfaces = current.getGenericInterfaces();
            if (genericInterfaces.length <= 0){
                current = current.getSuperclass();
                continue;
            }

            for (Type genericInterface : genericInterfaces) {
                if (genericInterface instanceof ParameterizedType){
                    ParameterizedType parameterizedType =  ((ParameterizedType)genericInterface);
                    if (genericInterfaceClass.equals(parameterizedType.getRawType())){
                        genericClassParameterizedType = parameterizedType;
                    }
                }
            }

            if (genericClassParameterizedType != null){
                break;
            }
            current = current.getSuperclass();
        }
        return genericClassParameterizedType;
    }

    public static <T> T getBeanOrNew(Class<T> beanClass,BeanFactory beanFactory){
        if (beanClass == null){
            return null;
        }
        try {
            return beanFactory.getBean(beanClass);
        } catch (NoSuchBeanDefinitionException e) {
            try {
                return beanClass.newInstance();
            } catch (InstantiationException | IllegalAccessException ex) {
                throw new RuntimeException(ex);
            }
        }
    }
}
