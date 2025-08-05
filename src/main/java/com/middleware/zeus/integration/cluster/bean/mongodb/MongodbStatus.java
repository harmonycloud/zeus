package com.middleware.zeus.integration.cluster.bean.mongodb;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author xutianhong
 * @Date 2025/8/1 13:08
 */
@Data
@Accessors(chain = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class MongodbStatus {

    private Backup backup;
    private String lastTransition;
    private String link;
    private int members;
    private int observedGeneration;
    private String phase;
    private String version;

    @Data
    @Accessors(chain = true)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Backup {
        private String statusName;
    }
}

