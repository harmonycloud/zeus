package com.middleware.zeus.integration.cluster.bean;

import com.alibaba.fastjson.annotation.JSONField;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * @author dengyulong
 * @date 2021/04/02
 */
@Accessors(chain = true)
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Status {

    private List<Condition> conditions;
    private Integer replicas;
    private String phase;
    private String lastChangeMaster;

    @Accessors(chain = true)
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Condition {
        private String lastTransitionTime;
        private String mode;
        private String name;
        private String nodeName;
        private String podIP;
        private String status;
        private String type;
    }

}
