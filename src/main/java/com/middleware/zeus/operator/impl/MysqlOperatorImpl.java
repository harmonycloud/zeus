package com.middleware.zeus.operator.impl;

import static com.middleware.zeus.common.constants.NameConstant.*;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.middleware.zeus.bean.BeanClusterMiddlewareInfo;
import com.middleware.zeus.common.enums.Protocol;
import com.middleware.zeus.common.model.ActiveAreaAnnotationDto;
import com.middleware.zeus.common.model.IngressComponentDto;
import com.middleware.zeus.common.model.MiddlewareServiceNameIndex;
import com.middleware.zeus.dao.BeanClusterMiddlewareInfoMapper;
import com.middleware.zeus.util.cmd.CmdExecUtil;
import com.middleware.zeus.bean.BeanCacheMiddleware;
import com.middleware.zeus.bean.BeanMysqlUser;
import com.middleware.zeus.service.k8s.*;
import com.middleware.zeus.integration.cluster.bean.*;
import com.middleware.zeus.service.middleware.ImageRepositoryService;
import com.middleware.zeus.service.mysql.MysqlDbPrivService;
import com.middleware.zeus.service.mysql.MysqlDbService;
import com.middleware.zeus.service.mysql.MysqlUserService;
import com.middleware.zeus.util.*;
import com.middleware.zeus.common.model.middleware.*;
import com.middleware.zeus.operator.BaseOperator;
import com.middleware.zeus.operator.api.MysqlOperator;
import com.middleware.zeus.operator.miiddleware.AbstractMysqlOperator;
import com.middleware.zeus.service.middleware.BackupService;
import com.middleware.zeus.service.middleware.impl.MiddlewareServiceImpl;
import com.middleware.zeus.util.middleware.ChartVersionUtil;
import com.middleware.zeus.util.middleware.MiddlewareResourceCalculateUtil;
import com.middleware.zeus.util.middleware.MysqlConnectionUtil;
import com.middleware.zeus.util.numeric.ResourceCalculationUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.CollectionUtils;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.middleware.zeus.common.constants.MysqlConstant;
import com.middleware.zeus.common.enums.DateType;
import com.middleware.zeus.common.enums.DictEnum;
import com.middleware.zeus.common.enums.ErrorMessage;
import com.middleware.zeus.common.enums.middleware.MiddlewareTypeEnum;
import com.middleware.zeus.common.exception.BusinessException;
import com.middleware.zeus.util.date.DateUtils;
import com.middleware.zeus.util.encrypt.PasswordUtils;
import com.middleware.zeus.annotation.Operator;
import com.middleware.zeus.integration.cluster.MysqlClusterWrapper;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.ObjectUtils;

import java.text.MessageFormat;

import static com.middleware.zeus.common.constants.CmdConstant.MYSQL_HAND_SWITCH;
import static com.middleware.zeus.common.constants.CommonConstant.OFF;
import static com.middleware.zeus.common.constants.CommonConstant.ON;
import static com.middleware.zeus.common.constants.MysqlConstant.SLOW_QUERY_LOG;
import static com.middleware.zeus.common.constants.middleware.MiddlewareConstant.*;

/**
 * @author dengyulong
 * @date 2021/03/23
 * 处理mysql逻辑
 */
@Slf4j
@Operator(paramTypes4One = Middleware.class)
public class MysqlOperatorImpl extends AbstractMysqlOperator implements MysqlOperator {

    @Value("${system.gracefulRestartParam:middleware.maintenance.lock:graceful-restart}")
    private String gracefulRestartParam;

    @Autowired
    private MysqlClusterWrapper mysqlClusterWrapper;
    @Autowired
    private BackupService backupService;
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
    @Autowired
    private BeanClusterMiddlewareInfoMapper beanClusterMiddlewareInfoMapper;


    @Override
    public boolean support(Middleware middleware) {
        return MiddlewareTypeEnum.MYSQL == MiddlewareTypeEnum.findByType(middleware.getType());
    }

    @Override
    public void replaceValues(Middleware middleware, MiddlewareClusterDTO cluster, JSONObject values) {
        // 替换通用的值
        replaceCommonValues(middleware, cluster, values);
        MiddlewareQuota quota = middleware.getQuota().get(middleware.getType());
        replaceCommonResources(quota, values.getJSONObject(RESOURCES));
        replaceCommonStorages(quota, values);
        JSONObject stsConfig = values.getJSONObject("statefulSetConfiguration");
        // 设置uid和gid
        if (stsConfig != null && stsConfig.containsKey("securityContext")) {
            super.setSecurityContext(middleware, stsConfig);
        }

        //添加业务数据库
        if (middleware.getBusinessDeploy() != null && !middleware.getBusinessDeploy().isEmpty()) {
            JSONArray array = values.getJSONArray("businessDeploy");
            middleware.getBusinessDeploy().forEach(mysqlBusinessDeploy -> array.add(JSONUtil.parse(mysqlBusinessDeploy)));
        }

        // mysql参数
        JSONObject mysqlArgs = values.getJSONObject("args");
        if (StringUtils.isBlank(middleware.getPassword())) {
            middleware.setPassword(PasswordUtils.generateCommonPassword(10));
        }
        log.info("mysql特有参数：{}", mysqlArgs);
        mysqlArgs.put("root_password", middleware.getPassword());
        if (StringUtils.isNotBlank(middleware.getCharSet())) {
            mysqlArgs.put("character_set_server", middleware.getCharSet());
        }
        if (StringUtils.isNotBlank(middleware.getLanguage())){
            mysqlArgs.put("collation_server",middleware.getLanguage());
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
        }
        // 配置开启/关闭 审计日志和慢日志
        if (middleware.getSlowSql() != null && middleware.getSlowSql()) {
            mysqlArgs.put(SLOW_QUERY_LOG, ON);
        } else {
            mysqlArgs.put(SLOW_QUERY_LOG, OFF);
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
        convertRegistry(middleware, values);

        // 设置uid和gid
        JSONObject stsConfig = values.getJSONObject("statefulSetConfiguration");
        if (stsConfig != null && stsConfig.containsKey("securityContext")) {
            convertSecurityContext(middleware, stsConfig);
        }
        // 处理mysql的特有参数
        if (values != null) {
            convertResourcesByHelmChart(middleware, middleware.getType(), values.getJSONObject(RESOURCES));
            // 设置从节点数量
            Integer num = values.getInteger("replicaCount");
            if (num != null) {
                num = num - 1;
                middleware.getQuota().get(middleware.getType()).setNum(num);
            }

            JSONObject args = values.getJSONObject("args");
            if (args == null) {
                args = values.getJSONObject("mysqlArgs");
            }
            if (checkUserAuthority(MiddlewareTypeEnum.MYSQL.getType())) {
                middleware.setPassword(args.getString("root_password"));
            }
            middleware.setCharSet(args.getString("character_set_server"));
            middleware.setLanguage(args.getString("collation_server"));
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
            // 读写分离
            if (values.containsKey("proxy")){
                ReadWriteProxy readWriteProxy = new ReadWriteProxy();
                readWriteProxy.setEnabled(values.getJSONObject("proxy").getBoolean("enable"));
                middleware.setReadWriteProxy(readWriteProxy);
            }
            // 慢日志开关
            if (args.containsKey("slow_query_log") && args.getString("slow_query_log").equals(ON)){
                middleware.setSlowSql(true);
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
                // 修改proxy cpu参数规格
                String proxyCpu = MiddlewareResourceCalculateUtil.calculateProxyResource(quota.getCpu());
                sb.append("proxy.resources.requests.cpu=").append(proxyCpu).append(",proxy.resources.limits.cpu=").append(proxyCpu).append(",");
            }
            if (StringUtils.isNotBlank(quota.getMemory())) {
                sb.append("resources.requests.memory=").append(quota.getMemory()).append(",resources.limits.memory=")
                        .append(quota.getLimitMemory()).append(",");
                // 修改proxy memory参数规格
                String proxyMem = MiddlewareResourceCalculateUtil.calculateProxyResource(quota.getMemory().replace("Gi", ""));
                if (Double.parseDouble(proxyMem) < 0.256){
                    proxyMem = String.valueOf(0.256);
                }
                sb.append("proxy.resources.requests.memory=").append(proxyMem).append("Gi,proxy.resources.limits.memory=").append(proxyMem).append("Gi,");
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

        // 慢日志更新
        if (middleware.getSlowSql() != null) {
            if (middleware.getSlowSql()) {
                sb.append("args.slow_query_log=").append(ON).append(",");
            } else {
                sb.append("args.slow_query_log=").append(OFF).append(",");
            }
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
        helmChartService.upgrade(middleware, sb.toString(), null, cluster);
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
     * 检查是否是双活分区并设置双活配置字段
     * @param values
     * @param middleware
     */
    @Override
    public void checkAndSetActiveActive(JSONObject values, Middleware middleware) {
        if (namespaceService.isOpenAvailableDomain(middleware.getClusterId(), middleware.getNamespace())) {
            super.setActiveActiveConfig(null, values);
            super.setActiveActiveToleration(middleware, values);
            values.put(ACTIVE_ACTIVE, true);
        }
    }

    @Override
    public void deleteStorage(Middleware middleware) {
        this.deleteDisasterRecoveryInfo(middleware);
        super.deleteStorage(middleware);
        clearDbManageData(middleware);
    }

    @Override
    public SwitchInfo getManualSwitch(Middleware middleware) {
        SwitchInfo switchInfo = new SwitchInfo().setStatus(true);
        // 获取mysqlCluster
        MysqlCluster mysqlCluster =
            mysqlClusterWrapper.get(middleware.getClusterId(), middleware.getNamespace(), middleware.getName());
        List<Status.Condition> conditions = mysqlCluster.getStatus().getConditions();
        List<Status.Condition> syncList =
            conditions.stream().filter(con -> con.getType().equals("SyncSlave")).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(syncList)) {
            switchInfo.setStatus(false);
        }
        return switchInfo;
    }

    @Override
    public SwitchInfo getAutoSwitch(Middleware middleware) {
        SwitchInfo autoSwitchInfo = new SwitchInfo();
        MysqlCluster mysqlCluster = mysqlClusterWrapper.get(middleware.getClusterId(), middleware.getNamespace(), middleware.getName());
        autoSwitchInfo.setIsAuto(mysqlCluster.getSpec().getPassiveSwitched() == null || !mysqlCluster.getSpec().getPassiveSwitched());
        if (mysqlCluster.getStatus() != null && mysqlCluster.getStatus().getLastChangeMaster() != null) {
            autoSwitchInfo.setLastAutoSwitchTime(DateUtils.parseUTCDate(mysqlCluster.getStatus().getLastChangeMaster()));
        }
        return autoSwitchInfo;
    }

    @Override
    public SwitchInfo switchMiddleware(Middleware middleware) {
        MysqlCluster mysqlCluster = mysqlClusterWrapper.get(middleware.getClusterId(), middleware.getNamespace(), middleware.getName());
        if (mysqlCluster == null) {
            throw new BusinessException(DictEnum.MYSQL_CLUSTER, middleware.getName(), ErrorMessage.NOT_EXIST);
        }
        return middleware.getAutoSwitch() == null ? handSwitch(middleware, mysqlCluster) : autoSwitch(middleware, mysqlCluster);
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
                    data.set(i, data.get(i).replace(temp, customConfig.getValue().toString()));
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
    private SwitchInfo handSwitch(Middleware middleware, MysqlCluster mysqlCluster) {
        // 判断版本  根据operator版本判断切换方式
        QueryWrapper<BeanClusterMiddlewareInfo> wrapper = new QueryWrapper<>();
        wrapper.eq("cluster_id", middleware.getClusterId()).eq("chart_name", "mysql");
        BeanClusterMiddlewareInfo cm = beanClusterMiddlewareInfoMapper.selectOne(wrapper);
        if (ObjectUtils.isEmpty(cm) || cm.getChartVersion() == null) {
            throw new BusinessException(ErrorMessage.OPERATOR_INFO_ERROR);
        }
        if (ChartVersionUtil.compare(cm.getChartVersion(), "1.8.20") > 0) {
            return switchByChangeCr(middleware, mysqlCluster);
        } else {
            return switchByCurl(middleware, mysqlCluster);
        }
    }

    private SwitchInfo switchByCurl(Middleware middleware, MysqlCluster mysqlCluster) {
        MiddlewareClusterDTO cluster = clusterService.findById(middleware.getClusterId());
        // 先判断有没有sync_slave
        List<Status.Condition> conditions = mysqlCluster.getStatus().getConditions();
        List<Status.Condition> syncList = conditions.stream().filter(con -> con.getType().equals("SyncSlave")).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(syncList)) {
            throw new BusinessException(DictEnum.ROLE,SYNC_SLAVE,ErrorMessage.NOT_FOUND);
        }
        // 获取同步节点名称
        String syncName = syncList.get(0).getName();
        String execCommand = MessageFormat.format(MYSQL_HAND_SWITCH,
                syncName, middleware.getNamespace(), cluster.getAddress(), cluster.getAccessToken(),
                syncName, middleware.getNamespace(), mysqlCluster.getMetadata().getName());
        List<String> results = new ArrayList<>(2);
        // 411状态重发
        for (int i = 0; i <= 10; i++) {
            if (i > 0) {
                log.error("411异常重发请求，进行第{}次重发", i);
            }
            results = CmdExecUtil.execCmd(execCommand, null);
            if (!"411".equals(results.get(1)) || !results.get(0).endsWith("please apply your changes to the latest version and try again")) {
                break;
            }
        }
        // 判断结果
        parseHandSwitchResult(results);
        return new SwitchInfo().setNewMasterName(syncName);
    }

    private SwitchInfo switchByChangeCr(Middleware middleware, MysqlCluster mysqlCluster){
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
        return new SwitchInfo().setNewMasterName(slaveName);
    }

    /**
     * 自动切换
     */
    private SwitchInfo autoSwitch(Middleware middleware, MysqlCluster mysqlCluster) {
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
        return null;
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
        JSONObject proxy = values.containsKey("proxy") ? values.getJSONObject("proxy") : new JSONObject();
        proxy.put("enable", readWriteProxy.getEnabled());

        JSONObject requests = new JSONObject();
        JSONObject limits = new JSONObject();

        if (middleware.getQuota().containsKey(PROXY)){
            MiddlewareQuota proxyQuota = middleware.getQuota().get(PROXY);
            requests.put(CPU, proxyQuota.getCpu());
            requests.put(MEMORY, proxyQuota.getMemory());
            limits.put(CPU, proxyQuota.getCpu());
            limits.put(MEMORY, proxyQuota.getMemory());
        } else {
            MiddlewareQuota quota = middleware.getQuota().get(middleware.getType());
            String cpu = MiddlewareResourceCalculateUtil.calculateProxyResource(quota.getCpu());
            String memory = MiddlewareResourceCalculateUtil.calculateProxyResource(quota.getMemory().replace("Gi", ""));
            if (Double.parseDouble(memory) < 0.256){
                memory = String.valueOf(0.256);
            }
            requests.put(CPU, cpu);
            requests.put(MEMORY, memory + "Gi");
            limits.put(CPU, cpu);
            limits.put(MEMORY, memory + "Gi");
        }

        // 获取proxy节点数
        MysqlDTO mysqlDTO = middleware.getMysqlDTO();
        int replicaCount = mysqlDTO.getReplicaCount();
        proxy.put("replicaCount", replicaCount + 1);

        JSONObject resources = new JSONObject();
        resources.put("requests", requests);
        resources.put("limits", limits);

        proxy.put("resources", resources);
        values.put("proxy", proxy);
    }

    @Override
    public void createDisasterRecoveryMiddleware(Middleware middleware) {
        if (!licenseService.check(middleware.getClusterId())){
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
        MiddlewareClusterDTO cluster = clusterService.findById(relationMiddleware.getClusterId());
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

    @Override
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
                helmChartService.upgrade(relation, str.toString(), null, cluster);
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
    public ActiveAreaAnnotationDto getActiveAreaAnnotation(String clusterId, String namespace, String type, String middlewareName) {
        return super.getActiveAreaAnnotation(clusterId, namespace, type, middlewareName);
    }

    @Override
    public Double calculateCpuRequest(JSONObject values) {
        JSONObject resources = values.getJSONObject(RESOURCES);
        if (resources == null) {
            return 0.0;
        }
        String cpu = resources.getJSONObject(REQUESTS).getString(CPU);
        return ResourceCalculationUtil.getResourceValue(cpu, CPU, "") * getReplicas(values);
    }

    @Override
    public Integer getReplicas(JSONObject values){
        return values.getIntValue(MysqlConstant.REPLICA_COUNT);
    }

    @Override
    public List<IngressDTO> listHostNetworkAddress(String clusterId, String namespace, String middlewareName, String type) {
        return null;
    }

    @Override
    public void reboot(String clusterId, String namespace, String name, String type, String podType) {
        MysqlCluster mysqlCluster = mysqlClusterWrapper.get(clusterId, namespace, name);
        Integer replicas = mysqlCluster.getSpec().getReplicas();
        if (replicas == 1) {
            super.reboot(clusterId, namespace, name, type, podType);
            return;
        }
        Map<String, String> annotations = mysqlCluster.getMetadata().getAnnotations();
        String[] params = gracefulRestartParam.split(",");
        for (String param : params) {
            String[] kv = param.split(":");
            annotations.put(kv[0], kv[1]);
        }
        if (PROXY.equals(podType)){
            annotations.put("middleware.maintenance.component", "proxysql");
        }
        try {
            mysqlClusterWrapper.update(clusterId, namespace, mysqlCluster);
        }catch (Exception e){
            log.error("触发mysql优雅重启失败", e);
        }
    }

    @Override
    public Set<String> getCustomConfigRole(JSONObject values) {
        HashSet<String> result = new HashSet<>();
        result.add("major");
        if (values.containsKey("proxy") && values.getJSONObject("proxy").containsKey("enable")
            && values.getJSONObject("proxy").getBoolean("enable")) {
            result.add("proxy");
        }
        return result;
    }

    @Override
    public Boolean withProxy(String clusterId, Middleware middleware) {
        JSONObject values = helmChartService.getInstalledValues(middleware, clusterService.findById(clusterId));
        if (values.containsKey("proxy") && values.getJSONObject("proxy").containsKey("enable")
            && values.getJSONObject("proxy").getBoolean("enable")) {
            return true;
        }
        return false;
    }

}
