package com.middleware.zeus.common.base;

/**
 * @author dengyulong
 * @date 2021/01/21
 * 当前语言，需要初始化
 */
public class CurrentLanguage {

    private static final ThreadLocal<String> CURRENT_LANGUAGE = new ThreadLocal<>();

    public static void setLanguage(String language) {
        CURRENT_LANGUAGE.set(language);
    }

    public static String getLanguage() {
        return CURRENT_LANGUAGE.get();
    }

    public static void clear() {
        CURRENT_LANGUAGE.remove();
    }

}
