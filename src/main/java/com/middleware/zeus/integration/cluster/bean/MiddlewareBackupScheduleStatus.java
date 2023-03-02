package com.middleware.zeus.integration.cluster.bean;

import com.alibaba.fastjson.JSONObject;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * 中间件备份状态
 */
@Data
@Accessors(chain = true)
public class MiddlewareBackupScheduleStatus {

    private String creationTimestamp;

    private List<Record> records;

    private String phase;

    private String reason;

    private JSONObject storageProvider;

    /**
     * 备份记录
     */
    @Data
    public static class Record {
        private String creationTimestamp;

        private String uuid;

        private Detail detail;

        public static class Detail {
            private String name;

            private String pod;

            private String pvc;

            private String volumeSnapshot;

            private String phase;
        }

    }
}
