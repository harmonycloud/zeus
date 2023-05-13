package com.middleware.zeus.util.collection;

/**
 * @author dengyulong
 * @date 2020/12/03
 */
public class JsonUtils {

    /**
     * 是否json对象
     *
     * @param jsonString json字符串
     * @return
     */
    public static boolean isJsonObject(String jsonString) {
        if (jsonString == null) {
            return false;
        }
        return jsonString.startsWith("{") && jsonString.endsWith("}");
    }

    /**
     * 是否json数组
     *
     * @param jsonString json字符串
     * @return
     */
    public static boolean isJsonArray(String jsonString) {
        if (jsonString == null) {
            return false;
        }
        return jsonString.startsWith("[") && jsonString.endsWith("]");
    }

}
