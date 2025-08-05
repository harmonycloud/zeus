package com.middleware.zeus.integration.cluster.bean.mongodb;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author xutianhong
 * @Date 2025/8/1 11:08
 */

@Data
@Accessors(chain = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class OpsManagerStatus {

    private ApplicationDatabase applicationDatabase;
    private Backup backup;
    private OpsManager opsManager;

    @Data
    @Accessors(chain = true)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ApplicationDatabase {
        private String lastTransition;
        private int members;
        private int observedGeneration;
        private String phase;
        private String version;
    }

    @Data
    @Accessors(chain = true)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Backup {
        private String lastTransition;
        private int observedGeneration;
        private String phase;
        private String version;
    }

    @Data
    @Accessors(chain = true)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class OpsManager {
        private String lastTransition;
        private int observedGeneration;
        private String phase;
        private int replicas;
        private String url;
        private String version;
    }
}
