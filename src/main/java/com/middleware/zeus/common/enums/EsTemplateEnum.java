package com.middleware.zeus.common.enums;

/**
 * @author Administrator
 * @since 2020/1/17 14:59
 */
public enum EsTemplateEnum {
    //模板类型
    LOG_STASH("middlewarelogstash",
        "{\"settings\":{\"index\":{\"analysis\":{\"normalizer\":{\"keyword_lowercase\":{\"filter\":[\"lowercase\"],\"type\":\"custom\"}}}}},\"mappings\":{\"doc\":{\"properties\":{\"error_severity\":{\"type\":\"keyword\"},\"sql_state_code\":{\"type\":\"keyword\"},\"offset\":{\"type\":\"long\"},\"k8s_resource_name\":{\"type\":\"keyword\"},\"k8s_pod\":{\"type\":\"keyword\"},\"index\":{\"type\":\"keyword\"},\"source\":{\"type\":\"keyword\"},\"message\":{\"analyzer\":\"ik_max_word\",\"type\":\"text\",\"fields\":{\"keyword\":{\"normalizer\":\"keyword_lowercase\",\"ignore_above\":256,\"type\":\"keyword\"}}},\"k8s_container_name\":{\"type\":\"keyword\"},\"k8s_node_name\":{\"type\":\"keyword\"},\"k8s_pod_namespace\":{\"type\":\"keyword\"},\"@timestamp\":{\"type\":\"date\"},\"docker_container\":{\"type\":\"keyword\"},\"k8s_resource_type\":{\"type\":\"keyword\"}}}},\"index_patterns\":[\"middlewarelogstash-*\"],\"order\":2}"),
    STDOUT("middlewarestdout", "{     \"order\": 2,     \"index_patterns\": [         \"middlewarestdout-*\"     ],     \"settings\": {" +
            "         \"index\": {             \"analysis\": {                 \"normalizer\": {                    " +
            " \"keyword_lowercase\": {                         \"filter\": [                             \"lowercase\"           " +
            "              ],                         \"type\": \"custom\"                     }                 }             }  " +
            "       }     },     \"mappings\": {         \"doc\": {             \"properties\": {                 \"message\": { " +
            "                    \"type\": \"text\",                     \"analyzer\": \"ik_max_word\",                    " +
            " \"fields\": {                         \"keyword\": {                             \"type\": \"keyword\",           " +
            "                  \"ignore_above\": 256,                             \"normalizer\": \"keyword_lowercase\"          " +
            "               }                     }                 },                 \"offset\": {                    " +
            " \"type\": \"long\"                 },                 \"@timestamp\": {                     \"type\": \"date\"     " +
            "            },                 \"docker_container\": {                     \"type\": \"keyword\"                 }, " +
            "                \"k8s_pod\": {                     \"type\": \"keyword\"                 },                 " +
            "\"k8s_node_name\": {                     \"type\": \"keyword\"                 },                " +
            " \"k8s_pod_namespace\": {                     \"type\": \"keyword\"                 },                 \"source\": {" +
            "                     \"type\": \"keyword\"                 },                 \"index\": {                     " +
            "\"type\": \"keyword\"                 },                 \"k8s_container_name\": {                     " +
            "\"type\": \"keyword\"                 },                 \"k8s_resource_type\": {                    " +
            " \"type\": \"keyword\"                 },                 \"k8s_resource_name\": {                    " +
            " \"type\": \"keyword\"                 }             }         }     } } "),

    MYSQL_AUDIT_SQL("mysqlaudit", "{\n" +
            "\"order\":1,\n" +
            "\"index_patterns\":[\"mysqlaudit-*\"],\n" +
            "\"mappings\":{ \"doc\":  {\n" +
            "   \"properties\":{\n" +
            "     \"middleware_name\":{\n" +
            "        \"type\":\"keyword\",\n" +
            "        \"index\":true\n" +
            "     },\n" +
            "     \"k8s_pod_namespace\":{\n" +
            "        \"type\":\"keyword\",\n" +
            "        \"index\":true\n" +
            "     },\n" +
            "     \"query\":{\n" +
            "        \"type\":\"text\",\n" +
            "        \"index\":true,\n" +
            "        \"analyzer\":\"ik_max_word\"\n" +
            "     }\n" +
            "    }\n" +
            "}\n" +
            "},\n" +
            "\"settings\":{\n" +
            "   \"index\": {\n" +
            "      \"max_result_window\": \"30000000\"\n" +
            "    }\n" +
            "}\n" +
            "}"),

    MYSQL_SLOW_LOG("mysqlslowlog", "{ \"order\": 2, \"index_patterns\": [\"mysqlslowlog-*\"]," +
            " \"settings\":{\"index\":{\"max_result_window\":\"30000000\"}}, \"mappings\":{}}"),
    POSTGRESQL_AUDIT_SQL("postgresqlaudit", "{\n" +
            "\"order\":1,\n" +
            "\"index_patterns\":[\"postgresqlaudit-*\"],\n" +
            "\"mappings\":{ \"doc\":  {\n" +
            "   \"properties\":{\n" +
            "     \"middleware_name\":{\n" +
            "        \"type\":\"keyword\",\n" +
            "        \"index\":true\n" +
            "     },\n" +
            "     \"k8s_pod_namespace\":{\n" +
            "        \"type\":\"keyword\",\n" +
            "        \"index\":true\n" +
            "     },\n" +
            "     \"query\":{\n" +
            "        \"type\":\"text\",\n" +
            "        \"index\":true,\n" +
            "        \"analyzer\":\"ik_max_word\"\n" +
            "     }\n" +
            "    }\n" +
            "}\n" +
            "},\n" +
            "\"settings\":{\n" +
            "   \"index\": {\n" +
            "      \"max_result_window\": \"30000000\"\n" +
            "    }\n" +
            "}\n" +
            "}");



    private String name;
    private String code;


    EsTemplateEnum(String name, String code) {
        this.setCode(code);
        this.setName(name);
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
