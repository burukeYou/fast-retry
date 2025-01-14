package com.burukeyou.retry.spring;

import lombok.Data;

import java.lang.annotation.Annotation;

@Data
public class RetryAnnotationMeta {

    private FastRetry fastRetry;
    private Annotation subAnnotation;

    public RetryAnnotationMeta(FastRetry fastRetry, Annotation subAnnotation) {
        this.fastRetry = fastRetry;
        this.subAnnotation = subAnnotation;
    }
}
