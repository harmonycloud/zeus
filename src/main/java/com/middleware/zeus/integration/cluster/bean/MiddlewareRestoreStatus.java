package com.middleware.zeus.integration.cluster.bean;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @author liyinlong
 * @since 2021/9/15 5:09 下午
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class MiddlewareRestoreStatus {

    private String creationTimestamp;

    private String phase;

    private String reason;

    private List<Record> records;

    @AllArgsConstructor
    @NoArgsConstructor
    @Data
    public static class Record{
        private String creationTimestamp;

        private String uuid;

        private List<Detail> details;

        @AllArgsConstructor
        @NoArgsConstructor
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
