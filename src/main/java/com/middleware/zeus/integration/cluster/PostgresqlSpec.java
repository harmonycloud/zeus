package com.middleware.zeus.integration.cluster;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;
import java.util.Map;

/**
 * @author liyinlong
 * @since 2023/3/17 5:17 下午
 */
@Accessors(chain = true)
@Data
public class PostgresqlSpec {

    private Object additionalVolumes;

    private Object affinity;

    private Object allowedSourceRanges;

    private Object clone;

    private Object connectionPooler;

    private Object customEnvs;

    private Object customVolumes;

    private Object databases;

    private String dockerImage;

    private Boolean enableConnectionPooler;

    private Boolean enableHostNetwork;

    private Boolean enableLogicalBackup;

    private Boolean enableReplicaConnectionPooler;

    private Boolean enableReplicaLoadBalancer;

    private Boolean enableShmVolume;

    private String exporterImage;

    private String  imagePullPolicy;

    private Object initContainers;

    private Object init_containers;

    private String logicalBackupSchedule;

    private Object maintenanceWindows;

    private Object monitor;

    private Object nodeAffinity;

    private Integer numberOfInstances;

    private Object patroni;

    private Object podAnnotations;

    private String podPriorityClassName;

    private String pod_priority_class_name;

    private Object postgresql;

    private Object preparedDatabases;

    private Boolean replicaLoadBalancer;

    private Object resources;

    private String schedulerName;

    private Object serviceAnnotations;

    private Object sidecars;

    private Integer spiloFSGroup;

    private Integer spiloRunAsGroup;

    private Integer spiloRunAsUser;

    private Object standby;

    private String teamId;

    private Object tls;

    private Object tolerations;

    private Object topologySpreadConstraints;

    private Boolean useLoadBalancer;

    private Object userPasswords;

    private Object users;

    private Object volume;

}
