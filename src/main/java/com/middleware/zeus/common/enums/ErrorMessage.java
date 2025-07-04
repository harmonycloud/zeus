package com.middleware.zeus.common.enums;

import com.middleware.zeus.util.SpringContextUtils;
import com.skyview.language.service.LanguageErrorEnumUtilService;

public enum ErrorMessage {
    // common
    UNKNOWN(100001, "System error, please contact Administrator.", "系统错误，请联系系统管理员"),
    INVALID_PARAMETER(100002, "Invalid parameter.", "参数错误"),
    VALIDATE_FAILED(100003, "Validate failed.", "校验失败"),
    CREATE_TEMPORARY_FILE_ERROR(100004, "Fail to create the temporary file", "创建临时文件失败"),
    TARGET_FILE_NOT_FOUND(100005, "Target file not found", "目标文件不存在"),
    IO_FAILED(100006, "I/O failed", "I/O失败"),
    AUTH_FAILED(100007, "Auth failed", "认证失败"),
    YAML_FORMAT_WRONG(100008,"yaml format incorrect.","yaml格式不正确！"),
    PARSE_OBJECT_TO_CONFIGMAP_FAILED(100009,"failed to parse object to configmap","转换为configmap失败"),
    CERTIFICATE_AUTH_FAILED(100010, "Auth failed", "认证失败,您的集群证书信息可能有误"),
    PARAMETER_NOT_COMPLETE(100011, "Parameter is not complete", "参数不全"),
    QUERY_FAIL(100012, "Query failed.", "查询失败"),
    NOT_EXIST(100013, "does not exist", "不存在"),
    CREATE_FAIL(100014, "Create fail.", "创建失败"),
    UPDATE_FAIL(100015, "Update fail.", "更新失败"),
    NOT_BLANK(100016, "Can not be blank.", "不能为空"),
    CMD_RUN_FAILED(100017, "Cmd run failed", "命令运行失败"),
    SWITCH_FAILED(100018, "Switch fail.", "切换失败"),
    EXIST(100019, "Existing.", "已存在"),
    UPDATE_CONFIGMAP_FAILED(100020, "failed to update configmap", "更新配置文件失败"),
    NOT_EXIST_OR_NOT_RUNNING(100021, "not exist or not running", "不存在或运行异常"),
    LIST_BACKUP_FAILED(100022, "list backup failed", "获取备份列表失败"),
    CREATE_BACKUP_FAILED(100023, "create backuop failed", "创建备份失败"),
    DELETE_BACKUP_FILE_FAILED(100024,"delete backup failed", "删除备份文件失败"),
    BACKUP_FILE_NOT_EXIST(100025, "backup file not exist", "备份文件不存在"),
    DELETE_BACKUP_FAILED(100026, "delete backup failed", "删除备份失败"),
    TIME_PICK_ERROR(100027, "Time pick error, please try again", "时间选择错误，请重新选择"),
    MIDDLEWARE_BACKUP_UPDATE_FAILED(100028, "middleware backup update failed", "中间件备份任务更新失败"),
    INC_BACKUP_SCHEDULE_ERROR(100029, "increment middleware backup schedule error", "增量备份cr资源异常"),
    SYNC_SLAVE_NOT_FOUND(100030,"sync_slave not found", "同步节点未找到"),
    FIND_BACKUP_SCHEDULE_CRON_FAILED(100031,"find backup schedule cron failed","查找备份周期失败"),
    START_DATE_AFTER_END(100032, "Date error, start date is after end date.", "开始时间大于结束时间"),
    RESOURCE_METADATA_NOT_FOUND(100033, "resource metadata not found", "资源缺少metadata字段"),
    RESOURCE_KIND_NOT_FOUND(100034, "resource kind not found", "资源缺少kind字段"),
    RESOURCE_NAME_NOT_FOUND(100035, "resource name not found", "资源缺少name字段"),
    RESOURCE_NAMESPACE_NOT_FOUND(100036, "resource namespace not found", "资源缺少namespace字段"),
    NOT_LVM(100037, "not lvm", "所选存储类型不是lvm，无法更新"),
    FIND_PVC_FAILED(100038, "failed to find pvc", "未能找到pvc"),
    EMPTY_RESULT(100039, "result is empty", "结果为空"),
    DO_NOT_USE_CHINESE(100040, "do not use chinese", "请勿输入中文字符"),
    LOGIN_CAAS_API_FAILED(100041, "login caas api failed", "登录观云台失败"),
    LICENSE_CHECK_FAILED(100042, "license check failed", "license认证失败"),
    LICENSE_USED_IN_PLATFORM(100043, "license used in platform", "此license已与平台绑定"),
    LICENSE_CPU_RESOURCE_NOT_ENOUGH(100044, "license cpu resource not enough", "平台资源不足"),
    ACCESS_EXTERNAL_SERVICE_FAILED(100045, "Access to external service failed", "访问外部服务失败"),
    NO_AUTHORITY_WITH_EXTERNAL_SERVICE(100046, "no authority with external service", "无权限进行此操作"),
    BACKUP_SERVER_BOUND(100047, "backup server has bean bound", "备份服务器正在被使用"),
    SWITCH_TO_BACKUP_PLATFORM(100048, "switch to backup platform", "当前集群已由平台灾备功能进行切换，无法进行登录"),
    ILLEGAL_TOKEN(100049, "Illegal token", "token不合法"),
    TOKEN_TIMEOUT(100050, "Token has timed out", "token已超时"),
    TOKEN_NOT_YET_EFFECTIVE(100051, "Token not yet effective", "token未到生效时间"),
    DOES_NOT_EXIST(100052, "does not exist.", "不存在"),

    // internalService
    PARAMETER_VALUE_NOT_PROVIDE(101012, "Parameter cannot be null.", "参数不能为空"),
    SERVICE_DELETE_SOME_FAIL(101013, "Fail to delete some services.", "部分关联删除失败"),
    DELETE_FAIL(101014, "Delete failed.", "删除失败"),
    ADD_FAIL(101015, "add fail.", "添加失败"),
    GET_FAIL(101016, "get fail.", "查询失败"),
    NOT_FOUND(101017, "Not found.", "未找到"),

    // registry
    REGISTRY_NOT_FOUND(200001, "Registry not found.", "制品服务未找到"),
    REGISTRY_CONNECTION_FAILED(200002, "Cannot connect registry component.", "无法连接到制品服务器"),
    REGISTRY_PROJECT_AUTH_STRING_NULL(200003, "Auth string of project for registry cannot be null.", "项目与制品服务器的认证信息不能为空"),
    REGISTRY_AUTH_STRING_NULL(200004, "Auth string for registry cannot be null.", "制品服务器的认证信息不能为空"),
    REGISTRY_CALL_FAILED(200005, "call registry api failed.", "调用制品服务器接口失败"),
    REGISTRY_NAME_DUPLICATED(200006, "Registry name duplicated.", "制品服务名称已存在"),
    REGISTRY_NICKNAME_DUPLICATED(200007, "Registry nickname duplicated.", "制品服务的显示名称已存在"),
    REGISTRY_USER_CREATE_FAILED(200008, "Fail to create the Registry user.", "制品服务用户创建失败"),
    REGISTRY_REPO_NOT_FOUND(200009, "Repository not found.", "制品服务仓库未找到"),
    REGISTRY_REPO_RETENTION_RULE_POLICY_NOT_SUPPORT(200010, "Retention rule policy is not support", "平台不支持的镜像保留规则"),
    REGISTRY_REPO_RETENTION_TIMING_CANNOT_SET_WITH_EMPTY_RULE(200011, "Retention timing cannot be set when retention rule is empty", "定时任务无法在无规则的情况下配置"),
    REGISTRY_TARGET_UNHEALTHY(200012, "Target registry is unhealthy, please add after test connect successful.", "目标仓库不健康，请在测试连接通过后重试"),
    REGISTRY_USER_NOT_FOUND(200013, "Registry user not found", "制品服务用户不存在"),
    REGISTRY_URL_DUPLICATED(200014, "Registry url duplicated.", "制品服务url已存在"),
    REGISTRY_REPO_RETENTION_RULE_POLICY_PARAM_TOO_LONG(200015, "The number of retention rule param is gather 1, not support.", "平台不支持参数大于1的镜像保留规则"),
    REGISTRY_TYPE_NOT_SUPPORT_INTERFACE(200016, "Current registry type or version cannot support this function", "该类型或版本的制品服务不支持此功能"),
    SYSTEM_REPO_CANNOT_OPERATE(200017, "It's not allowed to operate system repository", "系统仓库不允许操作"),

    // registry project mapping
    REGISTRY_PROJECT_NOT_BIND(210001, "Registry is not bind to the project", "制品服务未绑定该项目"),
    REGISTRY_PROJECT_UNBIND_FAIL(210002, "Fail to unbind registry and project", "制品服务与项目解绑失败"),
    PROJECT_HAS_NO_PERMISSION_IN_REPO(210003, "Project or role has no permission in this repo", "该项目或角色不具有仓库的权限"),

    // helm chart
    REGISTRY_HELM_CHART_FILE_TRANSFER_ERROR(211001, "Fail to transfer the helm chart file", "传输chart包失败"),
    HELM_CHART_UNZIP_ERROR(211002, "Fail to unzip the helm chart file", "解压chart文件失败"),
    HELM_CHART_WRITE_ERROR(211003, "Fail to write the helm chart file", "写出chart文件失败"),
    CHART_YAML_READ_FAIL(211004, "Fail to read the chart yaml", "读取Chart.yaml文件失败"),
    HELM_CHART_FORMAT_ERROR(211005, "Helm chart format error", "chart文件格式错误"),
    HELM_INSTALL_PROMETHEUS_FAILED(211006, "helm install prometheus failed", "集群安装peometheus失败,请检查问题并重新添加"),
    HELM_INSTALL_LOCAL_PATH_FAILED(211007, "helm install local path failed", "集群安装local path失败,请检查问题并重新添加"),
    HELM_INSTALL_NGINX_INGRESS_FAILED(211008, "helm install nginx ingress failed", "集群安装nginx ingress失败,请检查问题并重新添加"),
    HELM_INSTALL_ALERT_MANAGER_FAILED(211009, "helm install alertmanager failed", "集群安装alertmanager失败"),
    HELM_INSTALL_MIDDLEWARE_CONTROLLER_FAILED(211010, "helm install middleware controller failed", "集群安装middleware controller失败, 请检查问题并重新添加"),
    HELM_INSTALL_MINIO_FAILED(211011, "helm install minio frailed", "集群安装minio失败"),
    HELM_INSTALL_LOG_FAILED(211012, "helm install log failed", "集群安装日志组件失败"),
    HELM_CHART_EXIST(211013, "helm chart has been existed", "该chart包已存在"),
    HELM_TEMPLATE_NOT_FOUND(211014,"helm template not founf","helm模版未找到"),
    UNSUPPORTED_YAML_TYPE(211015, "unsupported yaml of this type","暂不支持查看该类型Yaml"),

    // docker
    FAILED_CONNECT_DOCKERD(220001, "Cannot connect to dockerd", "无法连接到dockerd"),
    MULTI_IMAGE_UPLOAD_NOT_SUPPORT(220002, "Multi-image upload in one package not support", "暂不支持一个压缩包中包含多个镜像的上传"),
    DOWNLOAD_NEED_BEFORE_FETCH(220003, "Download image must before fetch", "下载镜像之前需要拉取镜像到本地"),

    // image
    IMAGE_NOT_FOUND(230001, "Image not found", "镜像不存在"),
    IMAGE_TAG_NOT_FOUND(230002, "Image tag not found", "镜像版本不存在"),
    IMAGE_TAG_ALREADY_EXISTS(230003, "Image tag already exists", "镜像版本已存在"),
    IMAGE_SYNC_DEST_EXIST(230004, "Sync image error. Same image name and tag exist in dest repository", "推送失败, 目标镜像已存在"),
    IMAGE_SYNC_POLICY_CREATE_FAIL(230005, "Sync image error. Fail to create the image replication policy", "推送失败, 复制策略创建失败"),

    // registry labels
    LABEL_ALREADY_EXIST(240001, "The same name label is already existed", "同名标签已存在"),

    // registry retention rules
    RETENTION_RULE_NOT_EXIST(250001, "The retention rule not exist", "尚未创建镜像清理规则"),

    // target registry ping
    TARGET_REGISTRY_INVALID(260001, "Target registry is invalid, please check metadata", "备份服务器信息有误，请检查信息填写是否有误或检查目标服务器状态"),
    TARGET_REGISTRY_NOT_FOUND(260002, "Target registry not found", "找不到备份服务器，请检查信息填写是否有误或检查目标服务器状态"),
    TARGET_REGISTRY_DUPLICATED(260003, "Target registry is duplicate, please check name and url", "备份服务器已存在，请检查名称和地址"),

    // cluster
    CLUSTER_NOT_FOUND(300001, "Cluster not found", "集群未找到"),
    CLUSTER_AUTH_STRING_NULL(300002, "Auth string for cluster cannot be null", "集群认证信息不能为空"),
    CLUSTER_AUTH_NOT_SUPPORT(300003, "Not support auth type", "不支持的集群认证方式"),
    CLUSTER_CONNECTION_FAILED(300004, "Cannot connect cluster", "无法连接到集群"),
    CLUSTER_CALL_FAILED(300005, "Call cluster api failed", "调用集群接口失败"),
    CLUSTER_RESOURCE_ALREADY_EXIST(300006, "Resource already exist in cluster", "资源已经存在与集群中"),
    CLUSTER_COMP_ADDRESS_NOT_FOUND(300007, "Cluster component address not found", "集群组件地址未找到"),
    CLUSTER_MONITOR_INFO_NOT_FOUND(300008, "Cluster monitor info not found", "集群监控信息未找到"),
    CLUSTER_COMPONENT_UNSUPPORTED(300009, "Cluster component is unsupported", "集群组件不支持"),
    CLUSTER_ES_SERVICE_ERROR(300010, "Cant not connect to es service.","集群es组件连接失败"),
    INGRESS_CONTROLLER_FIRST(300011, "Please make sure ingress has deployed and tcp configmap is filed in before deploy this component.","部署该组件之前，请确保Ingress已经部署，且填写了TCP配置"),
    NAMESPACE_NOT_BLANK(300012, "Namespace name can not be blank.", "命名空间名不能为空"),
    CLUSTER_NOT_REGISTERED(300013, "no clusters have been registered", "没有已注册集群"),
    CLUSTER_NOT_EMPTY(300014, "cluster is not empty", "集群存在实例，请先删除集群内所有实例"),
    NAMESPACE_REGISTRY_FAILED(300015, "failed to registry namespace", "注册分区失败"),
    NAMESPACE_NOT_FOUND(300016, "failed to find namespace", "分区未找到"),
    PROMETHEUS_NOT_INSTALLED(300017, "prometheus has not been installed or integrated", "prometheus 未安装或接入"),
    ALERT_MANAGER_NOT_INSTALLED(300018, "alertManager has not been installed or integrated", "alertManager 未安装或接入"),
    NAMESPACE_EXIST(300019, "namespace has existed", " 英文名称重复，请修改"),
    INGRESS_CLASS_EXISTED(300020, "ingress class has existed", "ingress class 已存在"),
    INGRESS_CLASS_NOT_EXISTED(300021, "ingress class not existed", "ingress class 不存在"),
    LVM_ALREADY_EXISTED(300022, "LVM already existed", "集群中已存在lvm或lvm相关资源"),
    LOCAL_PATH_ALREADY_EXIST(300023, "local-path already exist", "local-path已存在"),
    ELASTICSEARCH_CONNECT_FAILED(300024,"failed to connect to elasticsearch", "日志组件连接失败"),
    CAN_NOT_DELETE_NS_MIDDLEWARE_OPERATOR(300025, "delete namespace middleware-operator failed", "middleware-opeartor包含较多平台组件，不可被删除"),
    PVC_CAN_LESS_THAN_PREVIOUS(300026, "field can not be less than previous value", "pvc 修改值不可小于当前值"),
    NAMESPACE_NOT_EMPTY(300027, "namespace not empty", "分区下存在服务，请先清除分区下服务"),
    NAMESPACE_ALIAS_NAME_EXIST(300028, "alias name has existed", " 命名空间名称重复，请修改"),
    STORAGE_CLASS_NOT_FOUND(300029, "storage class not found", "存储服务未找到"),
    STORAGE_CLASS_NAME_EXIST(300030, "storage class name exist", "存储服务名称已存在"),
    STORAGE_CLASS_IS_BEING_USED(300031, "storage class is being used", "该存储正在被使用，无法删除"),
    INGRESS_CONFIGMAP_NOT_EXIST(300032, "ingress class has existed", "分区下的配置文件不存在"),
    CLUSTER_NOT_ADD_REPOSITORY(300033, "cluster not add repository", "集群未添加镜像仓库"),
    COMPONENTS_NOT_FOUND(300034, "components not found", "组件包未找到"),
    INGRESS_COMPONENTS_VALUES_NOT_FOUND(300035, "ingress components values not found", "负载均衡组件 values.yaml未找到"),
    CLUSTER_NOT_SET_DEFAULT_REPOSITORY(300036, "cluster not set default repository", "集群未设置默认镜像仓库"),
    STORAGE_NOT_ENOUGH(300037, "storage not enough", "存储不足"),
    UPDATE_MAXIMUM_LOG_RETENTION_TIME_FAILED(300038,"update the maximum log retention time failed","更新日志最大保留时间失败"),
    EMPTY_CLUSTER_ID(300039, "cluster id can't be empty", "集群id不能为空"),
    CONFIGMAP_NOT_EXIST(300040, "configmap not exist", "查询配置文件失败，未找到该配置文件"),
    LOG_SEARCH_TYPE_NOT_SUPPORT(300041, "search type not support.", "查询类型不支持"),
    // pod
    POD_NOT_EXIST(300042, "Pod not exist.", "POD不存在"),
    NS_POD_CONTAINER_NOT_BLANK(300044, "deploy pod and container can not be blank at same.", "服务名、pod名称和容器名称不能都为空"),
    POD_MIGRATE_FAILED(300045, "pod migrate failed", "POD迁移失败"),
    SWITCH_FAILD_BECAUSE_DELAY(300046,"sync_slave pod have data delay","sync同步节点存在数据延迟"),
    POD_MIGRATE_NOT_EXISTS(300047, "pod migrate info not exists", "pod迁移信息不存在"),
    THE_CONNECTION_ADDRESS_FOR_PROMETHEUS_IS_INCORRECT(300048, "The connection address for Prometheus is incorrect.", "获取prometheus连接地址错误"),

    // node 301xxx
    EDGE_NODE_PACKAGE_NOT_FOUND(301001, "Install package not found", "安装包未找到，请先生成"),
    NODE_NOT_FOUND(301002, "node not found", "节点未找到"),
    NODE_CONTAINS_MIDDLEWARE_PODS(301003, "node contains middleware pods", "节点上存在中间件pods， 不能移除"),

    GRAFANA_LOGIN_FAIL(302001, "Grafana login fail.", "Grafana登录失败"),

    //user
    USER_NOT_EXIT(400001, "user not exit.", "用户不存在"),
    USER_ROLE_EXIST(400002, "User has bound with role", "用户已与角色绑定"),
    USER_ROLE_NOT_EXIT(400003,"user has not bound with role,please contact administrator to bind role.", "该用户尚未关联角色，请联系管理员关联一个角色。"),
    ROLE_NOT_EXIST(400004, "role not exist", "角色不存在"),
    USERNAME_SHOULD_NOT_BE_NULL(400005, "username should not be null", "用户名不得为空"),
    USER_EXIST(400006, "username has been used", "账户已存在"),
    NO_AUTHORITY(400007, "no authority", "无权限"),
    WRONG_PASSWORD(400008, "wrong password", "密码错误"),
    PASSWORD_DO_NOT_MATCH(400009,"the new password do not match repeat new password", "新密码二次输入不匹配"),
    RSA_DECRYPT_FAILED(400010, "Decrypt password by private key failed", "rsa私钥解密失败"),
    CREATE_ROLE_FAILED(400011, "failed to create role", "创建角色失败"),
    ROLE_EXIST(400012, "role name has been used", "角色名已存在"),
    UPDATE_ROLE_FAILED(400013, "failed to update role", "更新角色失败"),
    ROLE_HAS_BEEN_BOUND(400014, "role has been bound", "该角色已与用户绑定"),
    ROLE_PERMISSION_IS_EMPTY(400015, "role permission is empty", "该用户绑定的角色权限为空"),
    ROLE_NAMESPACE_PERMISSION_EMPTY(400016, "role namespace perimission should not be empty", "角色分区权限不能为空"),
    LOGIN_FAILED(400017, "Login failed", "您的账户或密码有误，请重试"),
    PASSWORD_IS_EXPIRED(400018, "password is expired.", "密码已过有效期，请到容器平台修改密码后重新登录！"),
    DROP_USER_FAILED(400019, "drop user failed", "删除用户失败"),
    LDAP_SERVER_CONNECT_FAILED(400020 , "connect to ldap server failed", "LDAP连接失败"),
    LDAP_INCOMPLETE_PARAMETERS(400021, "Incomplete parameters", "LDAP参数不全"),
    // mail and ding
    SMTP_SERVER_CONNECT_FAILED(400022, "connect to smtp server failed", "smtp邮箱服务器连接失败"),
    MAIL_INCOMPLETE_PARAMETERS(400023, "Incomplete parameters", "邮箱参数不全"),
    MAIL_ADDRESS_INVALID(400024, "invalid mail address", "邮箱地址不合法"),
    DING_SERVER_CONNECT_FAILED(400025, "connect to ding server failed", "钉钉连接失败"),
    DING_INCOMPLETE_PARAMETERS(400026, "Incomplete parameters", "钉钉参数不全"),
    WEB_HOOK_REPETITION(400027,"webhook repetition","webhook重复"),
    LDAP_USER_NOT_EXIST(400028, "ldap user not exist", "ldap 用户不存在"),

    // mysql database manage
    MYSQL_INCOMPLETE_PARAMETERS(400050, "Incomplete parameters", "参数不全"),
    MYSQL_PASSWORD_NOT_MATCH(400051, "two password not match", "两次密码不一致"),
    MYSQL_UPDATE_PASSWORD_FAILED(400052, "mysql update password failed", "mysql更新密码失败"),
    MYSQL_CONNECTION_FAILED(400053, "mysql connection failed,please check service routine,Ensure that at least " +
            " one external service exposure is available", "连接mysql失败，请检查服务暴露，确保至少有一个对外服务暴露可用"),

    // project
    PROJECT_NOT_EXIST(400101, "Project not exist", "项目不存在"),
    PROJECT_NAMESPACE_ALREADY_BIND(400102, "This namespace is already bound to a project", "该分区已与项目绑定"),
    PROJECT_IS_NOT_EMPTY(400103, "project is not empty", "项目下不为空，无法删除"),
    PROJECT_NAME_EXIST(400104, "project name exist", "项目名称已存在"),
    NAMESPACE_IS_NOT_EMPTY(400105, "namespace is not empty", "分区存在中间件，无法取消接入"),
    PROJECT_STORAGE_USING(400106, "The storage has assigned quotas to namespaces; please cancel the assignment first", "该存储存在已分配配额于命名空间，请先取消分配"),
    PROJECT_CPU_MEMORY_USING(400107, "project cpu memory using", "该cpu memory存在已分配配额于命名空间，请先取消分配"),
    PROJECT_BACKUP_SERVER_USING(400108, "project backup server using", "该备份服务器已绑定备份位置，请先取消绑定"),
    PROJECT_ADD_USER_EMPTY_LIST(400109, "project add user list is empty", "添加失败，请选择新增的成员"),
    // organization
    ORGANIZATION_NOT_EXIST(400150, "organization not exist", "组织不存在"),
    ORGANIZATION_NAME_EXIST(400151, "organization name has been existed", "组织名称已存在"),
    ORGANIZATION_USER_USED_IN_PROJECT(400152, "organization user used in project", "组织用户存在项目角色"),
    ORGANIZATION_INCLUDE_PROJECT(400153, "organization include project", "组织下存在项目"),
    ORGANIZATION_STORAGE_USING(400154, "organization storage using", "该存储存在已分配配额于项目，请先取消分配"),
    ORGANIZATION_CPU_MEMORY_USING(400155, "organization cpu memory using", "该cpu memory存在已分配配额于项目，请先取消分配"),
    ORGANIZATION_BACKUP_SERVER_USING(400156, "organization backup server using", "该备份服务器存在已分配至项目，请先取消分配"),
    ORGANIZATION_ADD_USER_EMPTY_LIST(400157, "organization add user list is empty", "添加失败，请选择新增的成员"),
    // redis
    REDIS_INCOMPLETE_PARAMETERS(400201, "Incomplete parameters", "参数不全"),
    TEMPORARY_NOT_SUPPORT_CLUSTER(400202,"Temporary does not support cluster","暂不支持集群模式"),
    OUT_OF_RANGE(400203,"value is not an integer or out of range","超时时间长度设置过长"),
    KEY_ALREADY_EXISTS(400204,"The key already exists","键名已存在"),
    REDIS_SERVER_CONNECT_FAILED(400205,"redis server connection failed","redis链接失败"),
    NOT_SELECT_DATABASE(400206,"unselected database","未选择数据库"),
    NOT_AN_INTEGER_VALUE(400207,"not an integer value","超时时间请输入数字"),
    CANNOT_FIND_MASTER(400208,"find master node faild","查找主节点失败"),

    // loadbalancer
    K8S_NGINX_LB_CANNOT_FOUND_USEFUL_HTTPS_PORT(500001, "Cannot found useful port for nginx ingress controller https port", "暂无适合NGINX负载均衡使用的HTTPS端口"),
    K8S_NGINX_LB_CANNOT_FOUND_USEFUL_HEALTH_PORT(500002, "Cannot found useful port for nginx ingress controller health port", "暂无适合NGINX负载均衡使用的健康检查端口"),
    K8S_NGINX_LB_CANNOT_FOUND_USEFUL_STATUS_PORT(500003, "Cannot found useful port for nginx ingress controller status port", "暂无适合NGINX负载均衡使用的状态端口"),
    LB_NOT_EXIST(500004, "Loadbalancer not exist", "负载均衡不存在"),
    LB_DOMAIN_EXIST(500005, "Domain already exist", "域名已存在"),
    LB_INVALID_CA(500006, "Invalid CA file", "无效的证书"),

    // ingress// middleware
    INGRESS_NOT_EXIST(600001, "ingress not exist", "对外路由不存在"),
    INGRESS_ALREADY_EXIST(600002, "ingress name already exist", "对外路由名称已存在"),
    INGRESS_DOMAIN_DUPLICATED(600003, "Domain duplicate.", "包含该域名的ingress已存在"),
    INGRESS_TCP_PORT_EXIST(600005, "external routing TCP port already exists", "对外路由TCP端口已存在"),
    INGRESS_TCP_NOT_NULL(600006, "the external routing TCP list cannot be empty", "对外路由TCP列表不能为空"),
    INGRESS_TCP_PORT_NOT_NULL(600007, "the external routing TCP port cannot be empty", "对外路由TCP端口不能为空"),
    INGRESS_TCP_CONFIG_NOT_EXIST(600008, "the external routing TCP configuration does not exist", "对外路由TCP配置不存在"),
    UNSUPPORT_EXPOSE_TYPE(600009, "unsupported method of external exposure", "不支持的对外暴露方式"),
    INGRESS_NODEPORT_NOT_NULL(600010, "the external routing NodPort list cannot be empty", "对外路由NodPort列表不能为空"),
    INGRESS_NODEPORT_PORT_EXIST(600011, "external routing NodePort port already exists", "对外路由NodePort端口已存在"),
    INGRESS_TCP_OLD_PORT_NOT_NULL(600012, "the old external routing TCP port cannot be empty", "对外路由TCP旧端口不能为空"),
    INGRESS_DOMAIN_NAME_FORMAT_NOT_SUPPORT(600013,"ingress domain name format not supported","对外路由域名格式错误"),
    TCP_PORT_ALREADY_USED(600014, "external routing port already used", "该端口已被使用"),
    INGRESS_NOT_AVAILABLE(600015, "the ingress is not available", "负载均衡不可用"),
    PORT_IS_DEFINED_BY_TRAEFIK(600016, "the port is defined by traefik", "该端口已被traefik占用"),
    INGRESS_NODEPORT_PORT_NOT_NULL(600017, "Unknown NodPort for external routing", "对外路由NodPort端口未知"),

    //alert
    PROMETHEUS_RULES_NOT_EXIST(650001, "prometheus alert rule is not exist.", "查询prometheus rule规则文件失败"),
    UPDATE_RULES_FAILED(650002, "Failed to update prometheus alert rules.", "更新告警规则失败"),
    CREATE_RULES_FAILED(650003, "Failed to create prometheus alert rules.", "创建告警规则失败"),
    DELETE_RULES_FAILED(650004, "Failed to delete prometheus alert rules.", "删除告警规则失败"),
    LOG_ALERT_NAME_EXISTS(650005, "LOG_ALERT_NAME_EXISTS", "日志告警名称已存在"),

    //minio
    BUCKET_ALREADY_EXISTS(650101, "Bucket already exists", "bucket已存在"),
    CONNECTION_FAILED(650102, "connection failed", "连接失败"),
    CHINESE_NAME_REPETITION(650103, "chinese name repetition", "中文名称重复"),

    //monitor
    GRAFANA_ID_NOT_FOUND(660001, "Failed to find grafana id", "更新granafa id 失败"),
    GRAFANA_DASHBOARD_NOT_FOUND(660002, "grafana dashboard not found", "监控面板查询失败"),

    // middleware
    RESTART_POD_FAIL(700001, "The restart pod is not assign to the middleware cluster, please refresh browser and try again.", "重启pod不属于该中间件，请刷新重试"),
    MIDDLEWARE_CLUSTER_IS_NOT_RUNNING(700002, "Please make sure this middleware cluster is Running.", "请确保该中间件服务处于运行中"),
    MIDDLEWARE_CLUSTER_POD_ERROR(700003, "This middleware cluster pod role are error.", "该中间件集群pod角色错误"),
    MIDDLEWARE_NOT_EXIST(700004, "middleware not exist", "中间件不存在"),
    MIDDLEWARE_CLUSTER_STATUS_ABNORMAL(700005,"the midllware cluster is in the abnormal state", "中间件集群状态异常"),
    MIDDLEWARE_SUCCESS_INGRESS_FAIL(700006, "Middleware create success, but expose fail, please try again in the detail page", "中间件创建成功，对外访问创建失败，请稍后在详情页重试"),
    MIDDLEWARE_BACKUP_STORAGE_NOT_EXIST(700007, "Middleware backup storage is not exist.", "中间件备份的存储不存在"),
    MIDDLEWARE_SIZE_LIMIT(700008, "The number of Middleware reaches the limit. Please update the license.", "中间件数量达到上限，请更新license"),
    MIDDLEWARE_UPLOAD_FAILED(700009, "Upload failed!","上传失败"),
    MIDDLEWARE_UPDATE_MYSQL_CONFIG_FAILED(700010, "Failed to update custom config to mysql!", "同步自定义配置至数据库失败"),
    CREATE_DYNAMIC_FORM_FAILED(700011, "Failed to create dynamic form!", "生成动态表单失败"),
    CREATE_MIDDLEWARE_OPERATOR_FAILED(700012, "Failed to create middleware operator", "创建operator失败"),
    MIDDLEWARE_STILL_BE_USED(700013, "This version is being used by other cluster and cannot be taken down", "该版本正在被其余集群使用，不能下架"),
    MIDDLEWARE_SERVICE_EXIST(700014, "Exist released middleware service", "存在已发布中间件服务，不能删除"),
    FIND_CACHE_MIDDLEWARE_FAILED(700015, "failed to fing cache middleware", "查询已删除中间件缓存记录失败"),
    MIDDLEWARE_REBOOT_FAILED(700016, "failed to restart middleware", "服务重启失败"),
    FIND_POD_IN_MIDDLEWARE_FAIL(700017, "failed to find pod in middleware", "通过middleware确认pod失败"),
    PARSE_VALUES_FAILED(700018, "failed to parse values from string to JSONObject", "将values.yaml转换为JSONObject失败"),
    MYSQL_CONFIG_UPDATE_FAILED(700019, "failed to set global for mysql", "mysql手动执行set global设置参数失败"),
    REDIS_CONFIG_UPDATE_FAILED(700020, "failed to config set for redis", "redis手动执行config set设置参数失败"),
    CUSTOM_CONFIG_IS_EMPTY(700021, "custom config is empty", "参数配置列表不能为空"),
    CUSTOM_CONFIG_VALUE_IS_EMPTY(700022, "custom config values is empty", "参数配置数值不能为空"),
    CUSTOM_CONFIG_TEMPLATE_EXIST(700023, "custom config template exist", "存在同名模板"),
    MIDDLEWARE_CONTROLLER_NOT_INSTALL(700024, "middleware controller component not install", "中间件管理组件未安装"),
    MIDDLEWARE_CONTROLLER_INSTALL_FAILED(700025, "middleware controller component install failed", "安装middleware-controller失败"),
    SAME_NAME_MIDDLEWARE_STORAGE_EXIST(700026, "same name middleware storage still exist", "存在同名中间件未清除数据"),
    CRD_NOT_EXISTED(700027, "crd not exist", "缺少crd资源"),
    RESOURCE_ALREADY_EXISTED(700028, "the component already exists", "该组件已存在"),
    MIDDLEWARE_MANAGER_PLATFORM_NOT_SUPPORT(700029, "middleware manager platform not support", "此类型中间件暂不支持管理控制台"),
    MIDDLEWARE_PVC_SCALE_UP_FAILED(700030, "middleware pvc scale up failed", "中间件存储扩容失败"),
    MIDDLEWARE_PVC_ROLL_BACK_FAILED(700031, "middleware pvc roll back failed", "中间件存储回滚失败"),
    NAMESPACE_QUOTA_NOT_ENOUGH(700032, "namespace quota not enough", "分区配额不足"),
    MIDDLEWARE_MAINTENANCE_SCALE_UP_NOT_FOUND(700033, "middleware maintenance scale up not found", "中间件存储扩容 cr对象未找到"),
    MAINTENANCE_STATUS_ERROR(700034, "maintenance status error", "扩容cr状态异常"),
    GET_CUSTOM_CONFIG_ROLE_FAILED(700035, "get custom config role failed", "获取自定义配置节点类型失败"),
    CUSTOM_CONFIG_ROLE_CAN_NOT_BE_NULL(700036, "custom config role can not be null", "参数节点类型不能为空"),
    UPDATE_CUSTOM_CONFIG_FAILED(700037, "update config config failed", "更新自定义参数失败"),
    MONGODB_GET_ORGAN_ID_FAILED(700038, "mongodb get organ Id failed", "获取组织id失败"),

    BACKUP_ALREADY_EXISTS(710000, "Backup already exists", "备份已存在"),
    BACKUP_RECORD_MAY_NOT_EXIST(710001, "Record delete failed,record may not exist", "删除失败，备份记录可能不存在"),
    BACKUP_RESTORE_FAILED(710002, "backup restore failed", "备份恢复失败"),
    BACKUP_JOB_NAME_ALREADY_EXISTS(710003, "The backup job name already exists", "备份任务名称已存在"),
    CREATE_INCREMENT_BACKUP_FAILED(710004, "create increment backup failed", "创建增量备份失败"),
    MIDDLEWARE_BACKUP_SCHEDULE_EXIST(710005, "middleware backup schedule existed", "已存在定时备份，不可创建多个"),
    MIDDLEWARE_BACKUP_CRON_ILLEGAL(710006, "The max cron interval must small than retentionTime","备份最大间隔时间必须小于备份保留时间"),
    BACKUP_NOT_EXISTS(710007, "backup record not exists", "备份记录不存在"),
    FAILED_TO_DELETE_BACKUP_RESTORE(710008, "failed to delete backup restore", "删除克隆记录失败"),
    RUNNING_SCHEDULE_BACKUP_EXISTED(710009, "There is an ongoing schedule backup task.", "存在运行中的周期备份任务"),
    INCREMENTAL_BACKUP_NOT_EXIST(710010, "Incremental backup not exist", "增量备份任务不存在"),

    UPGRADE_LOWER_VERSION_FAILED(720000, "Can't upgrade to lower version", "不能升级到更低版本"),
    UPGRADE_OVER_VERSION_FAILED(720001, "Can't upgrade over big version", "不能跨大版本升级"),
    UPGRADE_NOT_SATISFY_LOWEST_VERSION(720002, "Not satisfy lowest version", "不满足升级所需最低版本"),
    UPGRADE_OPERATOR_UPDATING(720003, "Operator is updating", "Operator正在升级中"),
    UPGRADE_OPERATOR_TOO_LOWER(720004, "Current operator version is too lower,please upgrade operator", "当前中间件版本太低,请先升级operator"),
    OPERATOR_INFO_ERROR(720005,"operator info error","对应operator信息有误"),

    // postgresql
    POSTGRESQL_CREATE_DATABASE_FAILED(730001, "postgresql create database failed", "postgresql创建数据库失败"),
    POSTGRESQL_UPDATE_DATABASE_FAILED(730002, "postgresql update database failed", "postgresql更新数据库失败"),
    POSTGRESQL_DELETE_DATABASE_FAILED(730003, "postgresql delete database failed", "postgresql删除数据库失败"),
    POSTGRESQL_CREATE_SCHEMA_FAILED(730004, "postgresql create schema failed", "postgresql创建模式失败"),
    POSTGRESQL_UPDATE_SCHEMA_FAILED(730005, "postgresql update schema failed", "postgresql更新模式失败"),
    POSTGRESQL_DELETE_SCHEMA_FAILED(730006, "postgresql delete schema failed", "postgresql删除模式失败"),
    POSTGRESQL_CREATE_TABLE_FAILED(730007, "postgresql create table failed", "postgresql创建表失败"),
    POSTGRESQL_UPDATE_TABLE_FAILED(730008, "postgresql update table failed", "postgresql更新表失败"),
    POSTGRESQL_DELETE_TABLE_FAILED(730009, "postgresql delete table failed", "postgresql删除表失败"),
    POSTGRESQL_DATABASE_NOT_FOUND(730010, "postgresql database not found", "postgresql 数据库未找到"),
    POSTGRESQL_SCHEMA_NOT_FOUND(730011, "postgresql schema not found", "postgresql 模式未找到"),
    POSTGRESQL_TABLE_NOT_FOUND(730012, "postgresql table not found", "postgresql 表未找到"),
    POSTGRESQL_CREATE_FOREIGN_KEY_FAILED(730013, "postgresql create foreign key failed", "postgresql创建外键失败"),
    POSTGRESQL_CREATE_UNIQUE_FAILED(730014, "postgresql create unique failed", "postgresql创建唯一约束失败"),
    POSTGRESQL_CREATE_EXCLUDE_FAILED(730015, "postgresql create exclusion failed", "postgresql创建排它约束失败"),
    POSTGRESQL_CREATE_CHECK_FAILED(730016, "postgresql create check failed", "postgresql创建检查约束失败"),
    POSTGRESQL_DROP_CONSTRAINT_FAILED(730017, "postgresql drop constraint failed", "postgresql删除约束失败"),
    POSTGRESQL_ADD_TABLE_INHERIT_FAILED(730018, "postgresql add table inherit failed", "postgresql添加表继承失败"),
    POSTGRESQL_DROP_TABLE_INHERIT_FAILED(730019, "postgresql drop table inherit failed", "postgresql取消表继承失败"),
    POSTGRESQL_CREATE_USER_FAILED(730020, "postgresql create user failed", "postgresql创建用户失败"),
    POSTGRESQL_DELETE_USER_FAILED(730021, "postgresql delete user failed", "postgresql删除用户失败"),
    POSTGRESQL_UPDATE_USER_FAILED(730022, "postgresql update user failed", "postgresql更新用户失败"),
    POSTGRESQL_GRANT_USER_DATABASE_FAILED(730023, "postgresql grant user database failed", "postgresql赋权用户库权限失败"),
    POSTGRESQL_GRANT_USER_SCHEMA_FAILED(730024, "postgresql grant user schema failed", "postgresql赋权用户模式权限失败"),
    POSTGRESQL_GRANT_USER_TABLE_FAILED(730025, "postgresql grant user table failed", "postgresql赋权用户表权限失败"),
    POSTGRESQL_REVOKE_USER_DATABASE_FAILED(730026, "postgresql revoke user database failed", "postgresql取消赋权用户库权限失败"),
    POSTGRESQL_REVOKE_USER_SCHEMA_FAILED(730027, "postgresql revoke user schema failed", "postgresql取消赋权用户模式权限失败"),
    POSTGRESQL_REVOKE_USER_TABLE_FAILED(730028, "postgresql revoke user table failed", "postgresql取消赋权用户表权限失败"),
    POSTGRESQL_SELECT_FAILED(730029, "postgresql select failed", "postgresql查询数据失败"),
    MIDDLEWARE_API_REQUEST_ERROR(730030, "middleware manager center request error", "中间件管理中心接口请求失败"),
    POSTGRESQL_ADD_TABLE_COLUMN_IS_NULL(730031, "postgresql create table, column could not be null", "postgresql创建表，列信息不能为空"),
    POSTGRESQL_USER_RESET_PASSWORD_FAILED(730032, "postgresql user reset password failed", "重置用户密码失败"),
    POSTGRESQL_SYNC_POD_NOT_EXIST(730033, "postgresql sync pod not exist", "postgresql未存在同步节点，不可切换"),
    // mysql dashboard
    CREATE_DATABASE_FAILED(800000, "failed to create database", "创建数据库失败"),
    DELETE_DATABASE_FAILED(800001, "failed to delete database", "删除数据库失败"),
    ALTER_DATABASE_FAILED(800002, "failed to update database", "更新数据库失败"),
    DATABASE_EXISTS(800003, "database exists", "该数据库已存在"),
    FAILED_TO_EXEC_QUERY(800004, "failed to exec query", "SQL执行失败"),

    // backup server
    FAILED_TO_SAVE_BACKUP_SERVER_DETAIL(740000, "failed to save backup server detail", "保存备份服务器详细信息失败"),
    BACKUP_SERVER_ALREADY_USED(740001, "This item has already created a backup location using this backup server, and cannot be created again", "该项目已使用该备份服务器创建过备份位置了，无法再次创建"),
    BACKUP_SERVER_NOT_FOUND(740002, "can't found backup server", "未找到服务器"),
    SERVER_NAME_ALREADY_EXISTS(740003,"the server name already exists","服务器名称已存在"),
    SERVER_ADDRESS_ALREADY_EXISTS(740004,"the server address already exists","服务器地址已存在"),
    AUTHORIZATION_FAILED(740005, "authorization failed", "用户名或密码错误"),
    FAILED_TO_DELETE_BACKUP_SERVER(740006, "failed to delete this backup server,which a backup task used it already", "存在使用该备份服务器的备份任务，无法删除该服务器"),
    FAILED_TO_DELETE_BACKUP_POSITION(740007, "There are backup tasks that use this backup location, and it cannot be deleted", "存在使用该备份位置的备份任务，无法删除该备份位置"),

    // user
    CREATE_MYSQL_USER_FAILED(810001, "failed to create user", "创建用户失败"),
    DELETE_MYSQL_USER_FAILED(810002, "failed to create user", "删除用户失败"),
    MYSQL_USER_EXISTS(810003, "user already exists", "该用户已存在"),
    MYSQL_DB_EXISTS(810004, "db already exists", "该数据库已存在"),
    MYSQL_LOGIN_FAILED(810005, "login failed,wrong username or password", "登录失败，用户名或密码错误"),
    MYSQL_USER_NOT_EXISTS(810006, "user not exists", "该用户不存在"),
    FAILED_TO_UPDATE_USER_PASSWORD(810007, "failed to update user password", "更新用户密码失败"),
    LOCK_USER_FAILED(810008, "failed to lock user", "锁定用户失败"),
    UNLOCK_USER_FAILED(810009, "failed to unlock user", "解锁用户失败"),
    GRANT_DATABASE_FAILED(810010, "failed to grant database privilege", "授权数据库权限失败"),
    GRANT_TABLE_FAILED(810011, "failed to grant table privilege", "授权数据表权限失败"),
    REVOKE_DATABASE_FAILED(810012, "failed to revoke database privilege", "释放数据库权限失败"),
    REVOKE_TABLE_FAILED(810013, "failed to revoke table privilege", "释放数据表权限失败"),
    FAILED_TO_UPDATE_USER(810014, "failed to update user", "更新用户失败"),

    // table
    CREATE_TABLE_FAILED(820000, "failed to create table", "创建数据表失败"),
    DELETE_TABLE_FAILED(820001, "failed to delete table", "删除数据表失败"),
    ALTER_TABLE_FAILED(820002, "failed to update table", "更新数据表失败"),
    OBTAIN_TABLE_DETAIL_FAILED(820003, "failed to obtain table detail", "获取表详情失败"),
    FAILED_TO_EXPORT_TABLE_SQL(820004, "failed to export table sql", "导出建表sql文件失败"),
    FAILED_TO_EXPORT_TABLE_EXCEL(820005, "failed to export table excel", "导出表结构Excel文件失败"),
    FAILED_TO_OBTAIN_TABLE_DATA(820006, "failed to obtain table data", "查询表数据失败"),
    FAILED_TO_OBTAIN_TABLE_RECORD(820007, "failed to obtain table recordd", "查询表记录数  失败"),
    INCOMPLETE_TABLE_COLUMN(820008, "a table must have at least one column", "数据表至少应包含一行列信息"),
    ALTER_TABLE_COLUMN_FAILED(820009, "failed to update table columns", "更新数据表列信息失败"),
    ALTER_TABLE_INDICES_FAILED(820010, "failed to update table indices", "更新数据表索引信息失败"),
    ALTER_TABLE_FOREIGN_KEYS_FAILED(820011, "failed to update table foreign keys", "更新数据表外键信息失败"),

    //redis
    FAILED_TO_LOGIN_REDIS(860001, "failed to login redis,maybe wrong username or password", "登录redis失败，您的用户名或密码可能有误"),
    FAILED_TO_QUERY_KEY(860002, "failed to query key info", "查询key信息失败"),
    UNKNOWN_REDIS_CLUSTER(860003, "unknown redis cluster", "未知的redis集群模式"),
    TOO_LONG_EXPIRE_TIME(860004, "too long expire time", "超时时间不得超过2^31-1"),

    ERROR_PG_POD_NUm(870001, "error postgresql pod num", "postgresql从节点数量不合法"),

    // disasterRecovery
    RELATION_ADDR_NOT_EXIST(900001,"relation service address not exist","备平台链接地址不存在"),
    SWITCH_PLATFORM_NOT_SUPPORT(900002,"switch platform is not support","平台灾备切换功能不支持"),
    CONNECT_REMOTE_HOST_FAILED(900003,"please check your relation cluster is running or your connection address is right","请检查您的备集群是否正常运行，或链接地址是否正确"),
    REMOTE_SWITCH_FAILED(900004,"switch failed, please contact Administrator","切换失败，请联系系统管理员"),
    SWITCH_NO_POWER(900005,"check whether the platform is connected to the primary platform","请检查平台是否被主平台接入"),
    DISASTER_RECOVERY_NOT_SUPPORT(900006,"the primary platform does not support the dissterRecovery","主平台不支持灾备功能"),
    DISASTER_ONLY_ADMIN_CAN_LOGIN(900007,"the current platform is standby and only the super administrator can log in","当前为备平台，仅支持超级管理员登录")
    ;

    private final int code;
    private final String enMsg;
    private final String zhMsg;

    ErrorMessage(int code, String enMsg, String zhMsg) {
        this.code = code;
        this.enMsg = enMsg;
        this.zhMsg = zhMsg;
    }

    public int getCode() {
        return code;
    }

    public  String getMsg() {
        LanguageErrorEnumUtilService languageErrorEnumUtilService = null;
        try {
            languageErrorEnumUtilService = SpringContextUtils.getBeanIgnoreNotFound(LanguageErrorEnumUtilService.class);
        } catch (Exception e) {
            return this.zhMsg;
        }
        if (languageErrorEnumUtilService == null) {
            return this.zhMsg;
        }
        return languageErrorEnumUtilService.translateByErrorCodeWithDefault(this.code, this.zhMsg);
    }

    @Deprecated
    public String getEnMsg() {
        return enMsg;
    }

    public String getZhMsg() {
        return zhMsg;
    }


    public static void main(String[] args) {
        for (ErrorMessage errorMessage : ErrorMessage.values()) {
            System.out.println(errorMessage.getCode() + "=" + errorMessage.getEnMsg());
        }
    }
}
