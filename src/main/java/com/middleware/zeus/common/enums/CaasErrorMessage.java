package com.middleware.zeus.common.enums;

public enum CaasErrorMessage {
    // common
    UNKNOWN(100001, "System error, please contact Administrator.", "系统错误，请联系系统管理员"),
    INVALID_PARAMETER(100002, "Invalid parameter.", "参数错误"),
    VALIDATE_FAILED(100003, "Validate failed.", "校验失败"),
    DATE_FORMAT_PARSE_ERROR(100004, "Format date string to date failed", "无法转换无效的日期格式"),
    CREATE_TEMPORARY_FILE_ERROR(100005, "Fail to create the temporary file", "创建临时文件失败"),
    SOURCE_FILE_NOT_FOUND(100006, "Source file not found", "源文件不存在"),
    TARGET_FILE_NOT_FOUND(100007, "Target file not found", "目标文件不存在"),
    IO_FAILED(100008, "I/O failed", "I/O失败"),
    AUTH_FAILED(100009, "Auth failed", "认证失败"),

    NOT_SUIT_OPERATOR_IMPL(100009, "Cannot found suit operator for this interface. please contact Administrator.", "未能找到适配的操作实现类，请联系系统管理员"),
    NOT_SUIT_FACTORY_IMPL(100010, "Cannot found suit factory for this interface. please contact Administrator.", "未能找到适配的工厂族，请联系系统管理员"),
    QUERY_FAIL(100011, "Query failed.", "查询失败"),
    NOT_EXIST(100012, "Not exists.", "不存在"),
    CREATE_FAIL(100013, "Create fail.", "创建失败"),
    UPDATE_FAIL(100014, "Update fail.", "更新失败"),
    NOT_BLANK(100015, "Can not be blank.", "不能为空"),
    FILE_DOWNLOAD_FAIL(100016, "File download fail.", "文件下载失败"),
    CMD_RUN_FAILED(100017, "Cmd run failed", "命令运行失败"),
    RESOURCE_NOT_EXIST(100018, "Resource is not exist at this namespace", "该命名空间下未发现该资源"),
    CONFLICT(100019, "Resource exist or name conflict", "资源已存在或名称冲突"),

    // internalService
    PARAMETER_VALUE_NOT_PROVIDE(101012, "Parameter cannot be null.", "参数不能为空"),
    SERVICE_DELETE_SOME_FAIL(101013, "Fail to delete some services.", "部分关联删除失败"),
    DELETE_FAIL(101014, "Delete failed.", "删除失败"),
    ADD_FAIL(101015, "add fail.", "添加失败"),
    NOT_FOUND(101017, "Not found.", "未找到"),
    PARAMETER_EDGENODENAME_VALUE_NOT_PROVIDE(101018, "Parameter cannot be null.", "边缘节点组名称不能为空"),
    SERVICE_ASSOCIATED_LABS_CONFLICT(101019, "The service associated labs %s conflict, please retry.", "服务选择标签%s在部署中已存在关联标签，请选择其他部署进行关联"),
    NAMESPACE_IS_NULL(101020, "namespace cannot be null.", "命名空间为空，请选择命名空间再进行操作"),

    // registry
    REGISTRY_NOT_FOUND(200001, "Registry not found.", "制品服务未找到"),
    REGISTRY_CONNECTION_FAILED(200002, "Cannot connect registry component.", "无法连接到制品服务器"),
    REGISTRY_PROJECT_AUTH_STRING_NULL(200003, "Auth string of  resource group for registry cannot be null.", "资源集与制品服务器的认证信息不能为空"),
    REGISTRY_AUTH_STRING_NULL(200004, "Auth string for registry cannot be null.", "制品服务器的认证信息不能为空"),
    REGISTRY_CALL_FAILED(200005, "call registry api failed.", "调用制品服务器接口失败"),
    REGISTRY_NAME_DUPLICATED(200006, "Registry name duplicated.", "制品服务名称已存在"),
    REGISTRY_NICKNAME_DUPLICATED(200007, "Registry nickname duplicated.", "制品服务的显示名称已存在"),
    REGISTRY_USER_CREATE_FAILED(200008, "Fail to create the Registry user.", "制品服务用户创建失败"),
    REGISTRY_REPO_NOT_FOUND(200009, "Repository not found.", "制品服务仓库未找到"),
    REGISTRY_REPO_RETENTION_RULE_POLICY_NOT_SUPPORT(200010, "Retention rule policy is not support", "平台不支持的镜像保留规则"),
    REGISTRY_REPO_RETENTION_TIMING_CANNOT_SET_WITH_EMPTY_RULE(200011, "Retention timing cannot be set when retention rule is empty", "定时任务无法在无规则的情况下配置"),
    REGISTRY_TARGET_UNHEALTHY(200012, "Target registry is unhealthy, please add after test.txt connect successful.", "目标仓库不健康，请在测试连接通过后重试"),
    REGISTRY_USER_NOT_FOUND(200013, "Registry user not found", "制品服务用户不存在"),
    REGISTRY_URL_DUPLICATED(200014, "Registry url duplicated.", "制品服务url已存在"),
    REGISTRY_REPO_RETENTION_RULE_POLICY_PARAM_TOO_LONG(200015, "The number of retention rule param is gather 1, not support.", "平台不支持参数大于1的镜像保留规则"),
    REGISTRY_TYPE_NOT_SUPPORT_INTERFACE(200016, "Current registry type or version cannot support this function", "该类型或版本的制品服务不支持此功能"),
    SYSTEM_REPO_CANNOT_OPERATE(200017, "It's not allowed to operate system repository", "系统仓库不允许操作"),
    REGISTRY_JFROG_NOT_SUPPORT(200018, "jfrog not support.", "jfrog制品服务不支持该功能"),
    REGISTRY_DELETE_FORBIDDEN(200019,"Can't delete registry,replication policies use it as destination registry","不能删除该制品服务器，因为有复制策略在使用中"),
    REGISTRY_USER_UNAUTHORIZED(200020, "user unauthorized when binding tenant project with registry","绑定租户项目的时候，该用户认证失败"),
    REGISTRY_REPLICATION_ERROR(200021,"there are no successful execution tasks of this registry replication policy","当前的镜像复制策略没有Task是成功状态的记录条数"),

    //角色权限 203xxx
    ROLE_NOT_EXIST(203001, "Role not exist.","角色不存在!"),

    // registry project mapping
    REGISTRY_PROJECT_NOT_BIND(210001, "Registry is not bind to the  resource group", "制品服务未绑定该资源集"),
    REGISTRY_PROJECT_UNBIND_FAIL(210002, "Fail to unbind registry and  resource group", "制品服务与资源集解绑失败"),
    PROJECT_HAS_NO_PERMISSION_IN_REPO(210003, "Resource group or role has no permission in this repo", "该资源集或角色不具有仓库的权限"),
    REGISTRY_PROJECT_NAME_EXISTS(210004,"The name of Registry already exists ","制品服务的名称已经存在，请更换其他名称！"),
    REGISTRY_PROJECT_NAME_NOT_EXISTS(210005,"The name of Registry project not exists","制品服务的项目名称不存在！"),
    // helm chart
    REGISTRY_HELM_CHART_FILE_TRANSFER_ERROR(211001, "Fail to transfer the helm chart file", "传输chart包失败"),
    HELM_CHART_UNZIP_ERROR(211002, "Fail to unzip the helm chart file", "解压chart文件失败"),
    HELM_CHART_WRITE_ERROR(211003, "Fail to write the helm chart file", "写出chart文件失败"),
    CHART_YAML_READ_FAIL(211004, "Fail to read the chart yaml", "读取Chart.yaml文件失败"),
    HELM_CHART_FORMAT_ERROR(211005, "Helm chart format error", "chart文件格式错误"),

    // docker
    FAILED_CONNECT_DOCKERD(220001, "Cannot connect to dockerd", "无法连接到dockerd"),
    MULTI_IMAGE_UPLOAD_NOT_SUPPORT(220002, "Multi-image upload in one package not support", "暂不支持一个压缩包中包含多个镜像的上传"),
    DOWNLOAD_NEED_BEFORE_FETCH(220003, "Download image must before fetch", "下载镜像之前需要拉取镜像到本地"),
    DOCKER_CONNECT_TYPE_NOT_SET(220004, "Docker connect type is not set.", "未设置docker连接方式"),
    // image
    IMAGE_NOT_FOUND(230001, "Image not found", "镜像不存在"),
    IMAGE_TAG_NOT_FOUND(230002, "Image tag not found", "镜像版本不存在"),
    IMAGE_TAG_ALREADY_EXISTS(230003, "Image tag already exists", "镜像版本已存在"),
    IMAGE_SYNC_DEST_EXIST(230004, "Sync image error. Same image name and tag exist in dest repository", "推送失败, 目标镜像已存在"),
    IMAGE_SYNC_POLICY_CREATE_FAIL(230005, "Sync image error. Fail to create the image replication policy", "推送失败, 复制策略创建失败"),
    IMAGE_HAS_NEVER_BEEN_SCANED(230006, "The image has never been scaned , please scan it before get scan detail", "该镜像无扫描记录，请在获取扫描详情前先进行镜像扫描"),

    // registry labels
    LABEL_ALREADY_EXIST(240001, "The same name label is already existed", "同名标签已存在"),

    // registry retention rules
    RETENTION_RULE_NOT_EXIST(250001, "The retention rule not exist", "尚未创建镜像清理规则"),

    // target registry ping
    TARGET_REGISTRY_INVALID(250001, "Target registry is invalid, please check metadata", "备份服务器信息有误，请检查信息填写是否有误或检查目标服务器状态"),
    TARGET_REGISTRY_NOT_FOUND(250002, "Target registry not found", "找不到备份服务器，请检查信息填写是否有误或检查目标服务器状态"),
    TARGET_REGISTRY_DUPLICATED(250003, "Target registry is duplicate, please check name and url", "备份服务器已存在，请检查名称和地址"),

    // cluster
    CLUSTER_NOT_FOUND(300001, "Cluster not found", "集群未找到"),
    CLUSTER_AUTH_STRING_NULL(300002, "Auth string for cluster cannot be null", "集群认证信息不能为空"),
    CLUSTER_AUTH_NOT_SUPPORT(300003, "Not support auth type", "不支持的集群认证方式"),
    CLUSTER_CONNECTION_FAILED(300004, "Cannot connect cluster", "无法连接到集群"),
    CLUSTER_CALL_FAILED(300005, "Call cluster api failed", "调用集群接口失败"),
    CLUSTER_RESOURCE_ALREADY_EXIST(300006, "Resource already exist in cluster", "资源已经存在与集群中"),
    CLUSTER_COMP_ADDRESS_NOT_FOUND(300007, "Cluster component address not found", "集群组件地址未找到"),
    CLASS_OPERATOR_NOT_FOUND(300008, "class operator not found", "operator未找到，请尝试切换到边缘集群进行操作"),
    CLASS_FACTORY_NOT_FOUND(300009, "class factory not found", "factory未找到，请尝试切换到边缘集群进行操作"),
    CLUSTER_NETWORK_TYPE_NOT_SUPPORT(300010, "Cluster network type is not supported", "集群网络类型不支持"),

    SERVICE_ALREADY_EXIST(300010, "service already exist", "在此集群中服务已存在，请修改服务后继续操作"),
    TARGETPORT_ERROR(300011, "targetPort is error", "请检查targetPort，范围为：1-65535"),
    SERVICE_NAME_REPEAT(300012, "service name repeat", "创建服务类型中存在相同名称，请修改后创建"),
    SERVICE_PORT_REPEAT(300013, "service port repeat", "创建服务类型中存在相同port，请修改后创建"),

    // node 301xxx
    EDGE_NODE_PACKAGE_NOT_FOUND(301000, "Install package not found", "安装包未找到，请先生成"),
    NODE_LABEL_CREATE_ERROR(301001, "Node label create failed.","主机标签创建失败"),
    NODE_LABEL_ERROR(301002, "Node label error.","主机标签错误"),
    NODENAME_NOT_BLANK(301003, "Node name can not blank.","主机名称不能为空"),
    NODE_NOT_EXIST(301004, "Node was not existed.","主机不存在"),
    NODE_EXIST(301005, "Node was existed.","主机已经存在"),
    SYSTEM_POOL_NODE_CAN_NOT_OFFLINE(301006, "Master node and system node pool's nodes can not remove.","主控节点和系统资源池节点不能下线"),
    DRAIN_IN_PROCESS(301007, "There is already a migrate progress in process.","主机应用迁移已在进行中"),
    NODE_STATUS_REQUIRE(301008, "Only allow for idle or stateless node","只能修改闲置主机或者无状态主机"),
    PRIVATE_NODE_NOT_EXIST(301009, "Warn: Can not find private node for this namespace, node label has been modified",
            "警告：已经删除当前命名空间，但是当前命名空间为私有命名空间，所属独占主机属性被修改，导致独占主机不存在，请不要随意更改主机私有属性，以免发生冲突，如果在接下来的操作中遇到其它问题，请联系管理员!"),
    NODE_NOT_REMOVE(301010, "The node has other pods ,please delete it.","主机有其他应用pod，请删除pod后再移除"),
    NODE_POD_NONE(301011, "There is no pod on the node.","主机上没有运行pod"),
    NODE_CANNOT_REMOVED(301012, "Node can not be removed.","主机已经分配给租户不能下线，请在对应的租户配额中移除该主机后下线!"),
    NODE_CANNOT_REMOVED_FORTENANT(301013, "Node can not be removed as it is allocated to a namespace, please remove it from the namespace first.","主机已经分配给命名空间不能移除，请在对应的命名空间中移除该主机!"),
    NODE_LABEL_UPDATE_ERROR(301014, "Node status update failed.","主机状态更新失败"),
    NODE_UNSCHEDULABLE_ONLY(301015, "Only allow for maintain node.","只允许操作维护状态主机"),
    NODE_STATUS_NOT_REMOVE(301016, "The node has other pods ,please drain application.","主机有其他应用pod，请应用迁移后再修改"),
    NODE_TYPE_UNKNOWN(301017, "Unknown node type.","节点类型不能识别"),
    NODE_REMOVE_RESOURCE_FAIL(301018, "Allocated resource quota need this node resource, please change tenant cluster quota.","已分配的集群配额需要包括该节点的资源，请先调整租户集群配额"),
    NODE_UP_NET_SEGMENT_USED(301019, "The network segment has been used.", "网段已被使用"),
    NODE_UP_NET_SEGMENT_CHECK_FAILED(301020, "Failed while check network segment", "网段校验失败"),
    NODE_UP_NET_SEGMENT_CREATE_FAILED(301021, "Failed to create network segment","网段创建失败"),
    NODE_SEGMENT_VALID_FAILED(301022,"The network segment is invalid","网段不合法"),
    NODE_SIZE_LIMIT(301023, "The number of nodes reaches the limit. Please update the license.", "节点数量达到上限，请更新license"),
    DATA_COLL_NOT_DEPLOY(301024, "Please deploy data-coll service when install netsniffer.", "安装NetSniffer请先部署data-coll服务。"),
    NODE_AGENT_NO_DEPLOY(301025, "Please deploy node agent on the host first.", "请先在主机上部署并开启隔离组件"),
    ISOLATE_LOCK_GET_ERROR(301026, "Get etcd key error", "获取节点隔离锁失败"),
    ISOLATE_LOCK_REVOKE_ERROR(301027, "Revoke etcd lease error", "获取节点隔离锁失败"),
    LV_WITH_VG_NAMED_DOCKER_NOT_EXIST(301028, "Logical volume with vg named docker does not exist", "该节点docker命名空间未分配"),
    NODE_MISS_LIBSECCOMP(301029, "Library libseccomp.so.2 is not installed.", "服务器缺少依赖包libseccomp.so.2"),
    HOST_DUPLICATED(301030, "host duplicated.", "主机重复"),
    DEFAULT_POOL_FORBID_DELETE(301031, "It's not allowed to delete the default node pool.", "不允许删除默认资源池"),
    DEFAULT_POOL_FORBID_DELETE_NODE(301032, "It's not allowed to delete the default node pool's node.", "不允许删除默认资源池的主机"),
    TAINT_EFFECT_ERROR(301033, "Effect only support values : NoSchedule, PreferNoSchedule, NoExecute.", "污点策略只能填写NoSchedule, PreferNoSchedule, NoExecute"),
    K8S_SYSTEM_LABEL_NOT_ALLOW(301034, "It's not allowed to choose the K8S system label", "不允许使用k8s系统标签，请重新选择"),
    DEFAULT_POOL_FORBID(301035, "It's not allowed to operate the default node pool.", "不允许操作默认资源池"),
    CPU_CORE_GET_ERROR(301036, "Failed to query node cpu cores.", "查询节点的cpu核数失败"),
    CPU_SIZE_LIMIT(301037,"The cpu resources reach the limit. Please update the license.", "CPU数量达到上限，请更新license"),
    RESOURCE_LIMIT_CHECK_ERROR(301038,"Resource limit of license check failed. Please update the license.", "校验license授权资源失败，请更新license"),
    CLUSTER_COMPONENT_INSTALL_FORBBIDE(301039,"It's not allowed to install this component.", "该组件不允许安装."),
    CLUSTER_COMPONENT_UNINSTALL_FORBBIDE(301040,"It's not allowed to uninstall this component.", "该组件不允许卸载."),
    NODE_ISOLATION_NETWORK_REQUIRE(301041, "Only calico network support for node isolation.", "主机故障隔离仅支持Calico网络集群"),
    NODE_AUTOSCALING(301037, "The node is autoscaling and can not be edited", "主机正在扩缩容当中，无法修改"),

    // 远程调用 302xxx
    REMOTE_INVOCATION_ERROR(302001, "Storage remote invocation error","storageClass远程调用错误"),
    STORAGE_REMOTE_INVOCATION_EXCEPTION(302002, "Storage remote invocation encounters exception", "storageClass远程调用异常"),
    STORAGE_NOT_FOUND(302002, "Storage remote invocation encounters exception", "storageClass远程调用异常"),

    // 资源相关nodepool 305xxx
    NODE_POOL_WITH_NODE_OR_QUOTA(305001, "There exist nodes or allocated tenant quota in node poll %s, please remove them.", "节点组%s下存在节点或已分配配额，请移除后再操作"),
    NODE_POOL_QUOTA_NOT_ENOUGH(305002, "The current node pool's total resources cannot be less than the quotas allocated by the organize", "当前节点组的总资源不能小于组织已分配的配额"),
    NODE_POOL_LABELS_AND_TAINTS_NOT_NULL(305003, "The node pool's labels or taints are not null", "资源池标签或污点不能为空"),
    NODE_POOL_RESOURCE_LESS_THAN_USED_QUOTA(305004, "nodepool's total resources cannot be less than the quotas allocated by tenants","资源池的总资源不能小于租户已分配的配额"),
    CPU_QUOTA_EXCEED(305005, "CPU quota exceed allocatable quota", "CPU配额超出了可分配配额"),
    MEMORY_QUOTA_EXCEED(305006, "Memory quota exceed allocatable quota", "内存配额超出了可分配配额"),
    GPU_CORE_QUOTA_EXCEED(305007, "GPUCore quota exceed allocatable quota", "GPU核数配额超出了可分配配额"),
    GPU_MEMORY_QUOTA_EXCEED(305008, "GPUMemory quota exceed allocatable quota", "GPU显存配额超出了可分配配额"),
    NO_EXTENSION_RESOURCE(305009, "No extension class resource is configured", "没有配置的扩展类资源"),
    THIS_EXTENSION_RESOURCE_NOT_SUPPORT(305010, "This extension class resource is not supported", "不支持当前扩展类资源"),
    EXTRA_NODES_CONTAIN_THIS_LABELS_AND_TAINTS(305011, "Extra nodes contain this labels and taints, so which maybe cause some applications can not run normally", "额外的节点包含此标签和污点，有可能导致部分应用无法正常运行"),
    QUOTA_EXCEED(305012, "Resource quota exceed allocatable quota", "资源配额超出了可分配配额"),
    TENANT_THIS_NODE_POOL_QUOTA_NOT_EXIST(305013, "This node pool items quota of tenant are not exist, so can not update items quota", "租户当前资源池配额不存在，无法更新配额"),


    // pod
    POD_NOT_EXIST(400012, "Pod not exist.", "POD不存在"),

    // loadbalancer
    K8S_NGINX_LB_CANNOT_FOUND_USEFUL_HTTPS_PORT(500001, "Cannot found useful port for nginx ingress controller https port", "暂无适合NGINX负载均衡使用的HTTPS端口"),
    K8S_NGINX_LB_CANNOT_FOUND_USEFUL_HEALTH_PORT(500002, "Cannot found useful port for nginx ingress controller health port", "暂无适合NGINX负载均衡使用的健康检查端口"),
    K8S_NGINX_LB_CANNOT_FOUND_USEFUL_STATUS_PORT(500003, "Cannot found useful port for nginx ingress controller status port", "暂无适合NGINX负载均衡使用的状态端口"),
    LB_NOT_EXIST(500004, "Loadbalancer not exist", "负载均衡不存在"),
    LB_DOMAIN_EXIST(500005, "Domain already exist", "域名已存在"),
    LB_INVALID_CA(500006, "Invalid CA file", "无效的证书"),
    LB_DEFAULT_NOT_DELETE(500007, "The default ingress controller is not allowed to be deleted.", "全局负载均衡器不允许被删除"),
    LB_HAD_USED(500008, "The ingress controller is in using, cant not change port or delete.", "已有服务使用该负载均衡器, 不能修改端口或删除"),
    LB_NODE_HAD_USED(500008, "The node is in using, cant not change port or delete.", "负载均衡器的节点下有服务, 不能修改负载均衡器的节点"),
    LB_NAME_EXIST(500009, "English short name exists", "负载均衡名称已存在"),
    LB_NICKNAME_EXIST(500010, "Nickname exists", "负载均衡显示名称已存在"),
    LB_HTTP_PORT_USED(500011, "The http port of the ingress controller has been used.", "负载均衡器HTTP端口已经被使用"),
    LB_PORT_RANGE_NOT_FOUND(500012, "Cannot get the port range of the ingress controller.", "获取不到负载均衡器的端口范围"),
    LB_HTTP_PORT_ERROR(500013, "The port of the ingress controller is not in the specified range.", "负载均衡器的端口不在指定范围"),
    LB_PORT_UNSAFE(500014, "Please input another port as 87 and 95 is unsafe port for chrome.", "87和95端口对于chrome浏览器是非安全的端口，请使用别的端口"),
    LB_CA_CAN_NOT_FIND_IN_KUBE_SYSTEM(500015, "can't find root CA file at namespace of kube-system", "无法在kube-system命名空间下发现该证书"),
    LB_CA_IS_BEING_USED(500016,"This CA file is being used.","该证书正在被使用"),

    // ingress
    INGRESS_NOT_EXIST(600001, "ingress not exist", "对外路由不存在"),
    INGRESS_ALREADY_EXIST(600002, "ingress name already exist", "对外路由名称已存在"),
    INGRESS_DOMAIN_DUPLICATED(600003, "Domain duplicate.", "包含该域名的ingress已存在"),
    EXIST(600004, "Existing.", "已存在"),
    INGRESS_GROUP_DOMAIN_ALREADY_EXISTS(600005, "Ingress Group domain already exist.", "分组分流域名已存在"),
    INGRESS_GROUP_SERVICE_ALREADY_HAS_INGRESS(600006, "Service already has ingress.", "服务已存在ingress "),
    INGRESS_CONTROLLER_UN_EXISTS(600007, "the ingress-controller dose not exist.", "负载均衡器不存在"),


    //通用错误 1xxxxx

    ADMIN_NOT_FOUND(100006, "admin account not found.", "未找到admin用户"),
    RUN_COMMAND_ERROR(100007, "command run failed.", "命令执行失败"),
    OPERATION_FAIL(100008, "Operation failed.", "操作失败"),
    OPERATION_DISABLED(100009, "Operation disabled.", "功能已停用"),
    UPLOAD_FAIL(100014, "upload fail.", "上传失败"),
    SAVE_FAIL(100015, "save failed.", "保存失败"),
    GET_LDAP_CONF_FAIL(100016, "Retrieve ldap config fail..", "获取Ldap配置失败"),
    HTTP_EXCUTE_FAILED(100017, "Http excute failed.", "http访问失败."),
    FILE_CONTENT_BLANK(100018, "file content is blank.", "文件内容不能为空."),
    FILE_TYPE_SUPPORT(100019, "only support file type", "只支持的文件类型"),
    FILE_NOT_FOUND(100020, "file not found", "未找到文件"),
    PASSWORD_FORMAT_ERROR(100021, "Password should contain digital and letter, length between 7 and 12.", "密码格式为7-12位数字和字母的组合！"),
    INVALID_MEMORY_UNIT_TYPE(100022, "invalid memory unit type.", "内存单位类型错误"),
    NAME_EXIST(100023, "name exists", "名称已经存在"),
    FORMAT_ERROR(100024, "format error", "格式错误"),
    CONNECT_TIMEOUT(100025, "connect timeout", "连接超时"),
    CONNECT_FAIL(100026, "connect fail", "连接失败"),
    STARTED(100027, "had already started.", "已经启动"),
    STOPPED(100028, "had already stopped.", "已经停止"),
    AUTH_FAIL(100032, "Auth fail, please check username and password.", "认证未通过, 请检查用户名或密码是否正确"),
    PASSWORD_IS_EXPIRED(20038, "password is expired.", "密码已过有效期，请到容器平台修改密码后重新登录！"),
    START_DATE_AFTER_END(100033, "date error, start date is after end date.", "开始时间大于结束时间"),
    RESERVED_KEYWORD(100034, "reserved keyword, please choose another word", "保留关键字，请修改名称"),
    MESSAGE_SEND_ERROR(100035, "message send error", "短信发送失败"),
    ADMIN_NOT_ENBALE(100036, "admin account cannot be blocked.", "不能阻止admin用户"),
    HARBOR_HTTPS_ERROR(100037, "Harbor https call error.", "Harbor https访问失败"),
    DOWNLOAD_FAIL(100038, "download fail.", "下载失败"),
    FILE_NOT_EXIST_FAIL(100039, "File is not exist.", "文件不存在"),
    DATE_FROM_AFTER_TO(100040, "Date interval error, from date is after to date.", "日期区间错误，开始时间在结束时间之后"),
    SYSTEM_IN_MAINTENANCE(100041, "System is under maintenance.", "系统维护中"),
    RESPONSE_TIMEOUT(100042, "response timeout", "请求获取响应超时"),
    PAGE_SIZE_MAX_ERROR(100043, "Page size can not be greater than 1000.", "一页数量不能超过1000条记录"),
    DATE_FORMAT_ERROR(100044, "time format error", "时间格式错误"),
    UNSUPPORTED_SERVICE_TYPE(100045, "unsupported service type", "不支持的服务类型"),
    CLUSTERID_NOT_BLANK(100046, "clusterId can not be empty.", "clusterId不能为空"),
    CLUSTERID_NOT_SAME(100047, "clusterId can not be different.", "clusterId不能不一致"),
    CLUSTERID_NOT_EXIST(100047, "cluster not exist.", "集群不存在"),
    CLUSTERID_EXCEPTION(100048, "Exception in cluster.", "集群存在异常，请联系管理员！"),
    COMPADDRESS_BLANK(100048, "Compaddress is blank.", "集群Compaddress为空"),
    USERNAME_PASSWORD_ERROR(100049,"Username or password is not correct","用户名或密码错误"),
    LICENSE_INVALID_TIME_ERROR(100050, "The time of License is invalida, please update the valid time.", "注册码时间已无效，请联系服务商，更新注册码的有效时间"),
    CONFIG_INVALID(100051, "Invalid Config", "无效的配置"),
    NO_DATA_EXPORT(100052, "No data to export.", "没有需要导出的数据！"),
    RESPONSE_RESULT_EMPTY(100053, "Response is empty.", "响应结果为空！"),
    HTTP_ERROR(100054, "https call error.", "访问外部服务异常"),
    NOT_SUPPORT(100055, "not support", "不支持"),
    CHECK_FAIL(100056, "check fail.","校验失败"),
    GET_FAIL(100057, "get fail.","获取失败"),
    CHECK_CERT_ERROR(100059, "Wrong format certificate file", "格式错误的证书文件"),
    IDENTIFY_CA_ERROR(100060, "CA certificate not recognized", "未识别到CA证书"),
    IDENTIFY_CERT_ERROR(100061, "Certificate not recognized", "未识别到证书"),
    IDENTIFY_KEY_ERROR(100062, "The key was not recognized", "未识别到密钥"),
    CRD_QUERY_LIST_ERROR(100063, "The failed of query crd list", "查询crd列表失败"),
    CRD_QUERY_ERROR(100064, "The crd query failed", "查询crd失败"),
    UNAVAILABLE(100065, "is unavailable", "不可用"),
    ALLOCATE(100066, "is allocated", "已分配"),
    DUPLICATE(100067, "is duplicate", "重复"),
    OPERATION_FAIL_TRY_AGAIN(100068, "Operation failed, try again","操作失败，请重试"),
    PROTECTED_RESOURCE(100069, "Protected resource can not be deleted","系统保护资源，不允许删除"),
    TYPE_EXIST(100070,"Type exists","类型已存在"),
    TYPE_NOT_EMPTY(100070,"Type is not empty","此类别包含应用，不允许删除"),
    JSON_DECODE_ERROR(100071,"JSON string decode error","JSON解析失败"),
    LOGIN_FAIL(100072,"login fail","登录失败"),
    LOCK_TIMEOUT(100073,"lock timeout","获取锁超时"),
    FAILED_INITIALIZE(100074, "Failed to initialize", "根据license初始化资源失败"),
    LICENSE_PRIVILEDGE_ERROR(100075, "privilege error of license", "license获取权限异常"),
    LICENSE_DECODE_ERROR(100076, "privilege error of license", "license解密或类型转换失败"),
    LICENSE_NULL_ERROR(100077,"license is null","license为空，请更新license"),
    ENCODE_ERROR(100078,"encode error","编码失败"),
    DECODE_ERROR(100079,"decode error","解码失败"),
    // 资源相关
    CONFIGMAP_IS_EMPTY(400042, "ConfigMap is blank.", "配置文件内容为空"),

    //告警相关 500xxx
    UNSUPPORT_ALARM_TYPE(500001, "Alarm type is not support.", "不支持的告警类型"),
    ALARM_RECORD_NOT_EXIST(500002, "There is no available alarm record..", "不存在可处理的告警记录"),
    EMAIL_SEND_FAIL(500003, "Fail to send email.", "发送邮件失败"),
    USER_REAL_NAME_BLANK(500004, "user real name is blank.", "用户真实姓名为空"),
    MESSAGE_SEND_FAIL(500005, "Fail to send message.", "发送短信失败"),
    MESSAGE_TEMPLATE_ERROR(500006, "Message template error.", "短信模板数据异常"),
    RECEIVE_ALARM_EMAIL_ERROR(500007, "Receive alarm email error.", "接收告警邮件失败"),
    ALARM_TYPE_INCONSISTENT(500008, "Create alarm content error, alarm type is inconsistent.", "创建告警邮件内容失败，告警类型不一致"),
    CONFIG_EXISTS(500009, "config exists.", "配置项已经存在"),
    EMAIL_CONNECT_ERROR(500010, "email connect error.", "邮箱连接失败"),
    ALARM_RULE_EXIST(500011, "There is already a rule created for this deployment, please modify rule.", "该部署已经创建告警，请修改告警规则"),
    INFLUXDB_CONNECT_FAIL(500012, "Can not connect influxdb, initial failed.", "Influxdb 连接失败"),
    ALARM_RESOURCE_TYPE_NOT_SUPPORT(500013, "Not support resource monitor type.", "不支持的资源告警类型"),
    USAGE_RATE_DATA_FORMAT_ERROR(500014, "Data format error for usage rate.", "查询使用率，数据格式有误"),
    ES_QUERY_EXCEEDS_LIMIT(500015, "Only query in 300000 records, please limit your searching scope.", "仅能查看300000条内的记录，请缩小查询范围"),
    ES_INDEX_DELETED(500016, "Log is deleted at elasticsearch.", "Es日志已被删除"),
    GET_PROMETHEUS_ERROR(500017, "prometheus connection failed.", "获取prometheus连接失败"),

    //指标阈值相关 600xxx
    METRICS_THRESHOL_DUPLICATED(600001, "metrics threshold duplicated.", "阈值指标重复"),
    METRICS_THRESHOL_NULL(600002, "metrics threshold list can not be null.", "阈值指标不能为空"),
    CLUSTER_METRICS_NULL(600003, "cluster has no metrics", "集群还没有配置指标"),
    HEALTH_MONITOR_NOT_EXIST(600004, "health  monitor not exist.", "巡检监控不存在"),
    HEALTH_MONITOR_EMPTY(600005, "health  monitor is empty.", "巡检监控为空"),
    METRICS_THRESHOL_EXIST(600006, "metrics thredshold already exist.", "集群已存在指标阈值"),


    // prometheus相关 700xxx
    FAILED_GET_NODE(700001, "Failed to get node,Please check module", "获取集群主机失败！请联系管理员"),
    RESULT_DATA_EMPTY(700002, "Query result is empty", "系统错误！请联系管理员"),

    // 应用相关  800xxx
    // 1. trait相关
    NOT_SUPPORT_TRAIT_TYPE(800001, "Trait type not support", "没有提供支持的trait类型"),
    APPLICATION_IN_ROLLINGUPDATE(800002, "The applicaiton is in upgrade", "应用在升级中"),
    APPLICATION_BLUEGREENUPDATE_NO_SERVICE_TRAIT(800002,
        "There should be a service trait in application when bluegreen update", "蓝绿升级需要创建关联服务"),
    SERVICE_TEMPLATE_IMAGE_INFO_NOT_NULL(800003, "Image info not exist in service template.", "服务模板内不存在镜像信息"),

    //弹性伸缩 403xxx
    SERVICE_AUTOSCALE_CREATE_FAILURE(403001, "Create autoScale failure.", "自动伸缩创建失败"),
    SERVICE_AUTOSCALE_DELETE_FAILURE(403002, "Delete autoScale failure.", "自动伸缩删除失败"),
    AUTOSCALE_CONDITION_REQUIRE(403003, "can not create autoscale for TPS metric as application do not have create service.", "该应用未对外创建服务，不能根据TPS指标伸缩"),
    AUTOSCALE_NOT_SELECTED(403004, "Please select a metric for autoscale", "至少需要设置一项伸缩指标"),
    AUTOSCALE_METRIC_NOT_SUPPORT(403005, "not support for metric", "不支持的伸缩指标"),
    AUTOSCALE_TIME_MAX_MIN_ERROR(403006,"Max,min replicas value is error", "max ,min 值错误"),
    AUTOSCALE_TIME_ZONE_ERROR(403007,"Time zone value is error","负载均衡时间段错误"),
    SERVICE_AUTOSCALE_UPDATE_FAILURE(403008, "Create autoScale failure.", "自动伸缩升级失败"),
    AUTOSCALE_NOT_FOUND(403009, "Can not found autoscale.", "自动伸缩找寻不到"),
    SERVICE_AUTOSCALE_LABEL_UPDATE_FAILURE(403010, "Update autoscale label failure.", "更新自动伸缩标签失败"),
    AUTOSCALE_TIME_PODS_ERROR(403011,"Time based pods num should be greater than min pods and smaller than max pods.",
            "时间段实例数应大于或等于最小实例数，并小于或等于最大实例数"),

    // 网络 303
    EXTEND_IP_OVER_LIMIT(303026, "The workload can occupy {} IP at most. If you need to continue to expand, please contact the system administrator.", "该应用最大可占用{}个IP，如需继续扩容请联系系统管理员"),
    IP_NOT_UNENABLE(303027, "The ip is not unenabled", "ip非未启用状态"),
    IP_UNENABLE(303028, "The ip is unenabled", "ip为未启用状态"),
    IP_STOP_UNDER_LIMIT(303029,
            "To ensure business stability, the sum of the number of IPs in use and to be used needs to be ≥ 1.5 times the number of pods",
            "为保障业务稳定，使用中和待使用的IP数之和需保持≥实例数的1.5倍"),
    IP_AMOUNT_ILEGAL(303030, "ip amount is illegal", "IP地址数量不符合规范"),
    SUBNET_IP_AMOUNT_LACK(303031, "number of subnet IP is lack", "子网可用ip数量小于固定ip数量"),

    //egress 601
    NOT_NON_GATEWAY_CLUSTER(601001, "The cluster is not a non gateway cluster", "该集群不是非网关集群"),
    CIDR_FORMAT_ERROR(601002, "cidr format error", "cidr格式错误"),
    IP_FORMAT_ERROR(601003, "ip format error", "ip格式错误"),
    PORT_FORMAT_ERROR(601004, "port format error", "port格式错误"),
    MANAGER_OPERATE(601005, "Please operate with manager account", "请用管理员账号操作");

    private final int code;
    private final String enMsg;
    private final String zhMsg;

    CaasErrorMessage(int code, String enMsg, String zhMsg) {
        this.code = code;
        this.enMsg = enMsg;
        this.zhMsg = zhMsg;
    }

    public int getCode() {
        return code;
    }

    public String getMsg() {
        return zhMsg;
    }

    public String getEnMsg() {
        return enMsg;
    }

    public String getZhMsg() {
        return zhMsg;
    }
}