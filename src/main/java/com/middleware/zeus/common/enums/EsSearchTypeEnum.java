package com.middleware.zeus.common.enums;

import java.util.EnumSet;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public enum EsSearchTypeEnum {
    MATCH("match", "分词搜索"),
    MATCH_PHRASE("matchPhrase", "精确搜索"),
    WILDCARD("wildcard", "模糊搜索"),
    REGEXP("regexp", "正则表达式搜索");

    private String code;
    private String name;
    private static final Map<String, EsSearchTypeEnum> ES_SEARCH_TYPE_MAP = new ConcurrentHashMap(values().length);

    private EsSearchTypeEnum(String code, String name) {
        this.setCode(code);
        this.setName(name);
    }

    public static Map<String, EsSearchTypeEnum> getEsSearchTypeMap() {
        return ES_SEARCH_TYPE_MAP;
    }

    public static EsSearchTypeEnum getByCode(String code) {
        return code == null ? null : (EsSearchTypeEnum)ES_SEARCH_TYPE_MAP.get(code);
    }

    public String getCode() {
        return this.code;
    }

    private void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return this.name;
    }

    private void setName(String name) {
        this.name = name;
    }

    static {
        Iterator var0 = EnumSet.allOf(EsSearchTypeEnum.class).iterator();

        while(var0.hasNext()) {
            EsSearchTypeEnum type = (EsSearchTypeEnum)var0.next();
            ES_SEARCH_TYPE_MAP.put(type.getCode(), type);
        }

    }
}
