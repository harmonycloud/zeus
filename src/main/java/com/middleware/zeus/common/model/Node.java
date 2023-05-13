package com.middleware.zeus.common.model;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * @author anson
 * @since 2021/1/5 13:44
 */
@Accessors(chain = true)
@Data
public class Node {
    private String ip;
    private String name;
    private String status;
    private String time;
    private String type;
    private NodeResource cpu;
    private NodeResource memory;
    private NodeResource disk;
    private NodeResource gpu;
    private Boolean scheduable;
    private String clusterId;
    private String clusterIp;
    private String netSegment;
    private String aliasName;
    private List<Taint> taints;
    private Map<String, String> labels;
    private String nodePool;
    private Date createTime;
    private String role;
}
