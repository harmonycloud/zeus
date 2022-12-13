package com.middleware.zeus.integration.cluster.bean;

import java.util.List;
import java.util.Map;

import com.alibaba.fastjson.annotation.JSONField;
import io.fabric8.kubernetes.api.model.Affinity;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.apps.StatefulSetUpdateStrategy;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author dengyulong
 * @date 2021/04/02
 */
@Accessors(chain = true)
@Data
public class MysqlClusterSpec {

    /**
     * 是否被动切换
     */
    private Boolean passiveSwitched;
    private List<Object> businessDeploy;
    private ClusterSwitch clusterSwitch;
    private String cmName;
    private DeployStrategy deployStrategy;
    private Map<String, Object> migratePolicy;
    private Map<String, Object> _statefulset;
    private Integer replicas;
    private String repository;
    private String secretName;
    private StatefulSet statefulset;
    private Map<String, Object> storageProvider;
    private String type;
    private String syncMode;
    private String version;
    private List<Map<String, Object>> volumeClaimTemplates;

    @JSONField(name = "_statefulset")
    public Map<String, Object> get_statefulset(){
        return _statefulset;
    }

    @JSONField(name = "_statefulset")
    public void set_statefulset(Map<String, Object> _statefulset){
        this._statefulset = _statefulset;
    }

    @Accessors(chain = true)
    @Data
    public static class ClusterSwitch {
        private Boolean finished;
        private Boolean switched;
        private String master;
    }

    @Accessors(chain = true)
    @Data
    public static class DeployStrategy {
        private Map<String, Object> basicConfig;
        private Map<String, Object> migration;
        private String type;
    }

    @Accessors(chain = true)
    @Data
    public static class StatefulSet {
        private Affinity affinity;
        private Map<String, String> annotations;
        private Map<String, String> labels;
        private String backupRestoreImage;
        private String configmap;
        private List<EnvVar> env;
        private String imagePullPolicy;
        private String initImage;
        private String middlewareImage;
        private String monitorImage;
        private MiddlewareResources resources;
        private Integer serverPort;
        private StatefulSetUpdateStrategy updateStrategy;
    }


}
