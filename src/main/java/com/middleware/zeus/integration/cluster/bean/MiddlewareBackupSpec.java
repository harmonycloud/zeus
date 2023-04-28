package com.middleware.zeus.integration.cluster.bean;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Map;

/**
 * 中间件备份记录spec
 * @author  liyinlong
 * @since 2021/9/15 10:06 上午
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@Accessors(chain = true)
@JsonIgnoreProperties(ignoreUnknown = true)
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

            private String bucketSubPath;

            private String userId;

            private String userKey;

            private String backupPassword;

        }
    }

}

