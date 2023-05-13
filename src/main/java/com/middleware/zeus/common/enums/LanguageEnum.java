package com.middleware.zeus.common.enums;

import org.apache.commons.lang3.StringUtils;

/**
 * @author dengyulong
 * @date 2020/07/06
 * 语言枚举类
 */
public enum LanguageEnum {

    CH("Ch"),
    EN("En"),
    ;

    private String language;

    /**
     * 是否为中文
     */
    public static boolean isChinese(String language) {
        if (StringUtils.isEmpty(language)) {
            return true;
        }
        // 默认为中文
        return CH.getLanguage().equalsIgnoreCase(language) || "Zh".equalsIgnoreCase(language);
    }

    /**
     * 是否为英文
     */
    public static boolean isEnglish(String language) {
        if (StringUtils.isEmpty(language)) {
            return false;
        }
        // 默认为中文
        return EN.getLanguage().equalsIgnoreCase(language);
    }

    LanguageEnum(String language) {
        this.language = language;
    }

    public String getLanguage() {
        return language;
    }
}
