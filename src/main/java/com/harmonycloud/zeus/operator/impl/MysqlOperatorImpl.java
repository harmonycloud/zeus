package com.harmonycloud.zeus.operator.impl;

import static com.harmonycloud.caas.common.constants.NameConstant.RESOURCES;
import static com.harmonycloud.caas.common.constants.middleware.MiddlewareConstant.*;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import com.alibaba.fastjson.JSON;
import com.harmonycloud.caas.common.enums.Protocol;
import com.harmonycloud.caas.common.model.IngressComponentDto;
import com.harmonycloud.caas.common.model.MiddlewareServiceNameIndex;
import com.harmonycloud.caas.common.model.MysqlDbDTO;
import com.harmonycloud.zeus.bean.BeanCacheMiddleware;
import com.harmonycloud.zeus.bean.BeanMysqlUser;
import com.harmonycloud.zeus.service.k8s.*;
import com.harmonycloud.zeus.service.mysql.MysqlDbPrivService;
import com.harmonycloud.zeus.service.mysql.MysqlDbService;
import com.harmonycloud.zeus.service.mysql.MysqlUserService;
import com.harmonycloud.zeus.util.MysqlConnectionUtil;
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
 * ??????mysql??????
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
    private ClusterService clusterService;
    @Autowired
    private MysqlReplicateCRDService mysqlReplicateCRDService;
    @Autowired
    private MiddlewareServiceImpl middlewareService;
    @Autowired
    private BaseOperatorImpl baseOperator;
    @Autowired
    private StorageClassService storageClassService;
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

    @Override
    public boolean support(Middleware middleware) {
        return MiddlewareTypeEnum.MYSQL == MiddlewareTypeEnum.findByType(middleware.getType());
    }

    @Override
    public void replaceValues(Middleware middleware, MiddlewareClusterDTO cluster, JSONObject values) {
        // ??????????????????
        replaceCommonValues(middleware, cluster, values);
        MiddlewareQuota quota = middleware.getQuota().get(middleware.getType());
        // ???????????????????????????3Gi????????????????????????????????????
        if (StringUtils.isNotBlank(middleware.getBackupFileName())) {
            int storageSize = Integer.parseInt(quota.getStorageClassQuota()) + 3;
            quota.setStorageClassQuota(String.valueOf(storageSize));
        }
        replaceCommonResources(quota, values.getJSONObject(RESOURCES));
        replaceCommonStorages(quota, values);

        //?????????????????????
        if (middleware.getBusinessDeploy() != null && !middleware.getBusinessDeploy().isEmpty()) {
            JSONArray array = values.getJSONArray("businessDeploy");
            middleware.getBusinessDeploy().forEach(mysqlBusinessDeploy -> array.add(JSONUtil.parse(mysqlBusinessDeploy)));
        }

        // mysql??????
        JSONObject mysqlArgs = values.getJSONObject("args");
        JSONObject features = values.getJSONObject("features");
        if (StringUtils.isBlank(middleware.getPassword())) {
            middleware.setPassword(PasswordUtils.generateCommonPassword(10));
        }
        log.info("mysql???????????????{}", mysqlArgs);
        mysqlArgs.put("root_password", middleware.getPassword());
        if (StringUtils.isNotBlank(middleware.getCharSet())) {
            mysqlArgs.put("character_set_server", middleware.getCharSet());
        }
        if (middleware.getPort() != null) {
            mysqlArgs.put("server_port", middleware.getPort());
        }
        if (middleware.getMysqlDTO() != null) {
            MysqlDTO mysqlDTO = middleware.getMysqlDTO();
            if (mysqlDTO.getReplicaCount() != null && mysqlDTO.getReplicaCount() > 0) {
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
            //??????SQL????????????
            checkAndSetAuditSqlStatus(features, mysqlDTO);
        }
        //??????mysql????????????
        if (!CollectionUtils.isEmpty(middleware.getEnvironment())) {
            middleware.getEnvironment().forEach(mysqlEnviroment -> mysqlArgs.put(mysqlEnviroment.getName(), mysqlEnviroment.getValue()));
        }
        // ?????????????????????
        if (StringUtils.isNotEmpty(middleware.getBackupFileName())) {
            BackupStorageProvider backupStorageProvider = backupService.getStorageProvider(middleware);
            values.put("storageProvider", JSONObject.toJSON(backupStorageProvider));
        }
    }

    @Override
    public Middleware convertByHelmChart(Middleware middleware, MiddlewareClusterDTO cluster) {
        JSONObject values = helmChartService.getInstalledValues(middleware, cluster);
        convertCommonByHelmChart(middleware, values);
        convertStoragesByHelmChart(middleware, middleware.getType(), values);
        convertRegistry(middleware, cluster);

        // ??????mysql???????????????
        if (values != null) {
            convertResourcesByHelmChart(middleware, middleware.getType(), values.getJSONObject(RESOURCES));
            JSONObject args = values.getJSONObject("args");
            if (args == null) {
                args = values.getJSONObject("mysqlArgs");
            }
            middleware.setPassword(args.getString("root_password"));
            middleware.setCharSet(args.getString("character_set_server"));
            middleware.setPort(args.getIntValue("server_port"));

            MysqlDTO mysqlDTO = new MysqlDTO();
            mysqlDTO.setReplicaCount(args.getIntValue(MysqlConstant.REPLICA_COUNT));
            // ????????????????????????
            MiddlewareQuota mysql = middleware.getQuota().get("mysql");
            mysqlDTO.setIsLvmStorage(mysql.getIsLvmStorage());
            middleware.setMysqlDTO(mysqlDTO);
            // ????????????????????????
            Boolean isSource = args.getBoolean(MysqlConstant.IS_SOURCE);
            if (isSource != null) {
                mysqlDTO.setOpenDisasterRecoveryMode(true);
                mysqlDTO.setIsSource(isSource);
                mysqlDTO.setReplicaCount(args.getIntValue(MysqlConstant.REPLICA_COUNT));
                //????????????????????????
                String relationClusterId = args.getString(MysqlConstant.RELATION_CLUSTER_ID);
                String relationNamespace = args.getString(MysqlConstant.RELATION_NAMESPACE);
                String relationName = args.getString(MysqlConstant.RELATION_NAME);
                String relationAliasName = args.getString(MysqlConstant.RELATION_ALIAS_NAME);
                String chartName = args.getString(MysqlConstant.CHART_NAME);
                mysqlDTO.setRelationClusterId(relationClusterId);
                mysqlDTO.setRelationNamespace(relationNamespace);
                mysqlDTO.setRelationName(relationName);
                mysqlDTO.setRelationAliasName(relationAliasName);
                mysqlDTO.setRelationExist(baseOperator.checkIfExist(relationNamespace, relationName, cluster));
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
        }
        return middleware;
    }

    @Override
    public void create(Middleware middleware, MiddlewareClusterDTO cluster) {
        super.create(middleware, cluster);
        MysqlDTO mysqlDTO = middleware.getMysqlDTO();
        if (mysqlDTO.getOpenDisasterRecoveryMode() != null && mysqlDTO.getOpenDisasterRecoveryMode() && mysqlDTO.getIsSource()) {
            String jsonStr = JSON.toJSONString(middleware);
            Middleware relationMiddleware = JSON.parseObject(jsonStr, Middleware.class);
            middleware.setRelationMiddleware(relationMiddleware);
            middlewareManageTask.asyncCreateDisasterRecoveryMiddleware(this, middleware);
        }
        // ??????????????????
        prepareDbManageOpenService(middleware);
        // ???????????????????????????
        prepareDbManageEnv(middleware);
    }

    @Override
    public void update(Middleware middleware, MiddlewareClusterDTO cluster) {
        if (cluster == null) {
            cluster = clusterService.findById(middleware.getClusterId());
        }
        StringBuilder sb = new StringBuilder();

        // ????????????
        if (middleware.getQuota() != null && middleware.getQuota().get(middleware.getType()) != null) {
            MiddlewareQuota quota = middleware.getQuota().get(middleware.getType());
            // ??????limit???resources
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

        // ????????????
        if (StringUtils.isNotBlank(middleware.getPassword())) {
            sb.append("args.root_password=").append(middleware.getPassword()).append(",");
        }

        // ????????????????????????
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

        // ??????????????????
        super.updateCommonValues(sb, middleware);

        // ???????????????????????????
        if (sb.length() == 0) {
            return;
        }
        // ?????????????????????
        sb.deleteCharAt(sb.length() - 1);
        // ??????helm
        helmChartService.upgrade(middleware, sb.toString(), cluster);
        if (mysqlDTO != null && mysqlDTO.getOpenDisasterRecoveryMode() != null && mysqlDTO.getOpenDisasterRecoveryMode() && mysqlDTO.getIsSource()) {
            Middleware disasterRecoverMiddleware = middleware.getRelationMiddleware();
            disasterRecoverMiddleware.setChartName(middleware.getChartName());
            disasterRecoverMiddleware.setChartVersion(middleware.getChartVersion());
            this.createDisasterRecoveryMiddleware(middleware);
        }
    }

    public void prepareDbManageOpenService(Middleware middleware){
        middlewareManageTask.asyncCreateMysqlOpenService(this, middleware);
    }

    /**
     * ???????????????mysql SQL??????????????????????????????SQL??????????????????????????????
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

    /*@Override
    public void delete(Middleware middleware) {
        this.deleteDisasterRecoveryInfo(middleware);
        super.delete(middleware);
        if (middleware.getDeleteBackupInfo() == null || middleware.getDeleteBackupInfo()) {
            // ??????????????????
            mysqlBackupService.deleteMiddlewareBackupInfo(middleware.getClusterId(), middleware.getNamespace(), middleware.getType(), middleware.getName());
            // ????????????????????????
            mysqlScheduleBackupService.delete(middleware.getClusterId(), middleware.getNamespace(), middleware.getName());
        }
    }*/

    @Override
    public void deleteStorage(Middleware middleware) {
        this.deleteDisasterRecoveryInfo(middleware);
        super.deleteStorage(middleware);
        clearDbManageData(middleware);
        if (middleware.getDeleteBackupInfo() == null || middleware.getDeleteBackupInfo()) {
            // ??????????????????
            mysqlBackupService.deleteMiddlewareBackupInfo(middleware.getClusterId(), middleware.getNamespace(), middleware.getType(), middleware.getName());
            // ????????????????????????
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
        // ????????????
        if (handSwitch(middleware, mysqlCluster)) {
            return;
        }
        // ????????????
        autoSwitch(middleware, mysqlCluster);

    }

    @Override
    public List<String> getConfigmapDataList(ConfigMap configMap) {
        return new ArrayList<>(Arrays.asList(configMap.getData().get("my.cnf.tmpl").split("\n")));
    }

    /**
     * ?????????configmap
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
            // ????????????
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
     * ??????data???map??????
     */
    @Override
    public void updateConfigData(ConfigMap configMap, List<String> data) {
        // ?????????configmap
        StringBuilder temp = new StringBuilder();
        for (String str : data) {
            temp.append(str).append("\n");
        }
        configMap.getData().put("my.cnf.tmpl", temp.toString());
    }

    /**
     * ????????????
     */
    private boolean handSwitch(Middleware middleware, MysqlCluster mysqlCluster) {
        // ?????????null??????????????????????????????
        if (middleware.getAutoSwitch() != null) {
            // false??????????????????true????????????
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
        } catch (IOException e) {
            log.error("??????id:{}???????????????:{}???mysql??????:{}?????????????????????", middleware.getClusterId(), middleware.getNamespace(),
                    middleware.getName(), e);
            throw new BusinessException(DictEnum.MYSQL_CLUSTER, middleware.getName(), ErrorMessage.SWITCH_FAILED);
        }
        return true;
    }

    /**
     * ????????????
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
                log.error("??????id:{}???????????????:{}???mysql??????:{}?????????/????????????????????????", middleware.getClusterId(),
                        middleware.getNamespace(), middleware.getName(), e);
                throw new BusinessException(DictEnum.MYSQL_CLUSTER, middleware.getName(), ErrorMessage.SWITCH_FAILED);
            }
        }
    }

    /**
     * ????????????????????????
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
            log.error("????????????{} ,??????????????????????????????", scheduleBackup.getName());
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

                //??????mysql?????????????????????????????????
                MysqlReplicateCR mysqlReplicate;
                if (isSource) {
                    mysqlReplicate = mysqlReplicateCRDService.getMysqlReplicate(relationClusterId, relationNamespace, relationName);
                } else {
                    mysqlReplicate = mysqlReplicateCRDService.getMysqlReplicate(clusterId, namespace, middlewareName);
                }
                if (mysqlReplicate != null) {
                    log.info("????????????????????????,clusterId={}, namespace={}, middlewareName={}", clusterId, namespace, middlewareName);
                    mysqlReplicateCRDService.deleteMysqlReplicate(clusterId, namespace, mysqlReplicate.getMetadata().getName());
                    log.info("????????????????????????");
                } else {
                    log.info("??????????????????????????????");
                }

                try {
                    MiddlewareClusterDTO middlewareClusterDTO = clusterService.findById(clusterId);
                    mysqlDTO.setIsSource(null);
                    mysqlDTO.setOpenDisasterRecoveryMode(false);
                    mysqlDTO.setType("master-slave");
                    update(middleware, middlewareClusterDTO);
                } catch (Exception e) {
                    log.error("????????????????????????", e);
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
                    log.error("????????????????????????", e);
                }
            }
        }
    }

    public void createDisasterRecoveryMiddleware(Middleware middleware) {
        MysqlDTO mysqlDTO = middleware.getMysqlDTO();
        //1.?????????????????????????????????(NodePort)
        createOpenService(middleware, true);
        //2.?????????????????????????????????????????????
        //2.1 ????????????????????????
        Middleware relationMiddleware = middleware.getRelationMiddleware();
        relationMiddleware.setClusterId(mysqlDTO.getRelationClusterId());
        relationMiddleware.setNamespace(mysqlDTO.getRelationNamespace());
        relationMiddleware.setName(mysqlDTO.getRelationName());
        relationMiddleware.setAliasName(mysqlDTO.getRelationAliasName());

        //2.2 ????????????????????????????????????
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

        BaseOperator operator = middlewareService.getOperator(BaseOperator.class, BaseOperator.class, relationMiddleware);
        MiddlewareClusterDTO cluster = clusterService.findByIdAndCheckRegistry(relationMiddleware.getClusterId());
        operator.createPreCheck(relationMiddleware, cluster);
        this.create(relationMiddleware, cluster);
        //3.????????????????????????
        this.createMysqlReplicate(middleware, relationMiddleware);

    }

    /**
     * ?????????????????????????????????????????????
     *
     * @param original
     */
    public void createMysqlReplicate(Middleware original, Middleware disasterRecovery) {
        Middleware middleware = middlewareService.detail(original.getClusterId(), original.getNamespace(), original.getName(), original.getType());
        List<IngressDTO> ingressDTOS = ingressService.get(original.getClusterId(), middleware.getNamespace(),
                middleware.getType(), middleware.getName());
        log.info("????????????MysqlReplicate,middleware={},ingressDTOS={}", middleware, ingressDTOS);
        if (!CollectionUtils.isEmpty(ingressDTOS)) {
            // ??????????????????
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

                    // ?????????mysqlreplicate
                    try {
                        mysqlReplicateCRDService.deleteMysqlReplicate(disasterRecovery.getClusterId(), disasterRecovery.getNamespace(), mysqlReplicateCR.getMetadata().getName());
                    } catch (Exception e) {
                        log.error("??????mysqlreplicate?????????", e);
                    }

                    try {
                        log.info("??????mysql?????? {} ??? {} ???????????????MysqlReplicate", original.getName(), middleware.getName());
                        mysqlReplicateCRDService.createMysqlReplicate(disasterRecovery.getClusterId(), mysqlReplicateCR);
                        log.info("MysqlReplicate????????????");
                    } catch (IOException e) {
                        log.error("MysqlReplicate????????????", e);
                        e.printStackTrace();
                    }
                }
            }
        } else {
            log.info("????????????????????????????????????MysqlReplicate");
        }
    }

    public void createOpenService(Middleware middleware, boolean isReadOnlyService) {
        log.info("????????????{} ??????????????????");
        executeCreateOpenService(middleware, isReadOnlyService);
    }

    private void executeCreateOpenService(Middleware middleware, boolean isReadOnlyService) {
        List<IngressComponentDto> ingressComponentList = ingressComponentService.list(middleware.getClusterId());
        log.info("?????????{}??????????????????????????????{}", middleware.getName(), middleware);
        if (CollectionUtils.isEmpty(ingressComponentList)) {
            log.info("?????????ingress?????????NodePort????????????");
            MiddlewareServiceNameIndex middlewareServiceNameIndex = ServiceNameConvertUtil.convertMysql(middleware.getName(), isReadOnlyService);
            super.createOpenService(middleware, middlewareServiceNameIndex);
        } else {
            log.info("??????ingress?????????ingress????????????");
            createIngressService(middleware, isReadOnlyService);
        }
    }

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
        // ??????mysql????????????
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
        // ?????????????????????????????????
        PortDetailDTO portDetailDTO = servicePortDTO.getPortDetailDtoList().get(0);
        ServiceDTO serviceDTO = new ServiceDTO();
        serviceDTO.setTargetPort(portDetailDTO.getTargetPort());
        serviceDTO.setServicePort(portDetailDTO.getPort());
        serviceDTO.setServiceName(servicePortDTO.getServiceName());
        List<ServiceDTO> serviceDTOS = new ArrayList<>();
        serviceDTOS.add(serviceDTO);

        ingressDTO.setServiceList(serviceDTOS);
        int availablePort = ingressService.getAvailablePort(middleware.getClusterId(), ingressClassName);
        serviceDTO.setExposePort(String.valueOf(availablePort));
        try {
            ingressService.create(middleware.getClusterId(), middleware.getNamespace(), middleware.getName(), ingressDTO);
        } catch (Exception e) {
            log.error("??????ingress?????????????????????" , e);
        }
    }

    public void deleteDisasterRecoveryInfo(Middleware middleware) {
        // ??????values.yaml
        BeanCacheMiddleware beanCacheMiddleware = cacheMiddlewareService.get(middleware);
        JSONObject values = JSONObject.parseObject(beanCacheMiddleware.getValuesYaml());

        //Middleware detail = middlewareService.detail(middleware.getClusterId(), middleware.getNamespace(), middleware.getName(), middleware.getType());
        if (values.getJSONObject("args").containsKey(MysqlConstant.IS_SOURCE)) {
            JSONObject args = values.getJSONObject("args");
            //??????????????????????????????????????????????????????
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
                log.error("?????????????????????????????????", e);
            }
            // ????????????????????????
            try {
                mysqlReplicateCRDService.deleteMysqlReplicate(middleware.getClusterId(), middleware.getNamespace(), middleware.getName());
                log.info("mysql??????????????????????????????");
            } catch (Exception e) {
                log.error("mysql??????????????????????????????", e);
            }
        }
    }

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

    public void clearDbManageData(Middleware middleware) {
        mysqlDbService.delete(middleware.getClusterId(), middleware.getNamespace(), middleware.getName());
        mysqlUserService.delete(middleware.getClusterId(), middleware.getNamespace(), middleware.getName());
        mysqlDbPrivService.delete(middleware.getClusterId(), middleware.getNamespace(), middleware.getName());
    }

}
