package com.burukeyou.retry.core.support;

import com.burukeyou.retry.core.annotations.FastRetry;
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
