package com.middleware.zeus.common.model.middleware.mongodb;

import io.swagger.annotations.ApiModel;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * @author xutianhong
 * @Date 2025/8/1 13:32
 */
@ApiModel("Mongodb快照")
@Data
@Accessors(chain = true)
public class MongodbSnapshotsDo {
    private String clusterId;
    private boolean complete;
    private Created created;
    private boolean doNotDelete;
    private String expires;
    private String groupId;
    private String id;
    private LastOplogAppliedTimestamp lastOplogAppliedTimestamp;
    private List<Link> links;
    private NamespaceFilterList namespaceFilterList;
    private List<Part> parts;

    @Data
    public static class Created {
        private String date;
        private int increment;
    }

    @Data
    public static class LastOplogAppliedTimestamp {
        private String date;
        private int increment;
    }

    @Data
    public static class Link {
        private String href;
        private String rel;
    }

    @Data
    public static class NamespaceFilterList {
        private List<String> filterList;
        private String filterType;
    }

    @Data
    public static class Part {
        private String clusterId;
        private String compressionSetting;
        private long dataSizeBytes;
        private boolean encryptionEnabled;
        private long fileSizeBytes;
        private String mongodVersion;
        private String replicaSetName;
        private long storageSizeBytes;
        private String typeName;
    }
}

