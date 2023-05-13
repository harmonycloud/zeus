package com.middleware.zeus.common.model.middleware;

import lombok.Data;
import java.io.Serializable;

/**
 * @author liyinlong
 * @since 2021/9/23 11:12 上午
 */
@Data
public class ClusterQuotaDTO implements Serializable {

    private static final long serialVersionUID = -679566785436262824L;
    /**
     * 集群数量
     */
    private int clusterNum;

    /**
     * 分区数量
     */
    private int namespaceNum;

    /**
     * cpu总量
     */
    private double totalCpu;

    /**
     * cpu使用量
     */
    private double usedCpu;

    /**
     * 内存总量
     */
    private double totalMemory;

    /**
     * 内存使用量
     */
    private double usedMemory;

    /**
     * cpu使用量百分比
     */
    private String cpuUsedPercent;

    /**
     * 内存使用量百分比
     */
    private String memoryUsedPercent;
}
