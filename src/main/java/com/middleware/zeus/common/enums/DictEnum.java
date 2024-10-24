

package com.middleware.zeus.common.enums;


import com.middleware.zeus.util.SpringContextUtils;
import com.skyview.language.context.LanguageContext;
import com.skyview.language.service.LanguageErrorEnumUtilService;

public enum DictEnum {

    NAME("name", "名称"),
    PORT("port", "端口"),
    PARAM("param", "参数"),
    CREATE_TIME("create time", "创建时间"),
    USERNAME("username", "用户名"),
    HARBOR_USER("harbor user","harbor用户"),
    HARBOR_HOST("harbor server address","harbor服务器地址"),
    HARBOR_TARGET("target harbor server","目标harbor服务器"),
    REAL_NAME("real name", "真实姓名"),
    PHONE("phone no.", "手机号"),
    PASSWORD("password","密码"),
    PASSWORD_NEW("new password","新密码"),
    SERVER_HOST("server host", "服务器地址"),
    USER_ID("user id", "用户id"),
    ROLE("role", "角色"),
    NAMESPACE("namespace", "分区"),
    NAMESPACED("namespaced", "分区资源标识"),
    NAMESPACE_NAME("namespace name", "分区名称"),
    NAMESPACE_ENGLISH("namespace english", "分区英文"),
    NAMESPACE_AUTOSCALE("namespace autoscale", "分区扩缩容"),
    CLUSTER("cluster", "集群"),
    CLUSTER_ID("clusterId", "集群id"),
	EDGE_NODE_NAME("EdgeNodeName","边缘节点组"),
    POD("pod", "pod"),
    NODE("node", "节点"),
    NODE_TYPE("node type", "节点类型"),
    TENANT("tenant", "租户"),
    TENANT_NAME("tenant name", "租户名称"),
    TENANT_ID("tenantId", "租户编号"),
    TENANT_MANAGER("tenant manager", "租户管理员"),
    PROJECT("project", "项目"),
    PROJECT_ID("project id", "项目编号"),
    ORGAN_ID("organization id", "组织编号"),
    PROJECT_NAME("project name", "项目名称"),
    APPLICATION("application", "应用"),
    APPLICATION_ID("applicationId", "应用编号"),
    APPLICATION_TEMPLATE("application template", "应用模板"),
    SERVICE("service","服务"),
    SERVICE_NAME("service name","服务名称"),
    RELATED_SERVICE("related service","关联服务"),
    JOB("job", "任务"),
    LABEL("label", "标签"),
    IMAGE("image", "镜像"),
    USER_GROUP("user group", "用户群组"),
    REPOSITORY("repository", "镜像仓库"),
    REPOSITORY_ID("harborRepositoryId", "镜像仓库id"),
    REPOSITORY_QUOTA("repository quota", "镜像仓库配额"),
    REPLICATION_POLICY("replication policy", "镜像同步规则"),
    REPLICATION_POLICY_ID("replication policy id", "镜像同步规则id"),
    REPLICATION_TARGET_ID("replication target id", "镜像同步对方服务器"),
    IMAGE_NAME("image name", "镜像名称"),
    IMAGE_TAG("image tag", "镜像版本"),
    PAGE_SIZE("page size", "分页页码"),
    QUERY("query condition", "查询条件"),
    FLAG_TYPE("flag type", "标识类型"),
    NETWORK("network","网络"),
    NETWORK_ID("network id","网络id"),
    NETWORK_NAME("network name","网络名称"),
    SUB_NETWORK_ID("sub network id","子网id"),
    SUB_NETWORK_NAME("sub network name","子网名称"),
    FILE("file","文件"),
    LINE("line","行号"),
    EMAIL("email","电子邮箱"),
    SERVICE_OUT("out access service", "对外服务"),
    DEPLOYMENT_NAME("deployment name", "部署名称"),
    AUTO_SCALE("auto scale", "自动伸缩"),
    LOCAL_ROLE_ID("local role id", "局部角色编号"),
    LOG_INDEX("log index", "日志索引"),
    CONFIG_MAP("configmap","配置文件"),
    CONFIG_MAP_NAME("configmap name","配置文件名称"),
    STORAGE_CLASS("storage class", "存储"),
    CONTAINER("container","容器"),
    LOG_DIR("logDir","日志目录"),
    LOG_FILE("logFile","目录文件"),
    STROAGE("stroage","存储"),
    PV("PersistentVolume", "存储卷"),
    PVC("PersistentVolumeClaim", "存储卷索取"),
    CONFIG_MAP_ID("configMapId","配置组id"),
    INGRESS_CONTROLLER("ingress controller","负载均衡器"),
    GLOBAL_INGRESS_CONTROLLER("Global Load Balancer","全局负载均衡"),
    NODE_MASTER("MASTER","主控"),
    NODE_SYSTEM("SYSTEM","系统"),
    NODE_BUILD("BUILDING","构建"),
    NODE_LB("SLB","负载均衡"),
    NODE_PRIVATE("PRIVATE","独占"),
    NODE_IDLE("IDLE","闲置"),
    NODE_PUBLIC("PUBLIC","共享"),
    NODE_ISTIO("ISTIO","微服务"),
    RULE_ID("IstioRuleId", "策略id"),
	NETWORK_POLICY("Network Policy", "网络隔离"),
	SECRET("Secret", "密钥"),
    YAML_CONTENT("Yaml Content", "yaml内容"),
    YAML_TEMPLATE_CONTENT("Yaml Template Content", "yaml模板内容"),
    YAML_TEMPLATE("Yaml Template", "yaml模板"),
    IS_PUBLIC("Public/Private tag", "公有私有标识"),
    YAML_TEMPLATE_ID("Yaml Template Id", "yaml模板编号"),
    UNIT("Unit", "单位"),
    RESOURCE_TYPE("Resource Type", "资源类型"),
    RESOURCE_NAME("Resource Name", "资源名称"),
    SERVICE_TYPE("Service Type","服务类型"),
    DOMAIN("domain","域名"),
    PATH("path","路径"),
    PASSTHROUGH("Passthrough","上游证书"),
    CERTIFICATE("Certificate","证书"),
    INGRESS_GROUP_RULE_TYPE("Ingress Group Rule Type","分组分流规则类型"),
    INGRESS_GROUP_RULE("Ingress Group Rule","分组分流规则"),
    EXPOSE_PORT("exposed port","暴露端口"),
    CONTAINER_PORT("container port","容器端口"),
    PROTOCOL("protocol","服务协议类型"),
    RESOURCE("Resource", "资源"),
    DEPLOYMENT("Deployment", "部署"),
    STATEFULSET("StatefulSet", "有状态部署"),
    DAEMONSET("DaemonSet", "守护进程"),
    NODE_PORT("NodePort", "NodePort"),
    FS_GROUP("fsGroup", "FS组策略"),
    FS_GROUP_RULE("fsGroup rule", "FS组策略规则"),
    RUN_AS_USER("runAsUser", "用户运行策略"),
    RUN_AS_USER_RULE("runAsUser rule", "用户运行策略规则"),
    SUPPLEMENTALGROUPS("supplementalGroups", "补充组策略"),
    SUPPLEMENTALGROUPS_RULE("supplementalGroups rule", "补充组策略规则"),
    SELINUX("seLinux", "seLinux"),
    SELINUX_RULE("seLinux rule", "seLinux规则"),
    POD_SECURITY_POLICY_TEMPLATE("pod安全策略模板","podSecurityPolicyTemplate"),
    POD_SECURITY_POLICY_TEMPLATE_NAME("pod安全策略模板名称","podSecurityPolicyTemplateName"),
    HOST("host","主机"),
    GPU("GPU","GPU"),
    CHART("Chart","Chart文件"),
    CHART_NAME("Chart Name","Chart名称"),
    VERSION("Version","版本"),
    HARBOR_PROJECT_NAME("Harbor Project Name","Harbor项目名"),
    BACKUP_TASK("Backup task","备份任务"),
    RESTORE_TASK("Restore task","恢复任务"),
    BACKUP_STORAGE_LOCATION("backup storage location","备份存储"),
    BACKUP_SCHEDULE_CRON("backup schedule cron", "备份周期"),
    BACKUP_RETENTION_TIME("backup retention time", "备份保留天数"),
    CLUSTER_ROLE("Cluster Role","ClusterRole"),
    CLUSTER_ROLE_RULES("Cluster Role Rules","ClusterRole规则"),
    NODE_POOL("Node pool", "主机资源池"),
    APIGROUP("apiGroup", "api组"),
    APPSORE_TAG("app tag", "应用版本"),
    APPSORE_TYPE("app type", "应用类型"),
    APPSORE_COVER("app cover", "应用封面"),
    MODE("mode", "创建方式"),
    DESCRIPTION("description", "描述"),
    TAINT("taint", "污点"),
    FILE_INDEX("file index", "文件序列"),
    FILE_NAME("file name", "文件名"),
    SVN("svn", "svn"),
    GIT("git", "git"),
    ACCESS_KEY_ID("access key id", "访问密钥ID"),
    ACCESS_KEY_SECRET("access key secret", "访问密钥"),
    KOK_CLUSTER("KOK Cluster", "KOK集群"),

    MIDDLEWARE("middleware ", "中间件"),
    MYSQL_CLUSTER("mysql cluster ", "mysql集群"),
    REDIS_CLUSTER("redis cluster ", "redis集群"),
    ES_CLUSTER("ElasticSearch cluster ", "ElasticSearch集群"),
    MQ_CLUSTER("mq cluster ", "mysql集群"),
    INGRESS("ingress ", "对外路由"),
    API_SERVER("API Server ", "API Server "),
    REGISTRY("Registry ", "制品服务"),
    ELASTIC_SEARCH("ElasticSearch ", "ElasticSearch "),
    ES_COMPONENT("ES Component ", "ES组件"),

    VG_NAME("vg name", "vg名称"),
    SIZE("storage limit", "存储限额"),
    ;


    private final String enPhrase;
    private final String chPhrase;

    DictEnum(String enPhrase, String chPhrase) {
        this.enPhrase = enPhrase;
        this.chPhrase = chPhrase;
    }

    public String phrase() {
        String language = LanguageContext.getLanguage();
        if (language.equals("en-US")) {
            return enPhrase;
        } else {
            return chPhrase;
        }
    }

    public String getEnPhrase() {
        return enPhrase;
    }

    public String getChPhrase() {
        return chPhrase;
    }

    public static String phrase(DictEnum dictEnum){
        return dictEnum.getChPhrase();
    }


}
