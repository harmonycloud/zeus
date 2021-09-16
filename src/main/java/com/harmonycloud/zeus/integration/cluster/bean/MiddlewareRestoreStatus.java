package com.harmonycloud.zeus.integration.cluster.bean;

import lombok.Data;

import java.util.List;

/**
 * @author liyinlong
 * @since 2021/9/15 5:09 下午
 */
@Data
public class MiddlewareRestoreStatus {

    private String creationTimestamp;

    private String phase;

    private String reason;

    private List<Record> records;

    @Data
    public static class Record{
        private String creationTimestamp;

        private String uuid;

        private List<Detail> details;

        @Data
        public static class Detail{
            private String name;

            private String pod;

            private String pvc;

            private String volumeSnapshot;

            private String phase;
        }


    }
}
