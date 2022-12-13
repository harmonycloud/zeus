package com.middleware.zeus.integration.cluster.bean;

import lombok.Data;
import lombok.experimental.Accessors;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Map;

/**
 * 中间件备份记录spec
 * @author  liyinlong
 * @since 2021/9/15 10:06 上午
 */
@Data
@Accessors(chain = true)
public class MiddlewareBackupSpec {

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
    private List<Map<String, Object>> customBackups;

    public MiddlewareBackupSpec() {
    }

    public MiddlewareBackupSpec(MiddlewareBackupDestination backupDestination, String name, String type, List<Map<String, Object>> customBackups) {
        this.backupDestination = backupDestination;
        this.name = name;
        this.type = type;
        this.customBackups = customBackups;
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

            private String bucketSubPath;

            private String userId;

            private String userKey;

            private String backupPassword;

            public MiddlewareBackupParameters(String bucket, String url, String bucketSubPath, String userId, String userKey, String backupPassword) {
                this.bucket = bucket;
                this.url = url;
                this.bucketSubPath = bucketSubPath;
                this.userId = userId;
                this.userKey = userKey;
                this.backupPassword = backupPassword;
            }

        }
    }

}

