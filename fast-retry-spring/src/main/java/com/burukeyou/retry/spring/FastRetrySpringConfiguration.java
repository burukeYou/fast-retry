package com.burukeyou.retry.spring;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FastRetrySpringConfiguration implements BeanFactoryAware {

    private BeanFactory beanFactory;

    @Bean
    public AnnotationRetryTaskFactory<?> fastRetryAnnotationRetryTaskFactory() {
        return new FastRetryAnnotationRetryTaskFactory(beanFactory);
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }
}
