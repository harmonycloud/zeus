package com.middleware.zeus.integration.cluster.bean;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;
import java.util.Map;

/**
 * @author liyinlong
 * @since 2021/9/15 5:07 下午
 */
@Data
@Accessors(chain = true)
public class MiddlewareRestoreSpec {

    /**
     * 地址信息
     */
    private MiddlewareBackupDestination backupDestination;

    /**
     * 备份记录名称
     */
    private String name;

    /**
     * 中间件类型
     */
    private String type;

    /**
     * 通用备份
     */
    private List<Map<String, Object>> customRestores;

    public MiddlewareRestoreSpec() {
    }

    public MiddlewareRestoreSpec(MiddlewareBackupDestination backupDestination, String name, String type, List<Map<String, Object>> customBackups) {
        this.backupDestination = backupDestination;
        this.name = name;
        this.type = type;
        this.customRestores = customRestores;
    }

    @Data
    public static class MiddlewareBackupDestination {

        private String destinationType;

        private MiddlewareBackupParameters parameters;

        public MiddlewareBackupDestination() {

        }

        @Data
        public static class MiddlewareBackupParameters {

            private String bucket;

            private String url;

            private String subPath;

            private String userId;

            private String userKey;

            private String backupPassword;

            public MiddlewareBackupParameters(String bucket, String url, String subPath, String userId, String userKey, String backupPassword) {
                this.bucket = bucket;
                this.url = url;
                this.subPath = subPath;
                this.userId = userId;
                this.userKey = userKey;
                this.backupPassword = backupPassword;
            }

        }
    }

}
