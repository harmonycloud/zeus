package com.harmonycloud.zeus.integration.cluster.bean;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;
import java.util.Map;


/**
 * @author tangtx
 * @date 2021/03/26 11:00 AM
 */
@Accessors(chain = true)
@Data
public class MiddlewareStatus {

    private String creationTimestamp;

    private Map<String, List<MiddlewareInfo>> include;
    
    private String phase;

    private MiddlewareResources resources;

    private Integer replicas;

    private String reason;
}
