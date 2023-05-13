package com.middleware.zeus.common.enums;

import java.util.HashMap;
import java.util.Map;

/**
 * @author xutianhong
 * @Date 2022/5/16 2:02 下午
 */
public enum ActiveAreaEnum {

    zoneA("zoneA", "可用区A"),
    zoneB("zoneB", "可用区B"),
    zoneC("zoneC", "仲裁区"),
    ;

    private final String name;
    private final String aliasName;

    private static final Map<String, String> map = new HashMap<>();

    static {
        for (ActiveAreaEnum a : ActiveAreaEnum.values()){
            map.put(a.name, a.aliasName);
        }
    }

    ActiveAreaEnum(String name, String aliasName){
        this.name = name;
        this.aliasName = aliasName;
    }

    public String getName() {
        return name;
    }

    public String getAliasName() {
        return aliasName;
    }

    public static String getByName(String name){
        return map.get(name);
    }
}
