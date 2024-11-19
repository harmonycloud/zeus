package com.middleware.zeus.common.model.middleware;

import com.alibaba.fastjson.JSONObject;
import com.middleware.zeus.common.model.AffinityDTO;
import com.middleware.zeus.common.model.MonitorResourceQuota;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static com.middleware.zeus.common.constants.CommonConstant.LINE;

/**
 * @author dengyulong
 * @date 2021/03/23
 */
@NoArgsConstructor
@Accessors(chain = true)
@Data
@ApiModel("中间件信息")
public class Middleware implements Serializable {

    private static final long serialVersionUID = -517223197859990399L;

    @ApiModelProperty("集群id")
    private String clusterId;

    @ApiModelProperty("命名空间")
    private String namespace;

    @ApiModelProperty("命名空间别名")
    private String namespaceAliasName;

    @ApiModelProperty("中间件名称")
    private String name;

    @ApiModelProperty("中间件别称")
    private String aliasName;

    @ApiModelProperty("中间件类型：redis,mysql,elasticsearch,rocketmq等")
    private String type;

    @ApiModelProperty("中间件版本")
    private String version;

    @ApiModelProperty("节点亲和")
    private List<AffinityDTO> nodeAffinity;

    @ApiModelProperty("中间件模式")
    private String mode;

    @ApiModelProperty("中间件标签")
    private String labels;

    @ApiModelProperty("中间件备注")
    private String description;

    @ApiModelProperty("中间件注解")
    private String annotations;

    @ApiModelProperty("中间件密码")
    private String password;

    @ApiModelProperty("中间件配额，redis/mysql/rocketmq的key都是自身，elasticsearch的则是各个角色master/kibana/data/client")
    private Map<String, MiddlewareQuota> quota;

    @ApiModelProperty("中间件对外访问")
    private List<IngressDTO> ingresses;

    @ApiModelProperty("自动切换")
    private Boolean autoSwitch;

    @ApiModelProperty("自动切换")
    private Date lastAutoSwitchTime;

    @ApiModelProperty("中间件字符集")
    private String charSet;

    @ApiModelProperty("中间件语言环境")
    private String language;

    @ApiModelProperty("中间件端口")
    private Integer port;

    @ApiModelProperty("chart包名称")
    private String chartName;

    @ApiModelProperty("chart包版本")
    private String chartVersion;

    @ApiModelProperty("备份文件名称")
    private String backupFileName;

    @ApiModelProperty("业务数据库")
    private List<MysqlBusinessDeploy> businessDeploy;

    @ApiModelProperty("mysql环境变量")
    private List<MysqlEnviroment> environment;

    @ApiModelProperty("污点容忍")
    private List<String> tolerations;

    @ApiModelProperty("镜像仓库id")
    private String mirrorImageId;

    @ApiModelProperty("扩展调度器")
    private Boolean scheduler;

    /**
     * 以下字段仅在返回时使用
     */
    @ApiModelProperty("中间件状态")
    private String status;

    @ApiModelProperty("中间件异常原因")
    private String reason;

    @ApiModelProperty("中间件pod列表")
    private List<PodInfo> pods;

    @ApiModelProperty("创建时间")
    private Date createTime;

    @ApiModelProperty("动态字段")
    private Map<String, String> dynamicValues;

    @ApiModelProperty("动态tab栏")
    private List<String> capabilities;

    @ApiModelProperty("是否开启采集文件日志")
    private Boolean filelogEnabled;
    
    @ApiModelProperty("是否开启采集标准输出日志")
    private Boolean stdoutEnabled;

    @ApiModelProperty("是否开启审计日志采集")
    private Boolean audit;

    @ApiModelProperty("是否开启慢日志采集")
    private Boolean slowSql;

    @ApiModelProperty("rocketMQ独有字段")
    private RocketMQParam rocketMQParam;

    @ApiModelProperty("postgresql独有字段")
    private PostgresqlParam postgresqlParam;

    @ApiModelProperty("redis独有字段")
    private RedisParam redisParam;

    @ApiModelProperty("kafka专有信息")
    private KafkaDTO kafkaDTO;

    @ApiModelProperty("mysql专有信息")
    private MysqlDTO mysqlDTO;

    @ApiModelProperty("es独有字段")
    private EsParam esParam;

    @ApiModelProperty("中间件关联实例信息")
    private Middleware relationMiddleware;

    @ApiModelProperty("是否为备份克隆")
    private Boolean isBackup;

    @ApiModelProperty("中间件实例数量")
    private Integer podNum;

    @ApiModelProperty("是否有管理平台")
    private Boolean managePlatform;

    @ApiModelProperty("管理平台地址")
    private String managePlatformAddress;

    @ApiModelProperty("是否全部使用的lvm存储")
    private Boolean isAllLvmStorage;

    @ApiModelProperty("中间件pod list组")
    private PodInfoGroup podInfoGroup;

    @ApiModelProperty("是否已设置备份")
    private Boolean hasConfigBackup;

    @ApiModelProperty("是否删除备份相关信息")
    private Boolean deleteBackupInfo;

    @ApiModelProperty("非自定义中间件镜像仓库")
    private String mirrorImage;

    @ApiModelProperty("资源(源自prometheus)")
    private MonitorResourceQuota monitorResourceQuota;

    @ApiModelProperty("图片路径")
    private String imagePath;

    @ApiModelProperty("项目名称")
    private String projectName;

    @ApiModelProperty("读写分离")
    private ReadWriteProxy readWriteProxy;

    @ApiModelProperty("自定义目录信息")
    private Map<String, CustomVolume> customVolumes;

    @ApiModelProperty("存储类型信息(即：storageClass)")
    private List<MiddlewareQuota> storageResource;

    @ApiModelProperty("容器uid")
    private Long containerUID;

    @ApiModelProperty("容器gid")
    private Long containerGID;

    @ApiModelProperty("部署类型 container:容器 server:虚拟机/服务器")
    private String deployMod;

    @ApiModelProperty("values.yaml")
    private JSONObject values;

    public Middleware(String clusterId, String namespace, String name, String type) {
        this.clusterId = clusterId;
        this.namespace = namespace;
        this.name = name;
        this.type = type;
    }
    
    public String toStringKey() {
        StringBuilder sb = new StringBuilder();
        if (StringUtils.isNotEmpty(this.getClusterId())) {
            sb.append(this.getClusterId()).append(LINE);
        }
        if (StringUtils.isNotEmpty(this.getNamespace())) {
            sb.append(this.getNamespace()).append(LINE);
        }
        if (StringUtils.isNotEmpty(this.getName())) {
            sb.append(this.getName()).append(LINE);
        }
        if (StringUtils.isNotEmpty(this.getType())) {
            sb.append(this.getType());
        }
        return sb.toString();
    }

}
