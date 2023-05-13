package com.middleware.zeus.common.model;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author anson
 * @since 2021/1/11 14:11
 */
@Accessors(chain = true)
@Data
public class ClusterComponentPod {
    private String ip;
    private String name;
    private String namespace;
    private String startTime;
    private String status;
    private String nodeName;
    private String ownerReferenceKind;
    private Double cpuRequest;
    private Double memoryRequest;
    private Integer gpuRequest;
    private Double cpuLimit;
    private Double memoryLimit;
}
