package com.middleware.zeus.common.enums;

/**
 * @author xutianhong
 * @Date 2022/10/21 4:56 下午
 */
public enum EncodingEnum {

    BIG5("BIG5"),
    EUC_CN("EUC_CN"),
    EUC_JP("EUC_JP"),
    EUC_JIS_2004("EUC_JIS_2004"),
    EUC_KR("EUC_KR"),
    EUC_TW("EUC_TW"),
    GB18030("GB18030"),
    GBK("GBK"),
    ISO_8859_5("ISO_8859_5"),
    ISO_8859_6("ISO_8859_6"),
    ISO_8859_7("ISO_8859_7"),
    ISO_8859_8("ISO_8859_8"),
    JOHAB("JOHAB"),
    KOI8R("KOI8R"),
    KOI8U("KOI8U"),
    LATIN1("LATIN1"),
    LATIN2("LATIN2"),
    LATIN3("LATIN3"),
    LATIN4("LATIN4"),
    LATIN5("LATIN5"),
    LATIN6("LATIN6"),
    LATIN7("LATIN7"),
    LATIN8("LATIN8"),
    LATIN9("LATIN9"),
    LATIN10("LATIN10"),
    MULE_INTERNAL("MULE_INTERNAL"),
    SJIS("SJIS"),
    SHIFT_JIS_2004("SHIFT_JIS_2004"),
    SQL_ASCII("SQL_ASCII"),
    UHC("UHC"),
    UTF8("UTF8"),
    WIN866("WIN866"),
    WIN874("WIN874"),
    WIN1250("WIN1250"),
    WIN1251("WIN1251"),
    WIN1252("WIN1252"),
    WIN1253("WIN1253"),
    WIN1254("WIN1254"),
    WIN1255("WIN1255"),
    WIN1256("WIN1256"),
    WIN1257("WIN1257"),
    WIN1258("WIN1258"),
    ;

    private String name;


    EncodingEnum(String name){
        this.name = name;
    }

    public String getName(){
        return name;
    }

}
