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

    private Object customEnvs;

    private String dockerImage;

    private Boolean enableConnectionPooler;

    private Boolean enableHostNetwork;

    private Boolean enableLogicalBackup;

    private Boolean enableReplicaConnectionPooler;

    private String  imagePullPolicy;

    private String logicalBackupSchedule;

    private Object monitor;

    private Integer numberOfInstances;

    private Object patroni;

    private Object postgresql;

    private Object resources;

    private String teamId;

    private Object topologySpreadConstraints;

    private Object userPasswords;

    private Object volume;
}
