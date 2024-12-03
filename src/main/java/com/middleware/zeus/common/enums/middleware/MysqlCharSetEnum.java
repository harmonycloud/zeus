package com.middleware.zeus.common.enums.middleware;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public enum MysqlCharSetEnum {
    MYSQL_CHAR_SET_5_7(Version5_7()),
    MYSQL_CHAR_SET_8_0(Version8_0());
    private final Map<String, List<String>> charSetMap;

    public Map<String, List<String>> getCharSetMap() {
        return charSetMap;
    }

    MysqlCharSetEnum(Map<String, List<String>> charSetMap){
        this.charSetMap = charSetMap;
    }

    public static Map<String, List<String>> getByVersion(String version) {
        if(version.startsWith("5.7")) {
            return MYSQL_CHAR_SET_5_7.getCharSetMap();
        }else {
            return MYSQL_CHAR_SET_8_0.getCharSetMap();
        }
    }

    private static Map<String, List<String>> Version5_7() {
        Map<String, List<String>> mp = new HashMap<>();
        mp.put("utf8mb4",Arrays.asList("utf8mb4_general_ci","utf8mb4_bin","utf8mb4_unicode_520_ci","utf8mb4_unicode_ci"));
        return mp;
    }

    private static Map<String, List<String>> Version8_0() {
        Map<String, List<String>> mp = new HashMap<>();
        mp.put("utf8mb4",Arrays.asList("utf8mb4_0900_ai_ci","utf8mb4_0900_as_ci","utf8mb4_0900_as_cs","utf8mb4_0900_bin",
                "utf8mb4_bin","utf8mb4_general_ci","utf8mb4_unicode_520_ci","utf8mb4_unicode_ci"));
        return mp;
    }

}
