package com.middleware.zeus.integration.cluster.bean;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * mysql灾备实例复制状态
 * @author liyinlong
 * @date 2021/8/11 2:43 下午
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@Accessors(chain = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class MysqlReplicateStatus {
    /**
     * 状态可以分为每个实例的状态和整体的状态
     * 分别包括Syncing(同步中)\stopSyncing(停止同步)\Error(错误). 每个实例/整体都可以为以上3种状态；
     * 当至少一个实例是err，整体的状态就是error
     */
    private String phase;
    /**
     * 故障原因
     */
    private String reason;

    private List<PodStatus> slaves;

    @AllArgsConstructor
    @NoArgsConstructor
    @Data
    public static class PodStatus{

        private String createTime;

        private String lastSuccessTime;

        private String lastUpdateTime;

        private String phase;

        private String podName;
    }
}
