package com.middleware.zeus.common.constants.middleware;

import static com.middleware.zeus.common.constants.CommonConstant.SLASH;

/**
 * @author dengyulong
 * @date 2021/03/25
 */
public class MiddlewareConstant {

    public final static String CR_GROUP = "harmonycloud.cn";
    public final static String CR_API_VERSION = "harmonycloud.cn/v1";
    public final static String APPS = "apps";
    public final static String V1 = "v1";
    public final static String V1_ALPHA1 = "v1alpha1";
    public static final String OFFICIAL_TAG = "harmonycloud";
    public static final String API_VERSION = "apiVersion";
    public static final String KIND = "kind";
    public static final String METADATA = "metadata";
    public static final String NAME = "name";
    public static final String NAMESPACE = "namespace";
    public static final String LABELS = "labels";
    public static final String MIDDLEWARE_CLUSTER = "MiddlewareCluster";

    public static final String MIDDLEWARE_CLUSTER_GROUP = CR_GROUP;
    public static final String MIDDLEWARE_CLUSTER_VERSION = V1;
    public static final String MIDDLEWARE_CLUSTER_PLURAL = "middlewareclusters";
    public static final String MIDDLEWARE_PLURAL = "middlewares";
    public static final String MIDDLEWARE_KIND = "Middleware";

    public static final String MYSQL_CLUSTER_GROUP = "mysql.middleware.harmonycloud.cn";
    public static final String MYSQL_CLUSTER_VERSION = V1_ALPHA1;
    public static final String MYSQL_CLUSTER_PLURAL = "mysqlclusters";
    public static final String MYSQL_CLUSTER_API_VERSION =  MYSQL_CLUSTER_GROUP + SLASH + MYSQL_CLUSTER_VERSION;

    public static final String TRAEFIC_GROUP = "traefik.containo.us";
    public static final String TRAEFIC_PLURAL = "ingressroutetcps";
    public static final String TRAEFIC_KIND = "IngressRouteTCP";

    public final static String NAMESPACED = "Namespaced";
    public final static String KUBE_SYSTEM = "kube-system";

    public final static String MIDDLEWARE_EXPOSE_INGRESS = "Ingress";
    public final static String MIDDLEWARE_EXPOSE_NODEPORT = "NodePort";

    public static final String MYSQL_BACKUP = "mysqlbackups";
    public static final String MYSQL_BACKUP_SCHEDULE = "mysqlbackupschedules";
    public static final String MIDDLEWARE_MYSQL_GROUP = "mysql.middleware.harmonycloud.cn";
    public static final String MIDDLEWARE_INCLUDE_VERSION = V1_ALPHA1;

    public static final String INGRESSES = "ingresses";
    public static final String SERVICES = "services";
    public static final String STATEFULSETS = "statefulsets";
    public static final String CONFIGMAPS = "configmaps";
    public static final String PODS = "pods";
    public static final String SECRET = "Secret";
    public static final String POD = "pod";
    public static final String PERSISTENT_VOLUME_CLAIMS = "persistentvolumeclaims";
    public static final String CONFIGMAP = "ConfigMap";

    public static final String MONITORING_CORS_COM = "monitoring.coreos.com";
    public static final String PROMETHEUS_RULE = "prometheusrules";

    public static final String MIDDLEWARE_CLUSTER_HARMONY_CLOUD_CN = "middlewareclusters.harmonycloud.cn";
    public static final String MIDDLEWARE_HARMONY_CLOUD_CN = "middlewares.harmonycloud.cn";

    public static final String MYSQLREPLICATES = "mysqlreplicates";
    public static final String MYSQL_REPLICAS_KIND = "MysqlReplicate";

    public static final String MIDDLEWAREBACKUPSCHEDULES = "middlewarebackupschedules";

    public static final String MIDDLEWAREBACKUP = "middlewarebackups";
    public static final String MIDDLEWARERESTORES = "middlewarerestores";

    public static final String MIDDLEWARE_OPERATOR = "middleware-operator";

    public static final String MYSQL_ARGS = "mysqlArgs";
    public static final String ARGS = "args";

    public static final String ASCEND = "ascend";
    public static final String DESCEND = "descend";

    public static final String EXPORTER = "exporter";
    public static final String HEADLESS = "headless";

    public static final String Slave_All = "Slave_All";
    public static final String SERVICE_TYPE = "middleware.harmonycloud.cn/service-type";

    public static final String ROOT = "root";

    public static final String EXTERNAL = "external";
    public static final String ENABLE = "enable";
    public static final String USE_NODE_PORT = "useNodePort";
    public static final String EXTERNAL_IP_ADDRESS = "externalIPAddress";
    public static final String SVC_NAME_TAG = "svcNameTag";

    public static final String MIDDLEWARE = "middleware";
    public static final String STORAGE_LIMIT = "storageLimit";

    public static final String PREDIXY = "predixy";

    public static final String NODE_AFFINITY = "nodeAffinity";

    public static final String STORAGE_PROVISIONER = "volume.beta.kubernetes.io/storage-provisioner";
    public static final String LVM_PROVISIONER = "localplugin.csi.alibabacloud.com";
    public static final String HITACHI_PROVISIONER = "hspc.csi.hitachi.com";

    public static final String ACTIVE_ACTIVE = "activeActive";
    public static final String STORAGE_ZONE = "topology.kubernetes.io/zone";

    public static final String MAINTENANCE_MIDDLEWARE_HC_CN = "maintenance.middleware.hc.cn";
    public static final String MAINTENANCES = "maintenances";

    public static final String MASTER = "master";
    public static final String SLAVE = "slave";
    public static final String SYNC_SLAVE = "sync_slave";

    public static final String NAMESERVER = "nameserver";

    public static final String K8S_POD_NAMESPACE = "k8s_pod_namesapce";
    public static final String YAML = "yaml";
    public static final String MIDDLEWARE_NAME = "middleware_name";
    public static final String KEYWORD = "keyword";
    public static final String ALERT_RULE = "alertrule";

}
