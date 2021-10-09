package com.harmonycloud.zeus.integration.cluster.bean;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * 中间件备份记录状态
 */
@Data
@Accessors(chain = true)
public class MiddlewareBackupStatus {

    private String creationTimestamp;

    private List<BackupInfo> backupInfos;

    private String phase;

    private String message;

    private StorageProvider storageProvider;
    /**
     * 备份记录
     */
    @Data
    public static class BackupInfo {
        private String repository;

        private String volumeSnapshot;

        public BackupInfo() {
        }
    }

    @Data
    public static class StorageProvider{

        private Minio minio;

        @Data
        public static class Minio{
            private String bucket;

            private String url;

            public Minio() {
            }
        }
    }
}
