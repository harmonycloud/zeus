package com.middleware.zeus.integration.cluster;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author liyinlong
 * @since 2023/3/19 2:05 下午
 */
@Accessors(chain = true)
@Data
public class RedisClusterSpec {

    private Object deployment;

    private Object env;

    private Object hostVolumes;

    private Object pod;

    private Object podManagementPolicy;

    private Object predixy;

    private Integer replicas;

    private String repository;

    private Object sentinel;

    private Integer servicePort;

    private Object statefulset;

    private String type;

    private Object updateStrategy;

    private String version;

    private Object volumeClaimTemplates;

}
