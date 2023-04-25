package com.middleware.zeus.integration.cluster.bean;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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
@JsonIgnoreProperties(ignoreUnknown = true)
public class MiddlewareStatus {

    private String creationTimestamp;

    private Map<String, List<MiddlewareInfo>> include;
    
    private String phase;

    private MiddlewareResources resources;

    private Integer replicas;

    private String reason;
}
