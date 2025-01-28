package com.burukeyou.retry.demo.data;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;

public class BaseSpringTest {

    public static AnnotationConfigApplicationContext context;



    public static void  initContext(Class<?>...registerClass){
            context = createContext("com.burukeyou.demo.retry",registerClass);
    }

    public static AnnotationConfigApplicationContext createContext(String packageName,Class<?>...registerClass) {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        ConfigurableEnvironment environment = context.getEnvironment();
        context.setEnvironment(environment);
        context.scan(packageName);

        for (Class<?> aClass : registerClass) {
            context.register(aClass);
        }

        context.refresh();
        return context;
    }

}
