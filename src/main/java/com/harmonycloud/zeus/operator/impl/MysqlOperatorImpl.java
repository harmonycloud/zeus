package com.harmonycloud.zeus.operator.impl;

import static com.harmonycloud.caas.common.constants.NameConstant.*;
import static com.harmonycloud.caas.common.constants.middleware.MiddlewareConstant.*;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import com.alibaba.fastjson.JSON;
import com.harmonycloud.caas.common.enums.Protocol;
import com.harmonycloud.caas.common.model.AffinityDTO;
import com.harmonycloud.caas.common.model.IngressComponentDto;
import com.harmonycloud.caas.common.model.MiddlewareServiceNameIndex;
import com.harmonycloud.tool.numeric.ResourceCalculationUtil;
import com.harmonycloud.zeus.bean.BeanCacheMiddleware;
import com.harmonycloud.zeus.bean.BeanMysqlUser;
import com.harmonycloud.zeus.service.k8s.*;
import com.harmonycloud.zeus.service.middleware.ImageRepositoryService;
import com.harmonycloud.zeus.service.mysql.MysqlDbPrivService;
import com.harmonycloud.zeus.service.mysql.MysqlDbService;
import com.harmonycloud.zeus.service.mysql.MysqlUserService;
import com.harmonycloud.zeus.service.system.LicenseService;
import com.harmonycloud.zeus.util.K8sConvert;
import com.harmonycloud.zeus.util.MysqlConnectionUtil;
import io.fabric8.kubernetes.api.model.NodeAffinity;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.harmonycloud.caas.common.constants.MysqlConstant;
import com.harmonycloud.caas.common.constants.NameConstant;
import com.harmonycloud.caas.common.enums.DateType;
import com.harmonycloud.caas.common.enums.DictEnum;
import com.harmonycloud.caas.common.enums.ErrorMessage;
import com.harmonycloud.caas.common.enums.middleware.MiddlewareTypeEnum;
import com.harmonycloud.caas.common.exception.BusinessException;
import com.harmonycloud.caas.common.model.middleware.*;
import com.harmonycloud.tool.date.DateUtils;
import com.harmonycloud.tool.encrypt.PasswordUtils;
import com.harmonycloud.zeus.annotation.Operator;
import com.harmonycloud.zeus.integration.cluster.MysqlClusterWrapper;
import com.harmonycloud.zeus.integration.cluster.bean.*;
import com.harmonycloud.zeus.operator.BaseOperator;
import com.harmonycloud.zeus.operator.api.MysqlOperator;
import com.harmonycloud.zeus.operator.miiddleware.AbstractMysqlOperator;
import com.harmonycloud.zeus.service.middleware.BackupService;
import com.harmonycloud.zeus.service.middleware.MysqlScheduleBackupService;
import com.harmonycloud.zeus.service.middleware.impl.MiddlewareServiceImpl;
import com.harmonycloud.zeus.service.middleware.impl.MysqlBackupServiceImpl;
import com.harmonycloud.zeus.util.DateUtil;
import com.harmonycloud.zeus.util.ServiceNameConvertUtil;

import cn.hutool.json.JSONUtil;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import lombok.extern.slf4j.Slf4j;

/**
 * @author dengyulong
 * @date 2021/03/23
 * 处理mysql逻辑
 */
@Slf4j
@Operator(paramTypes4One = Middleware.class)
public class MysqlOperatorImpl extends AbstractMysqlOperator implements MysqlOperator {

    @Autowired
    private MysqlClusterWrapper mysqlClusterWrapper;
    @Autowired
    private BackupService backupService;
    @Autowired
    private MysqlScheduleBackupService mysqlScheduleBackupService;
    @Autowired
    private ImageRepositoryService imageRepositoryService;
    @Autowired
    private ClusterService clusterService;
    @Autowired
    private MysqlReplicateCRDService mysqlReplicateCRDService;
    @Autowired
    private MiddlewareServiceImpl middlewareService;
    @Autowired
    private BaseOperatorImpl baseOperator;
    @Autowired
    private MysqlBackupServiceImpl mysqlBackupService;
    @Autowired
    private IngressComponentService ingressComponentService;
    @Autowired
    private ServiceService serviceService;
    @Autowired
    private MysqlDbService mysqlDbService;
    @Autowired
    private MysqlUserService mysqlUserService;
    @Autowired
    private MysqlDbPrivService mysqlDbPrivService;
    @Autowired
    private IngressService ingressService;
    @Autowired
    private NamespaceService namespaceService;


    @Override
    public boolean support(Middleware middleware) {
        return MiddlewareTypeEnum.MYSQL == MiddlewareTypeEnum.findByType(middleware.getType());
    }

    @Override
    public void replaceValues(Middleware middleware, MiddlewareClusterDTO cluster, JSONObject values) {
        // 替换通用的值
        replaceCommonValues(middleware, cluster, values);
        MiddlewareQuota quota = middleware.getQuota().get(middleware.getType());
        // 如果是克隆，则增加3Gi存储空间，否则集群起不来
        if (StringUtils.isNotBlank(middleware.getBackupFileName())) {
            int storageSize = Integer.parseInt(quota.getStorageClassQuota()) + 3;
            quota.setStorageClassQuota(String.valueOf(storageSize));
        }
        replaceCommonResources(quota, values.getJSONObject(RESOURCES));
        replaceCommonStorages(quota, values);

        //添加业务数据库
        if (middleware.getBusinessDeploy() != null && !middleware.getBusinessDeploy().isEmpty()) {
            JSONArray array = values.getJSONArray("businessDeploy");
            middleware.getBusinessDeploy().forEach(mysqlBusinessDeploy -> array.add(JSONUtil.parse(mysqlBusinessDeploy)));
        }

        // mysql参数
        JSONObject mysqlArgs = values.getJSONObject("args");
        JSONObject features = values.getJSONObject("features");
        if (StringUtils.isBlank(middleware.getPassword())) {
            middleware.setPassword(PasswordUtils.generateCommonPassword(10));
        }
        log.info("mysql特有参数：{}", mysqlArgs);
        mysqlArgs.put("root_password", middleware.getPassword());
        if (StringUtils.isNotBlank(middleware.getCharSet())) {
            mysqlArgs.put("character_set_server", middleware.getCharSet());
        }
        if (middleware.getPort() != null) {
            mysqlArgs.put("server_port", middleware.getPort());
        }
        if (middleware.getMysqlDTO() != null) {
            MysqlDTO mysqlDTO = middleware.getMysqlDTO();
            if (mysqlDTO.getReplicaCount() != null) {
                int replicaCount = mysqlDTO.getReplicaCount();
                values.put(MysqlConstant.REPLICA_COUNT, replicaCount + 1);
            }
            if (mysqlDTO.getOpenDisasterRecoveryMode() != null && mysqlDTO.getOpenDisasterRecoveryMode()) {
                mysqlArgs.put(MysqlConstant.IS_SOURCE, mysqlDTO.getIsSource());
                mysqlArgs.put(MysqlConstant.RELATION_CLUSTER_ID, mysqlDTO.getRelationClusterId());
                mysqlArgs.put(MysqlConstant.RELATION_NAMESPACE, mysqlDTO.getRelationNamespace());
                mysqlArgs.put(MysqlConstant.RELATION_NAME, mysqlDTO.getRelationName());
                mysqlArgs.put(MysqlConstant.RELATION_ALIAS_NAME, mysqlDTO.getRelationAliasName());
                mysqlArgs.put(MysqlConstant.CHART_NAME, middleware.getChartName());
            }
            if (StringUtils.isNotBlank(mysqlDTO.getType())) {
                values.put(MysqlConstant.SPEC_TYPE, mysqlDTO.getType());
            }
            if (StringUtils.isNotBlank(middleware.getVersion()) && !("8.0".equals(middleware.getVersion()))) {
                //设置SQL审计开关
                checkAndSetAuditSqlStatus(features, mysqlDTO);
            }
        }
        //配置mysql环境变量
        if (!CollectionUtils.isEmpty(middleware.getEnvironment())) {
            middleware.getEnvironment().forEach(mysqlEnviroment -> mysqlArgs.put(mysqlEnviroment.getName(), mysqlEnviroment.getValue()));
        }
        // 备份恢复的创建
        if (StringUtils.isNotEmpty(middleware.getBackupFileName())) {
            BackupStorageProvider backupStorageProvider = backupService.getStorageProvider(middleware);
            values.put("storageProvider", JSONObject.toJSON(backupStorageProvider));
        }
        // 添加双活配置
        checkAndSetActiveActive(values, middleware);
    }

    @Override
    public Middleware convertByHelmChart(Middleware middleware, MiddlewareClusterDTO cluster) {
        JSONObject values = helmChartService.getInstalledValues(middleware, cluster);
        convertCommonByHelmChart(middleware, values);
        convertStoragesByHelmChart(middleware, middleware.getType(), values);
        convertRegistry(middleware, cluster);
        // 处理mysql的特有参数
        if (values != null) {
            convertResourcesByHelmChart(middleware, middleware.getType(), values.getJSONObject(RESOURCES));
            JSONObject args = values.getJSONObject("args");
            if (args == null) {
                args = values.getJSONObject("mysqlArgs");
            }
            if (checkUserAuthority(MiddlewareTypeEnum.MYSQL.getType())) {
                middleware.setPassword(args.getString("root_password"));
            }
            middleware.setCharSet(args.getString("character_set_server"));
            middleware.setPort(args.getIntValue("server_port"));

            MysqlDTO mysqlDTO = new MysqlDTO();
            mysqlDTO.setReplicaCount(args.getIntValue(MysqlConstant.REPLICA_COUNT));
            // 设置是否允许备份
            mysqlDTO.setIsLvmStorage(true);
            middleware.setIsAllLvmStorage(true);
            middleware.setMysqlDTO(mysqlDTO);
            // 获取关联实例信息
            Boolean isSource = args.getBoolean(MysqlConstant.IS_SOURCE);
            if (isSource != null) {
                mysqlDTO.setOpenDisasterRecoveryMode(true);
                mysqlDTO.setIsSource(isSource);
                mysqlDTO.setReplicaCount(args.getIntValue(MysqlConstant.REPLICA_COUNT));
                //获取关联实例信息
                String relationClusterId = args.getString(MysqlConstant.RELATION_CLUSTER_ID);
                String relationNamespace = args.getString(MysqlConstant.RELATION_NAMESPACE);
                String relationName = args.getString(MysqlConstant.RELATION_NAME);
                String relationAliasName = args.getString(MysqlConstant.RELATION_ALIAS_NAME);
                String chartName = args.getString(MysqlConstant.CHART_NAME);
                mysqlDTO.setRelationClusterId(relationClusterId);
                mysqlDTO.setRelationNamespace(relationNamespace);
                mysqlDTO.setRelationName(relationName);
                mysqlDTO.setRelationAliasName(relationAliasName);
                mysqlDTO.setRelationExist(baseOperator.checkIfExist(relationNamespace, relationName, clusterService.findById(relationClusterId)));
                middleware.setChartName(chartName);

                MysqlReplicateCR mysqlReplicate;
                if (isSource) {
                    mysqlReplicate = mysqlReplicateCRDService.getMysqlReplicate(relationClusterId, relationNamespace, relationName);
                } else {
                    mysqlReplicate = mysqlReplicateCRDService.getMysqlReplicate(cluster.getId(), middleware.getNamespace(), middleware.getName());
                }
                if (mysqlReplicate != null && mysqlReplicate.getStatus() != null) {
                    mysqlDTO.setPhase(mysqlReplicate.getStatus().getPhase());
                    mysqlDTO.setCanSwitch(mysqlReplicate.getSpec().isEnable());
                    List<MysqlReplicateStatus.PodStatus> podStatuses = mysqlReplicate.getStatus().getSlaves();
                    if (!CollectionUtils.isEmpty(podStatuses)) {
                        MysqlReplicateStatus.PodStatus podStatus = podStatuses.get(0);
                        String lastUpdateTime = podStatus.getLastUpdateTime();
                        mysqlDTO.setLastUpdateTime(DateUtil.utc2Local(lastUpdateTime, DateType.YYYY_MM_DD_HH_MM_SS.getValue(), DateType.YYYY_MM_DD_HH_MM_SS.getValue()));
                    }
                }
            }
            // 是否自动切换
            MysqlCluster mysqlCluster = mysqlClusterWrapper.get(middleware.getClusterId(), middleware.getNamespace(), middleware.getName());
            middleware.setAutoSwitch(mysqlCluster.getSpec().getPassiveSwitched() == null || !mysqlCluster.getSpec().getPassiveSwitched());
            if (mysqlCluster.getStatus() != null && mysqlCluster.getStatus().getLastChangeMaster() != null){
                middleware.setLastAutoSwitchTime(DateUtils.parseUTCDate(mysqlCluster.getStatus().getLastChangeMaster()));
            }
            // 读写分离
            if (values.containsKey("proxy")){
                ReadWriteProxy readWriteProxy = new ReadWriteProxy();
                readWriteProxy.setEnabled(values.getJSONObject("proxy").getBoolean("enable"));
                middleware.setReadWriteProxy(readWriteProxy);
            }
        }
        return middleware;
    }

    @Override
    public void create(Middleware middleware, MiddlewareClusterDTO cluster) {
        super.create(middleware, cluster);
        MysqlDTO mysqlDTO = middleware.getMysqlDTO();
        if (mysqlDTO.getOpenDisasterRecoveryMode() != null && mysqlDTO.getOpenDisasterRecoveryMode() && mysqlDTO.getIsSource()) {
            //String jsonStr = JSON.toJSONString(middleware);
            //Middleware relationMiddleware = JSON.parseObject(jsonStr, Middleware.class);
            //middleware.setRelationMiddleware(relationMiddleware);
            middlewareManageTask.asyncCreateDisasterRecoveryMiddleware(this, middleware);
        }
        // 创建对外服务
        prepareDbManageOpenService(middleware);
        // 准备数据库管理环境
        prepareDbManageEnv(middleware);
    }

    @Override
    public void update(Middleware middleware, MiddlewareClusterDTO cluster) {
        if (cluster == null) {
            cluster = clusterService.findById(middleware.getClusterId());
        }
        StringBuilder sb = new StringBuilder();

        // 实例扩容
        if (middleware.getQuota() != null && middleware.getQuota().get(middleware.getType()) != null) {
            MiddlewareQuota quota = middleware.getQuota().get(middleware.getType());
            // 设置limit的resources
            setLimitResources(quota);
            if (StringUtils.isNotBlank(quota.getCpu())) {
                sb.append("resources.requests.cpu=").append(quota.getCpu()).append(",resources.limits.cpu=")
                        .append(quota.getLimitCpu()).append(",");
            }
            if (StringUtils.isNotBlank(quota.getMemory())) {
                sb.append("resources.requests.memory=").append(quota.getMemory()).append(",resources.limits.memory=")
                        .append(quota.getLimitMemory()).append(",");
            }
        }

        // 修改密码
        if (StringUtils.isNotBlank(middleware.getPassword())) {
            sb.append("args.root_password=").append(middleware.getPassword()).append(",");
        }

        // 修改关联实例信息
        MysqlDTO mysqlDTO = middleware.getMysqlDTO();
        if (mysqlDTO != null && mysqlDTO.getOpenDisasterRecoveryMode() != null) {
            sb.append(String.format("%s.%s=%s,", MysqlConstant.ARGS, MysqlConstant.IS_SOURCE, mysqlDTO.getIsSource()));
            sb.append(String.format("%s.%s=%s,", MysqlConstant.ARGS, MysqlConstant.RELATION_CLUSTER_ID, mysqlDTO.getRelationClusterId()));
            sb.append(String.format("%s.%s=%s,", MysqlConstant.ARGS, MysqlConstant.RELATION_NAMESPACE, mysqlDTO.getRelationNamespace()));
            sb.append(String.format("%s.%s=%s,", MysqlConstant.ARGS, MysqlConstant.RELATION_NAME, mysqlDTO.getRelationName()));
            sb.append(String.format("%s.%s=%s,", MysqlConstant.ARGS, MysqlConstant.RELATION_ALIAS_NAME, mysqlDTO.getRelationAliasName()));
        }

        if (mysqlDTO != null && mysqlDTO.getType() != null) {
            sb.append(String.format("%s=%s,", MysqlConstant.SPEC_TYPE, mysqlDTO.getType()));
        }

        if (mysqlDTO != null && mysqlDTO.getType() != null) {
            sb.append(String.format("%s=%s,", MysqlConstant.SPEC_TYPE, mysqlDTO.getType()));
        }

        // 更新通用字段
        super.updateCommonValues(sb, middleware);

        // 没有修改，直接返回
        if (sb.length() == 0) {
            return;
        }
        // 去掉末尾的逗号
        sb.deleteCharAt(sb.length() - 1);
        // 更新helm
        helmChartService.upgrade(middleware, sb.toString(), cluster);
        if (mysqlDTO != null && mysqlDTO.getOpenDisasterRecoveryMode() != null && mysqlDTO.getOpenDisasterRecoveryMode() && mysqlDTO.getIsSource()) {
            Middleware disasterRecoverMiddleware = middleware.getRelationMiddleware();
            disasterRecoverMiddleware.setChartName(middleware.getChartName());
            disasterRecoverMiddleware.setChartVersion(middleware.getChartVersion());
            this.createDisasterRecoveryMiddleware(middleware);
        }
    }

    @Override
    public void prepareDbManageOpenService(Middleware middleware){
        middlewareManageTask.asyncCreateMysqlOpenService(this, middleware);
    }

    /**
     * 检查并设置mysql SQL审计采集开关，若支持SQL审计，则默认设置为开
     * @param features
     * @param mysqlDTO
     */
    private void checkAndSetAuditSqlStatus(JSONObject features, MysqlDTO mysqlDTO) {
        if (features == null) {
            return;
        }
        if (features.getJSONObject(MysqlConstant.KEY_FEATURES_AUDITLOG) != null) {
            if (mysqlDTO.getAuditSqlEnabled() != null) {
                features.getJSONObject(MysqlConstant.KEY_FEATURES_AUDITLOG).put("enabled", mysqlDTO.getAuditSqlEnabled());
            } else {
                features.getJSONObject(MysqlConstant.KEY_FEATURES_AUDITLOG).put("enabled", true);
            }
        }
    }

    /**
     * 检查是否是双活分区并设置双活配置字段
     * @param values
     * @param middleware
     */
    @Override
    public void checkAndSetActiveActive(JSONObject values, Middleware middleware) {
        if (namespaceService.checkAvailableDomain(middleware.getClusterId(), middleware.getNamespace())) {
            super.setActiveActiveConfig(null, values);
            super.setActiveActiveToleration(middleware, values);
        }
    }

    @Override
    public void deleteStorage(Middleware middleware) {
        this.deleteDisasterRecoveryInfo(middleware);
        super.deleteStorage(middleware);
        clearDbManageData(middleware);
        if (middleware.getDeleteBackupInfo() == null || middleware.getDeleteBackupInfo()) {
            // 删除备份相关
            mysqlBackupService.deleteMiddlewareBackupInfo(middleware.getClusterId(), middleware.getNamespace(), middleware.getType(), middleware.getName());
            // 删除定时备份任务
            mysqlScheduleBackupService.delete(middleware.getClusterId(), middleware.getNamespace(), middleware.getName());
        }
    }

    @Override
    public void switchMiddleware(Middleware middleware) {
        MysqlCluster mysqlCluster = mysqlClusterWrapper.get(middleware.getClusterId(), middleware.getNamespace(), middleware.getName());
        if (mysqlCluster == null) {
            throw new BusinessException(DictEnum.MYSQL_CLUSTER, middleware.getName(), ErrorMessage.NOT_EXIST);
        }
        if (!NameConstant.RUNNING.equalsIgnoreCase(mysqlCluster.getStatus().getPhase())) {
            throw new BusinessException(ErrorMessage.MIDDLEWARE_CLUSTER_IS_NOT_RUNNING);
        }
        // 手动切换
        if (handSwitch(middleware, mysqlCluster)) {
            return;
        }
        // 自动切换
        autoSwitch(middleware, mysqlCluster);

    }

    @Override
    public List<String> getConfigmapDataList(ConfigMap configMap) {
        return new ArrayList<>(Arrays.asList(configMap.getData().get("my.cnf.tmpl").split("\n")));
    }

    /**
     * 构建新configmap
     */
    @Override
    public Map<String, String> configMap2Data(ConfigMap configMap) {
        String dataString = configMap.getData().get("my.cnf.tmpl");
        Map<String, String> dataMap = new HashMap<>();
        String[] datalist = dataString.split("\n");
        for (String data : datalist) {
            if (!data.contains("=") || data.contains("#")) {
                continue;
            }
            data = data.replaceAll(" ", "");
            // 特殊处理
            if (data.contains("plugin-load")) {
                dataMap.put("plugin-load", data.replace("plugin-load=", ""));
                continue;
            }
            String[] keyValue = data.split("=");
            dataMap.put(keyValue[0].replaceAll(" ", ""), keyValue[1]);
        }
        return dataMap;
    }

    @Override
    public void editConfigMapData(CustomConfig customConfig, List<String> data) {
        for (int i = 0; i < data.size(); ++i) {
            if (data.get(i).contains(customConfig.getName())) {
                String temp = StringUtils.substring(data.get(i), data.get(i).indexOf("=") + 1, data.get(i).length());
                if (data.get(i).replace(" ", "").replace(temp, "").replace("=", "").equals(customConfig.getName())) {
                    data.set(i, data.get(i).replace(temp, customConfig.getValue()));
                }
            }
        }
    }

    /**
     * 转换data为map形式
     */
    @Override
    public void updateConfigData(ConfigMap configMap, List<String> data) {
        // 构造新configmap
        StringBuilder temp = new StringBuilder();
        for (String str : data) {
            temp.append(str).append("\n");
        }
        configMap.getData().put("my.cnf.tmpl", temp.toString());
    }

    /**
     * 手动切换
     */
    private boolean handSwitch(Middleware middleware, MysqlCluster mysqlCluster) {
        // 不等于null，自动切换，无需处理
        if (middleware.getAutoSwitch() != null) {
            // false为无需切换，true为已切换
            return false;
        }
        String masterName = null;
        String slaveName = null;
        for (Status.Condition cond : mysqlCluster.getStatus().getConditions()) {
            if ("master".equalsIgnoreCase(cond.getType())) {
                masterName = cond.getName();
            } else if ("slave".equalsIgnoreCase(cond.getType())) {
                slaveName = cond.getName();
            }
        }
        if (masterName == null || slaveName == null) {
            throw new BusinessException(ErrorMessage.MIDDLEWARE_CLUSTER_POD_ERROR);
        }
        mysqlCluster.getSpec().getClusterSwitch().setFinished(false).setSwitched(false).setMaster(slaveName);
        try {
            mysqlClusterWrapper.update(middleware.getClusterId(), middleware.getNamespace(), mysqlCluster);
        } catch (Exception e) {
            log.error("集群id:{}，命名空间:{}，mysql集群:{}，手动切换异常", middleware.getClusterId(), middleware.getNamespace(),
                    middleware.getName(), e);
            throw new BusinessException(DictEnum.MYSQL_CLUSTER, middleware.getName(), ErrorMessage.SWITCH_FAILED);
        }
        return true;
    }

    /**
     * 自动切换
     */
    private void autoSwitch(Middleware middleware, MysqlCluster mysqlCluster) {
        boolean changeStatus = false;
        if (mysqlCluster.getSpec().getPassiveSwitched() == null) {
            if (!middleware.getAutoSwitch()) {
                changeStatus = true;
                mysqlCluster.getSpec().setPassiveSwitched(true);
            }
        } else if (mysqlCluster.getSpec().getPassiveSwitched().equals(middleware.getAutoSwitch())) {
            changeStatus = true;
            mysqlCluster.getSpec().setPassiveSwitched(!middleware.getAutoSwitch());
        }
        if (changeStatus) {
            try {
                mysqlClusterWrapper.update(middleware.getClusterId(), middleware.getNamespace(), mysqlCluster);
            } catch (IOException e) {
                log.error("集群id:{}，命名空间:{}，mysql集群:{}，开启/关闭自动切换异常", middleware.getClusterId(),
                        middleware.getNamespace(), middleware.getName(), e);
                throw new BusinessException(DictEnum.MYSQL_CLUSTER, middleware.getName(), ErrorMessage.SWITCH_FAILED);
            }
        }
    }

    /**
     * 计算下次备份时间
     */
    public Date calculateNextDate(ScheduleBackup scheduleBackup) {
        try {
            String[] cron = scheduleBackup.getSchedule().split(" ");
            String[] cronWeek = cron[4].split(",");
            List<Date> dateList = new ArrayList<>();
            for (String dayOfWeek : cronWeek) {
                Calendar cal = Calendar.getInstance();
                cal.set(Calendar.MINUTE, Integer.parseInt(cron[0]));
                cal.set(Calendar.HOUR_OF_DAY, Integer.parseInt(cron[1]));
                cal.set(Calendar.DAY_OF_WEEK, Integer.parseInt(dayOfWeek) + 1);
                cal.set(Calendar.SECOND, 0);
                cal.set(Calendar.MILLISECOND, 0);
                Date date = cal.getTime();
                dateList.add(date);
            }
            dateList.sort((d1, d2) -> {
                if (d1.equals(d2)) {
                    return 0;
                }
                return d1.before(d2) ? -1 : 1;
            });
            Date now = new Date();
            for (Date date : dateList) {
                if (now.before(date)) {
                    return date;
                }
            }
            return DateUtils.addInteger(dateList.get(0), Calendar.DATE, 7);
        } catch (Exception e) {
            log.error("定时备份{} ,计算下次备份时间失败", scheduleBackup.getName());
            return null;
        }
    }

    @Override
    public void switchDisasterRecovery(String clusterId, String namespace, String middlewareName) throws Exception {
        Middleware middleware = middlewareService.detail(clusterId, namespace, middlewareName, MiddlewareTypeEnum.MYSQL.getType());
        middleware.setClusterId(clusterId);
        middleware.setChartName(MiddlewareTypeEnum.MYSQL.getType());
        MysqlDTO mysqlDTO = middleware.getMysqlDTO();
        if (mysqlDTO != null) {
            Boolean isSource = mysqlDTO.getIsSource();
            if (isSource != null) {
                String relationClusterId = mysqlDTO.getRelationClusterId();
                String relationNamespace = mysqlDTO.getRelationNamespace();
                String relationName = mysqlDTO.getRelationName();

                //获取mysql复制关系，关闭复制关系
                MysqlReplicateCR mysqlReplicate;
                if (isSource) {
                    mysqlReplicate = mysqlReplicateCRDService.getMysqlReplicate(relationClusterId, relationNamespace, relationName);
                } else {
                    mysqlReplicate = mysqlReplicateCRDService.getMysqlReplicate(clusterId, namespace, middlewareName);
                }
                if (mysqlReplicate != null) {
                    log.info("开始删除灾备复制,clusterId={}, namespace={}, middlewareName={}", clusterId, namespace, middlewareName);
                    mysqlReplicateCRDService.deleteMysqlReplicate(clusterId, namespace, mysqlReplicate.getMetadata().getName());
                    log.info("成功删除灾备复制");
                } else {
                    log.info("该实例不存在灾备实例");
                }

                try {
                    MiddlewareClusterDTO middlewareClusterDTO = clusterService.findById(clusterId);
                    mysqlDTO.setIsSource(null);
                    mysqlDTO.setOpenDisasterRecoveryMode(false);
                    mysqlDTO.setType("master-slave");
                    update(middleware, middlewareClusterDTO);
                } catch (Exception e) {
                    log.error("实例信息更新失败", e);
                }

                try {
                    Middleware disasterRecovery = middlewareService.detail(relationClusterId, relationNamespace, relationName, MiddlewareTypeEnum.MYSQL.getType());
                    disasterRecovery.setChartName(MiddlewareTypeEnum.MYSQL.getType());
                    disasterRecovery.setClusterId(relationClusterId);
                    MysqlDTO disasterRecoveryMysqlDTO = disasterRecovery.getMysqlDTO();
                    disasterRecoveryMysqlDTO.setIsSource(null);
                    disasterRecoveryMysqlDTO.setOpenDisasterRecoveryMode(false);
                    disasterRecoveryMysqlDTO.setType("master-slave");
                    MiddlewareClusterDTO disasterRecoveryMiddlewareClusterDTO = clusterService.findById(relationClusterId);
                    update(disasterRecovery, disasterRecoveryMiddlewareClusterDTO);
                } catch (Exception e) {
                    log.error("实例信息更新失败", e);
                }
            }
        }
    }

    @Override
    public void replaceReadWriteProxyValues(Middleware middleware, JSONObject values){

        ReadWriteProxy readWriteProxy = middleware.getReadWriteProxy();
        JSONObject proxy = new JSONObject();;
        proxy.put("enable", readWriteProxy.getEnabled());
        proxy.put("podAntiAffinity", "soft");
        // 获取proxy节点数
        MysqlDTO mysqlDTO = middleware.getMysqlDTO();
        int replicaCount = mysqlDTO.getReplicaCount();
        proxy.put("replicaCount", replicaCount + 1);

        JSONObject requests = new JSONObject();
        JSONObject limits = new JSONObject();

        MiddlewareQuota quota = middleware.getQuota().get(middleware.getType());
        String cpu = calculateProxyResource(quota.getCpu());
        String memory = calculateProxyResource(quota.getMemory().replace("Gi", ""));
        if (Double.parseDouble(memory) < 0.256){
            memory = String.valueOf(0.256);
        }
        requests.put(CPU, cpu);
        requests.put(MEMORY, memory + "Gi");
        limits.put(CPU, cpu);
        limits.put(MEMORY, memory + "Gi");

        JSONObject resources = new JSONObject();
        resources.put("requests", requests);
        resources.put("limits", limits);

        proxy.put("resources", resources);
        values.put("proxy", proxy);
    }

    @Override
    public void createDisasterRecoveryMiddleware(Middleware middleware) {
        if (licenseService.check(middleware.getClusterId())){
            throw new BusinessException(ErrorMessage.LICENSE_CPU_RESOURCE_NOT_ENOUGH);
        }
        MysqlDTO mysqlDTO = middleware.getMysqlDTO();
        //1.为实例创建只读对外服务(NodePort)
        createOpenService(middleware, true, true);
        //2.设置灾备实例信息，创建灾备实例
        //2.1 设置灾备实例信息
        Middleware relationMiddleware = middleware.getRelationMiddleware();
        relationMiddleware.setClusterId(mysqlDTO.getRelationClusterId());
        relationMiddleware.setNamespace(mysqlDTO.getRelationNamespace());
        relationMiddleware.setName(mysqlDTO.getRelationName());
        relationMiddleware.setAliasName(mysqlDTO.getRelationAliasName());

        //2.2 给灾备实例设置源实例信息
        MysqlDTO sourceDto = new MysqlDTO();
        sourceDto.setRelationClusterId(middleware.getClusterId());
        sourceDto.setRelationNamespace(middleware.getNamespace());
        sourceDto.setRelationName(middleware.getName());
        sourceDto.setRelationAliasName(middleware.getAliasName());
        sourceDto.setReplicaCount(middleware.getMysqlDTO().getReplicaCount());
        sourceDto.setOpenDisasterRecoveryMode(true);
        sourceDto.setIsSource(false);
        sourceDto.setType("slave-slave");
        relationMiddleware.setMysqlDTO(sourceDto);

         //3 修改灾备实例镜像仓库
        MiddlewareClusterDTO cluster = clusterService.findByIdAndCheckRegistry(relationMiddleware.getClusterId());
        if (StringUtils.isNotEmpty(middleware.getMirrorImageId())) {
            cluster.setRegistry(imageRepositoryService.generateRegistry(middleware.getMirrorImageId()));
        }

        BaseOperator operator = middlewareService.getOperator(BaseOperator.class, BaseOperator.class, relationMiddleware);
        operator.createPreCheck(relationMiddleware, cluster);
        this.create(relationMiddleware, cluster);
        //3.异步创建关联关系
        this.createMysqlReplicate(middleware, relationMiddleware);

    }

    /**
     * 创建源实例和灾备实例的关联关系
     *
     * @param original
     */
    @Override
    public void createMysqlReplicate(Middleware original, Middleware disasterRecovery) {
        Middleware middleware = middlewareService.detail(original.getClusterId(), original.getNamespace(), original.getName(), original.getType());
        List<IngressDTO> ingressDTOS = ingressService.get(original.getClusterId(), middleware.getNamespace(),
                middleware.getType(), middleware.getName());
        log.info("准备创建MysqlReplicate,middleware={},ingressDTOS={}", middleware, ingressDTOS);
        if (!CollectionUtils.isEmpty(ingressDTOS)) {
            // 查询只读服务
            List<IngressDTO> readonlyIngressDTOList = ingressDTOS.stream().filter(item -> item.getName().contains("readonly"))
                            .collect(Collectors.toList());

            if (!CollectionUtils.isEmpty(readonlyIngressDTOList)) {
                IngressDTO ingressDTO = readonlyIngressDTOList.get(0);
                List<ServiceDTO> serviceList = ingressDTO.getServiceList();
                if (!CollectionUtils.isEmpty(serviceList)) {
                    ServiceDTO serviceDTO = serviceList.get(0);
                    MysqlReplicateSpec spec = new MysqlReplicateSpec(true, disasterRecovery.getName(),
                            ingressDTO.getExposeIP(), Integer.parseInt(serviceDTO.getExposePort()), "root", middleware.getPassword());

                    MysqlReplicateCR mysqlReplicateCR = new MysqlReplicateCR();
                    ObjectMeta metaData = new ObjectMeta();
                    metaData.setName(disasterRecovery.getName());
                    metaData.setNamespace(disasterRecovery.getNamespace());
                    Map<String, String> labels = new HashMap<>();
                    labels.put("operatorname", "mysql-operator");
                    metaData.setLabels(labels);

                    mysqlReplicateCR.setSpec(spec);
                    mysqlReplicateCR.setMetadata(metaData);
                    mysqlReplicateCR.setKind("MysqlReplicate");

                    // 先删除mysqlreplicate
                    try {
                        mysqlReplicateCRDService.deleteMysqlReplicate(disasterRecovery.getClusterId(), disasterRecovery.getNamespace(), mysqlReplicateCR.getMetadata().getName());
                    } catch (Exception e) {
                        log.error("删除mysqlreplicate出错了", e);
                    }

                    try {
                        log.info("创建mysql实例 {} 和 {} 的关联关系MysqlReplicate", original.getName(), middleware.getName());
                        mysqlReplicateCRDService.createMysqlReplicate(disasterRecovery.getClusterId(), mysqlReplicateCR);
                        log.info("MysqlReplicate创建成功");
                    } catch (IOException e) {
                        log.error("MysqlReplicate创建失败", e);
                        e.printStackTrace();
                    }
                }
            }
        } else {
            log.info("未找到只读服务，无法创建MysqlReplicate");
        }
    }

    public void createOpenService(Middleware middleware, boolean isReadOnlyService, boolean useNodePort) {
        log.info("为实例：{} 创建对外服务");
        executeCreateOpenService(middleware, isReadOnlyService, useNodePort);
    }

    private void executeCreateOpenService(Middleware middleware, boolean isReadOnlyService, boolean useNodePort) {
        List<IngressComponentDto> ingressComponentList = ingressComponentService.list(middleware.getClusterId());
        log.info("开始为{}创建对外服务，参数：{}", middleware.getName(), middleware);
        if (CollectionUtils.isEmpty(ingressComponentList) || useNodePort) {
            log.info("不存在ingress，使用NodePort暴露服务");
            MiddlewareServiceNameIndex middlewareServiceNameIndex = ServiceNameConvertUtil.convertMysql(middleware.getName(), isReadOnlyService);
            super.createOpenService(middleware, middlewareServiceNameIndex);
        } else {
            log.info("存在ingress，使用ingress暴露服务");
            createIngressService(middleware, isReadOnlyService);
        }
    }

    @Override
    public void createIngressService(Middleware middleware, boolean isReadOnlyService) {
        List<IngressComponentDto> ingressComponentList = ingressComponentService.list(middleware.getClusterId());
        if (CollectionUtils.isEmpty(ingressComponentList)) {
            return;
        }
        IngressComponentDto ingressComponentDto = ingressComponentList.get(0);
        String ingressClassName = ingressComponentDto.getIngressClassName();
        IngressDTO ingressDTO = new IngressDTO();
        ingressDTO.setIngressClassName(ingressClassName);
        ingressDTO.setExposeType(MIDDLEWARE_EXPOSE_INGRESS);
        ingressDTO.setProtocol(Protocol.TCP.getValue());
        ingressDTO.setMiddlewareType(middleware.getType());
        // 获取mysql服务列表
        List<ServicePortDTO> servicePortDTOList = serviceService.list(middleware.getClusterId(), middleware.getNamespace(), middleware.getName(), middleware.getType());

        if (isReadOnlyService) {
            servicePortDTOList = servicePortDTOList.stream().filter(item ->
                    (item.getServiceName().contains("readonly")))
                    .collect(Collectors.toList());
        } else {
            servicePortDTOList = servicePortDTOList.stream().filter(item ->
                    ((!item.getServiceName().contains("headless")) && (!item.getServiceName().contains("readonly"))))
                    .collect(Collectors.toList());
        }

        if (CollectionUtils.isEmpty(servicePortDTOList)) {
            return;
        }
        ServicePortDTO servicePortDTO = servicePortDTOList.get(0);
        if (CollectionUtils.isEmpty(servicePortDTO.getPortDetailDtoList())) {
            return;
        }
        // 设置需要暴露的服务信息
        PortDetailDTO portDetailDTO = servicePortDTO.getPortDetailDtoList().get(0);
        ServiceDTO serviceDTO = new ServiceDTO();
        serviceDTO.setTargetPort(portDetailDTO.getTargetPort());
        serviceDTO.setServicePort(portDetailDTO.getPort());
        serviceDTO.setServiceName(servicePortDTO.getServiceName());
        List<ServiceDTO> serviceDTOS = new ArrayList<>();
        serviceDTOS.add(serviceDTO);

        ingressDTO.setServiceList(serviceDTOS);
        int availablePort = ingressService.getAvailablePort(middleware.getClusterId(), ingressClassName);
        if (availablePort == 0) {
            throw new BusinessException(ErrorMessage.MYSQL_CONNECTION_FAILED);
        }
        serviceDTO.setExposePort(String.valueOf(availablePort));
        try {
            ingressService.create(middleware.getClusterId(), middleware.getNamespace(), middleware.getName(), ingressDTO);
        } catch (Exception e) {
            log.error("使用ingress暴露服务出错了" , e);
        }
    }

    @Override
    public void deleteDisasterRecoveryInfo(Middleware middleware) {
        // 获取values.yaml
        BeanCacheMiddleware beanCacheMiddleware = cacheMiddlewareService.get(middleware);
        JSONObject values = JSONObject.parseObject(beanCacheMiddleware.getValuesYaml());

        //Middleware detail = middlewareService.detail(middleware.getClusterId(), middleware.getNamespace(), middleware.getName(), middleware.getType());
        if (values.getJSONObject("args").containsKey(MysqlConstant.IS_SOURCE)) {
            JSONObject args = values.getJSONObject("args");
            //将关联实例中存储的当前实例的信息置空
            String relationClusterId = args.getString(MysqlConstant.RELATION_CLUSTER_ID);
            String relationNamespace = args.getString(MysqlConstant.RELATION_NAMESPACE);
            String relationName = args.getString(MysqlConstant.RELATION_NAME);
            Middleware relation = null;
            try {
                relation = middlewareService.detail(relationClusterId, relationNamespace, relationName, middleware.getType());
                relation.setChartName(middleware.getType());
                MiddlewareClusterDTO cluster = clusterService.findById(relationClusterId);
                StringBuilder str = new StringBuilder();
                str.append(String.format("%s.%s=%s,", MysqlConstant.ARGS, MysqlConstant.IS_SOURCE, null));
                str.append(String.format("%s.%s=%s,", MysqlConstant.ARGS, MysqlConstant.RELATION_CLUSTER_ID, null));
                str.append(String.format("%s.%s=%s,", MysqlConstant.ARGS, MysqlConstant.RELATION_NAMESPACE, null));
                str.append(String.format("%s.%s=%s,", MysqlConstant.ARGS, MysqlConstant.RELATION_NAME, null));
                str.append(String.format("%s.%s=%s", MysqlConstant.ARGS, MysqlConstant.RELATION_ALIAS_NAME, null));
                helmChartService.upgrade(relation, str.toString(), cluster);
            } catch (Exception e) {
                log.error("更新关联实例信息出错了", e);
            }
            // 删除灾备关联关系
            try {
                mysqlReplicateCRDService.deleteMysqlReplicate(middleware.getClusterId(), middleware.getNamespace(), middleware.getName());
                log.info("mysql灾备关联关系删除成功");
            } catch (Exception e) {
                log.error("mysql灾备关联关系删除失败", e);
            }
        }
    }

    @Override
    public void prepareDbManageEnv(Middleware middleware) {
        if (middleware.getMysqlDTO() != null && middleware.getMysqlDTO().getDeleteDBManageInfo() != null && Boolean.FALSE.equals(middleware.getMysqlDTO().getDeleteDBManageInfo())) {
            return;
        }
        clearDbManageData(middleware);
        BeanMysqlUser mysqlUser = new BeanMysqlUser();
        mysqlUser.setUser("root");
        mysqlUser.setCreatetime(LocalDateTime.now());
        mysqlUser.setPassword(middleware.getPassword());
        mysqlUser.setMysqlQualifiedName(MysqlConnectionUtil.getMysqlQualifiedName(middleware.getClusterId(), middleware.getNamespace(), middleware.getName()));
        mysqlUserService.create(mysqlUser);
    }

    @Override
    public void clearDbManageData(Middleware middleware) {
        mysqlDbService.delete(middleware.getClusterId(), middleware.getNamespace(), middleware.getName());
        mysqlUserService.delete(middleware.getClusterId(), middleware.getNamespace(), middleware.getName());
        mysqlDbPrivService.delete(middleware.getClusterId(), middleware.getNamespace(), middleware.getName());
    }

    @Override
    public Integer getReplicas(JSONObject values){
        return values.getIntValue(MysqlConstant.REPLICA_COUNT);
    }

}
