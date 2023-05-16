package com.middleware.zeus.integration.cluster.bean;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.List;
import java.util.Map;

/**
 * @author liyinlong
 * @since 2021/9/15 5:07 下午
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@Accessors(chain = true)
@JsonIgnoreProperties(ignoreUnknown = true)
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

    @AllArgsConstructor
    @NoArgsConstructor
    @Data
    public static class MiddlewareBackupDestination {

        private String destinationType;

        private MiddlewareBackupParameters parameters;

        @AllArgsConstructor
        @NoArgsConstructor
        @Data
        public static class MiddlewareBackupParameters {

            private String bucket;

            private String url;

            private String subPath;

            private String userId;

            private String userKey;

            private String backupPassword;

            private String bucketSubPath;

        }
    }

}
