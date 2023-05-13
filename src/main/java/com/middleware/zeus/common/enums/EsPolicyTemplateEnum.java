package com.middleware.zeus.common.enums;

/**
 * @author xutianhong
 * @Date 2023/5/13 1:30 下午
 */
public enum EsPolicyTemplateEnum {

    LOG_SAVE_TIME_POLICY("zeus-logSaveTime","{\n" +
            "    \"policy\": {\n" +
            "        \"phases\": {\n" +
            "            \"hot\": {\n" +
            "                \"min_age\": \"0ms\",\n" +
            "                \"actions\": {\n" +
            "                    \"set_priority\": {\n" +
            "                        \"priority\": 100\n" +
            "                    }\n" +
            "                }\n" +
            "            },\n" +
            "            \"delete\": {\n" +
            "                \"min_age\": \"%s\",\n" +
            "                \"actions\": {\n" +
            "                    \"delete\": {}\n" +
            "                }\n" +
            "            }\n" +
            "        }\n" +
            "    }\n" +
            "}"),
    INDEX_BIND_POLICY("index_bind_policy","{\n" +
            "  \"index\": {\n" +
            "    \"lifecycle\": {\n" +
            "      \"name\": \"%s\"\n" +
            "    }      \n" +
            "  }\n" +
            "}")
    ;
    private String name;
    private String template;


    EsPolicyTemplateEnum(String name, String template) {
        this.setTemplate(template);
        this.setName(name);
    }

    public String getTemplate() {
        return template;
    }

    public void setTemplate(String code) {
        this.template = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

}
