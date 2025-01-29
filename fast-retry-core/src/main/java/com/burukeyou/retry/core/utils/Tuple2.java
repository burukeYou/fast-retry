package com.burukeyou.retry.core.utils;

import lombok.Data;

@Data
public class Tuple2<T1,T2> {

    private T1 c1;
    private T2 c2;

    public Tuple2() {
    }

    public Tuple2(T1 c1, T2 c2) {
        this.c1 = c1;
        this.c2 = c2;
    }

}
