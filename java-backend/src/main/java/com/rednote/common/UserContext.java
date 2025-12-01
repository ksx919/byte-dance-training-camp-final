package com.rednote.common;

public class UserContext {
    private static final ThreadLocal<Long> userHolder = new ThreadLocal<>();

    public static void setUserId(Long userId) {
        userHolder.set(userId);
    }

    public static Long getUserId() {
        return userHolder.get();
    }

    public static void clear() {
        userHolder.remove(); // 防止内存泄漏，必须清理
    }
}