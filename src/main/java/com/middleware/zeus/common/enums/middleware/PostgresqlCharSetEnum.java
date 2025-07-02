package com.middleware.zeus.common.enums.middleware;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public enum PostgresqlCharSetEnum {
    MYSQL_CHAR_SET(Version());
    private final Map<String, List<String>> charSetMap;

    public Map<String, List<String>> getCharSetMap() {
        return charSetMap;
    }

    PostgresqlCharSetEnum(Map<String, List<String>> charSetMap){
        this.charSetMap = charSetMap;
    }

    public static Map<String, List<String>> getByVersion(String version) {
        return MYSQL_CHAR_SET.getCharSetMap();
    }

    private static Map<String, List<String>> Version() {
        Map<String, List<String>> mp = new HashMap<>();
        mp.put("UTF8",Arrays.asList("zh_CN.UTF-8","en_US.UTF-8","zh_TW.UTF-8", "C.UTF-8"));
        return mp;
    }

}
