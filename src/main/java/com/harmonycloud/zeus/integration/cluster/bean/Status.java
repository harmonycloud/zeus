package com.harmonycloud.zeus.integration.cluster.bean;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * @author dengyulong
 * @date 2021/04/02
 */
@Accessors(chain = true)
@Data
public class Status {

    private List<Condition> conditions;
    private Integer replicas;
    private String phase;

    @Accessors(chain = true)
    @Data
    public static class Condition {
        private String lastTransitionTime;
        private String mode;
        private String name;
        private String nodeName;
        @JSONField(name = "podIP")
        private String podIp;
        private boolean status;
        private String type;
    }

}
