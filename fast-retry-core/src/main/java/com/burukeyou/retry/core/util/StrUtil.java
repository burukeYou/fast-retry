package com.burukeyou.retry.core.util;

public class StrUtil {

    public static boolean isNotBlank(String str) {
        return !isBlank(str);
    }

    public static boolean isBlank(String str) {
        return str == null || str.isEmpty();
    }
}
