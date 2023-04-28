package com.middleware.zeus.integration.cluster.bean;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.List;
import java.util.Map;

/**
 * 中间件备份记录状态
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@Accessors(chain = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class MiddlewareBackupStatus {

    private String creationTimestamp;

    private List<Map<String, Object>> backupResults;

    private List<BackupInfo> backupInfos;

    private String phase;

    private String message;

    private StorageProvider storageProvider;

    private String reason;
    /**
     * 备份记录
     */
    @AllArgsConstructor
    @NoArgsConstructor
    @Data
    public static class BackupInfo {
        private Boolean readyToUse;

        private String repository;

        private String volumeSnapshot;

        private int orderNum;

    }

    @AllArgsConstructor
    @NoArgsConstructor
    @Data
    public static class StorageProvider{

        private Parameters parameters;

        @AllArgsConstructor
        @NoArgsConstructor
        @Data
        public static class Parameters{
            private String bucket;

            private String backupPassword;

            private String type;

            private String userId;

            private String userKey;

        }
    }
}
