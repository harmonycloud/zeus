package com.middleware.zeus.util;

import com.middleware.caas.common.enums.DictEnum;
import com.middleware.caas.common.enums.ErrorMessage;
import com.harmonycloud.caas.filters.enumm.LanguageEnum;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import java.util.Collection;
import java.util.Map;

/**
 * @author dengyulong
 * @date 2021/05/19
 */
public class AssertUtil {

    public static void notBlank(String value, DictEnum dictEnum) {
        if (StringUtils.isBlank(value)) {
            throwIllegalArgumentException(dictEnum);
        }
    }

    public static void notEmpty(Collection collection, DictEnum dictEnum) {
        if (CollectionUtils.isEmpty(collection)) {
            throwIllegalArgumentException(dictEnum);
        }
    }

    public static void notEmpty(Map map, DictEnum dictEnum) {
        if (CollectionUtils.isEmpty(map)) {
            throwIllegalArgumentException(dictEnum);
        }
    }

    public static void notEmpty(Object[] array, DictEnum dictEnum) {
        if (array == null || array.length == 0) {
            throwIllegalArgumentException(dictEnum);
        }
    }

    public static void notNull(Object obj) {
        if (obj == null) {
            throw new IllegalArgumentException(paramPromptMessage());
        }
    }

    public static void notNull(Object obj, DictEnum dictEnum) {
        if (obj == null) {
            throwIllegalArgumentException(dictEnum);
        }
    }

    public static void greaterZero(Integer num, DictEnum dictEnum) {
        if (num == null || num <= 0) {
            throwIllegalArgumentException(dictEnum);
        }
    }

    public static String blankPromptMessage() {
        if (LanguageEnum.isChinese()) {
            return ErrorMessage.NOT_BLANK.getZhMsg();
        } else {
            return ErrorMessage.NOT_BLANK.getEnMsg();
        }
    }

    public static String paramPromptMessage() {
        if (LanguageEnum.isChinese()) {
            return DictEnum.PARAM.getChPhrase() + ErrorMessage.NOT_BLANK.getZhMsg();
        } else {
            return DictEnum.PARAM.getChPhrase() + ErrorMessage.NOT_BLANK.getEnMsg();
        }
    }

    private static void throwIllegalArgumentException(DictEnum dictEnum) {
        throw new IllegalArgumentException(
            "[" + ErrorMessage.INVALID_PARAMETER.getCode() + "] " + dictEnum.phrase() + " " + blankPromptMessage());
    }

    public static void assertTrue(boolean flag, DictEnum dictEnum) {
        if (!flag) {
            throwIllegalArgumentException(dictEnum);
        }
    }

}
