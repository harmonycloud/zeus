package com.middleware.zeus.common.base;

/**
 * @author dengyulong
 * @date 2021/01/21
 * 当前语言，需要初始化
 */
public class CurrentLanguage {

    private static final ThreadLocal<String> currentLanguage = new ThreadLocal<>();

    public static void setLanguage(String language) {
        currentLanguage.set(language);
    }

    public static String getLanguage() {
        return currentLanguage.get();
    }

    public static void clear() {
        currentLanguage.remove();
    }

}
