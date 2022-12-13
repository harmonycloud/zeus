package com.middleware.zeus.integration.cluster.bean;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;
import java.util.Map;

/**
 * 中间件备份记录状态
 */
@Data
@Accessors(chain = true)
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
    @Data
    public static class BackupInfo {
        private Boolean readyToUse;

        private String repository;

        private String volumeSnapshot;

        private int orderNum;

        public BackupInfo() {
        }
    }

    @Data
    public static class StorageProvider{

        private Parameters parameters;

        @Data
        public static class Parameters{
            private String bucket;

            private String backupPassword;

            private String type;

            private String userId;

            private String userKey;

            public Parameters() {
            }
        }
    }
}
