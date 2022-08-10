package com.harmonycloud.zeus.service.middleware.impl;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.json.JSONUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.github.pagehelper.PageInfo;
import com.harmonycloud.caas.common.constants.NameConstant;
import com.harmonycloud.caas.common.enums.ErrorMessage;
import com.harmonycloud.caas.common.enums.middleware.MiddlewareTypeEnum;
import com.harmonycloud.caas.common.exception.CaasRuntimeException;
import com.harmonycloud.caas.common.model.*;
import com.harmonycloud.caas.common.model.middleware.Middleware;
import com.harmonycloud.caas.common.model.middleware.MiddlewareAlertsDTO;
import com.harmonycloud.caas.common.model.registry.HelmChartFile;
import com.harmonycloud.caas.common.model.user.UserDto;
import com.harmonycloud.tool.date.DateUtils;
import com.harmonycloud.tool.uuid.UUIDUtils;
import com.harmonycloud.zeus.bean.AlertRuleId;
import com.harmonycloud.zeus.bean.BeanAlertRule;
import com.harmonycloud.zeus.bean.BeanAlertSetting;
import com.harmonycloud.zeus.bean.BeanMailToUser;
import com.harmonycloud.zeus.dao.AlertRuleIdMapper;
import com.harmonycloud.zeus.dao.BeanAlertRuleMapper;
import com.harmonycloud.zeus.dao.BeanAlertSettingMapper;
import com.harmonycloud.zeus.dao.BeanMailToUserMapper;
import com.harmonycloud.zeus.dao.user.BeanUserMapper;
import com.harmonycloud.zeus.integration.cluster.PrometheusWrapper;
import com.harmonycloud.zeus.integration.cluster.bean.MiddlewareCluster;
import com.harmonycloud.zeus.integration.cluster.bean.prometheus.PrometheusRule;
import com.harmonycloud.zeus.integration.cluster.bean.prometheus.PrometheusRuleGroups;
import com.harmonycloud.zeus.integration.cluster.bean.prometheus.PrometheusRules;
import com.harmonycloud.zeus.service.k8s.MiddlewareClusterService;
import com.harmonycloud.zeus.service.k8s.PrometheusRuleService;
import com.harmonycloud.zeus.service.middleware.MiddlewareAlertsService;
import com.harmonycloud.zeus.service.middleware.MiddlewareService;
import com.harmonycloud.zeus.service.registry.HelmChartService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.yaml.snakeyaml.Yaml;

import java.math.BigDecimal;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.harmonycloud.caas.common.constants.AlertConstant.*;

/**
 * @author xutianhong
 * @Date 2021/4/26 10:23 上午
 */
@Service
@Slf4j
public class MiddlewareAlertsServiceImpl implements MiddlewareAlertsService {

    @Autowired
    private PrometheusWrapper prometheusWrapper;
    @Autowired
    private PrometheusRuleService prometheusRuleService;
    @Autowired
    private HelmChartService helmChartService;
    @Autowired
    private BeanAlertRuleMapper beanAlertRuleMapper;
    @Autowired
    private MiddlewareService middlewareService;
    @Autowired
    private AlertRuleIdMapper alertRuleIdMapper;
    @Autowired
    private BeanMailToUserMapper beanMailToUserMapper;
    @Autowired
    private MiddlewareClusterService clusterService;
    @Autowired
    private BeanAlertSettingMapper alertSettingMapper;
    @Autowired
    private BeanUserMapper  userMapper;

    @Override
    public PageInfo<MiddlewareAlertsDTO> listUsedRules(String clusterId, String namespace, String middlewareName,
                                                       String lay, String keyword) {
        QueryWrapper<AlertRuleId> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("lay",lay);
        if(!"system".equals(lay)) {
            if (StringUtils.isNotEmpty(namespace) && StringUtils.isNotEmpty(middlewareName)) {
                queryWrapper.eq("cluster_id",clusterId).eq("namespace",namespace).eq("middleware_name",middlewareName);
            }else {
                queryWrapper.eq("cluster_id",clusterId).isNotNull("namespace").isNotNull("middleware_name");
            }
        }
        if (StringUtils.isNotEmpty(keyword)) {
            String alertID = keyword.replaceAll("GJ","");
            if (isNumeric(alertID)) {
                queryWrapper.and(wrapper ->
                        wrapper.like("alert_id",Integer.parseInt(alertID)).or().like("alert_expr",keyword));
            } else {
                queryWrapper.and(wrapper ->
                        wrapper.or().eq("alert",keyword).or().eq("symbol",keyword)
                                .or().like("threshold",keyword)
                                .or().eq("alert_time",keyword).or().eq("alert_times",keyword)
                                .or().eq("content",keyword).or().eq("description",keyword)
                                .or().like("alert_expr",keyword)
                );
            }
        }
        List<AlertRuleId> alertInfos = alertRuleIdMapper.selectList(queryWrapper);
        PageInfo<MiddlewareAlertsDTO> alertsDTOPageInfo = new PageInfo<>();
        BeanUtils.copyProperties(new PageInfo<>(alertInfos),alertsDTOPageInfo);
        alertsDTOPageInfo.setList(alertInfos.stream().map(alertInfo -> {
            MiddlewareAlertsDTO middlewareAlertsDTO = new MiddlewareAlertsDTO();
            BeanUtils.copyProperties(alertInfo,middlewareAlertsDTO);
            Map<String, String > labels = JSON.parseObject(alertInfo.getLabels(), HashMap.class);
            Map<String, String> annotations = JSON.parseObject(alertInfo.getAnnotations(), HashMap.class);
            middlewareAlertsDTO.setLabels(labels);
            middlewareAlertsDTO.setAnnotations(annotations);
            middlewareAlertsDTO.setAlertId(calculateID(alertInfo.getAlertId()));
            middlewareAlertsDTO.setNickname(convertCluster(alertInfo.getClusterId()));
            return middlewareAlertsDTO;
        }).filter(alertInfo -> !filterExpr(alertInfo.getExpr())).collect(Collectors.toList()));
        alertsDTOPageInfo.getList().sort(
                (o1, o2) -> o1.getCreateTime() == null ? -1 : o2.getCreateTime() == null ? -1 : o2.getCreateTime().compareTo(o1.getCreateTime()));

        return alertsDTOPageInfo;
    }

    private String convertCluster(String clusterId) {
        List<MiddlewareCluster> clusters = clusterService.listClusters(clusterId);
        if (!CollectionUtils.isEmpty(clusters)) {
            return clusters.get(0).getMetadata().getAnnotations().get(NameConstant.NAME);
        }
        return null;
    }

    @Override
    public List<MiddlewareAlertsDTO> listRules(String clusterId, String namespace, String middlewareName, String type) {
        String data;
        Middleware middleware = middlewareService.detail(clusterId, namespace, middlewareName, type);
        try {
            QueryWrapper<BeanAlertRule> wrapper = new QueryWrapper<BeanAlertRule>().eq("chart_name", type)
                    .eq("chart_version", middleware.getChartVersion());
            data = beanAlertRuleMapper.selectOne(wrapper).getAlert();
        } catch (Exception e) {
            log.info("数据库中无此类中间件告警规则，开始数据同步");
            try {
                HelmChartFile helmChart = helmChartService.getHelmChartFromMysql(type, middleware.getChartVersion());
                data = updateAlerts2Mysql(helmChart, false);
            } catch (Exception ee) {
                throw new CaasRuntimeException(ErrorMessage.PROMETHEUS_RULES_NOT_EXIST);
            }
        }
        PrometheusRule prometheusRule = JSONObject.parseObject(data, PrometheusRule.class);
        List<PrometheusRules> rules = new ArrayList<>();
        // 获取所有规则
        for (PrometheusRuleGroups prometheusRuleGroups : prometheusRule.getSpec().getGroups()) {
            // 记录group组
            prometheusRuleGroups.getRules().forEach(rule -> {
                if (!StringUtils.isEmpty(rule.getAlert())) {
                    rule.getAnnotations().put("group", prometheusRuleGroups.getName());
                }
            });
            rules.addAll(prometheusRuleGroups.getRules());
        }
        rules.removeIf(rule -> StringUtils.isEmpty(rule.getAlert()));
        // 封装数据
        List<MiddlewareAlertsDTO> middlewareAlertsDTOList = new ArrayList<>();
        rules.forEach(rule -> {
            if (filterExpr(rule.getExpr())) {
                return;
            }
            MiddlewareAlertsDTO middlewareAlertsDTO = new MiddlewareAlertsDTO();
            BeanUtils.copyProperties(rule, middlewareAlertsDTO);
            middlewareAlertsDTO.setDescription(rule.getAnnotations().containsKey("description")
                && !type.equals(MiddlewareTypeEnum.POSTGRESQL.getType()) ? rule.getAnnotations().get("description")
                    : rule.getAlert());
            middlewareAlertsDTO.setUnit(rule.getAnnotations().getOrDefault("unit", ""));
            middlewareAlertsDTO.setType(type);
            middlewareAlertsDTOList.add(middlewareAlertsDTO);
        });
        return middlewareAlertsDTOList;
    }

    @Override
    public void createRules(String clusterId, String namespace, String middlewareName,
                            List<MiddlewareAlertsDTO> middlewareAlertsDTOList) {
        //告警规则入库
        middlewareAlertsDTOList.stream().forEach(middlewareAlertsDTO -> {
            //String prometheusRulesName = "postgresql".equals(middlewareAlertsDTO.getType()) ? "harmonycloud-" + middlewareName : middlewareName;
            middlewareAlertsDTO.setAlert(middlewareAlertsDTO.getAlert() + "-" + UUIDUtils.get8UUID());
            updateServiceAlerts2Prometheus(clusterId, namespace, middlewareName, middlewareName, middlewareAlertsDTO);
            addAlerts2Sql(clusterId, namespace, middlewareName, middlewareAlertsDTO);
        });
    }

    @Override
    public void deleteRules(String clusterId, String namespace, String middlewareName, String alert, String alertRuleId) {
        QueryWrapper<AlertRuleId> wrapper = new QueryWrapper<>();
        wrapper.eq("alert_id", analysisID(alertRuleId));
        AlertRuleId info = alertRuleIdMapper.selectOne(wrapper);
/*        // 特殊处理pg
        if ("postgresql".equals(info.getType())){
            middlewareName = "harmonycloud-" + middlewareName;
        }*/
        // 获取cr
        PrometheusRule prometheusRule = prometheusRuleService.get(clusterId, namespace, middlewareName);
        prometheusRule.getSpec().getGroups().forEach(prometheusRuleGroups -> {
            prometheusRuleGroups.getRules().removeIf(prometheusRules -> !StringUtils.isEmpty(prometheusRules.getAlert())
                    && prometheusRules.getAlert().equals(alert));
        });
        prometheusRuleService.update(clusterId, prometheusRule);
        QueryWrapper<AlertRuleId> deleteWrapper = new QueryWrapper<>();
        deleteWrapper.eq("alert",alert);
        alertRuleIdMapper.delete(deleteWrapper);
    }

    @Override
    public void updateRules(String clusterId, String namespace, String middlewareName,
                            String ding, String alertRuleId,
                            AlertUserDTO alertUserDTO) {
        MiddlewareAlertsDTO middlewareAlertsDTO = alertUserDTO.getMiddlewareAlertsDTO();
        QueryWrapper<AlertRuleId> wrapper = new QueryWrapper<>();
        wrapper.eq("alert_id", analysisID(middlewareAlertsDTO.getAlertId()));
        AlertRuleId info = alertRuleIdMapper.selectOne(wrapper);
        String group = middlewareAlertsDTO.getAnnotations().get("group");
        // 特殊处理pg
        //String prometheusRulesName = "postgresql".equals(info.getType()) ? "harmonycloud-" + middlewareName : middlewareName;
        updateServiceAlerts2Prometheus(clusterId, namespace, middlewareName, middlewareName, middlewareAlertsDTO);
        updateAlerts2Mysql(clusterId, namespace, middlewareName, middlewareAlertsDTO);
    }

    @Override
    public void createSystemRule(String clusterId, String ding, AlertsUserDTO alertsUserDTO) {
        alertsUserDTO.getMiddlewareAlertsDTOList().forEach(middlewareAlertsDTO -> {
            //构建执行规则
            String expr = buildExpr(middlewareAlertsDTO);
            middlewareAlertsDTO.setExpr(expr);
            // 写入通道沉默时间
            middlewareAlertsDTO.getAnnotations().put("silence", middlewareAlertsDTO.getSilence());
            // 写入创建时间
            middlewareAlertsDTO.getAnnotations().put("createTime",
                    DateUtils.dateToString(new Date(), DateUtils.YYYY_MM_DD_T_HH_MM_SS_Z));
            middlewareAlertsDTO.getAnnotations().put("product", "harmonycloud");
            middlewareAlertsDTO.getAnnotations().put("group", "memory-used");
            middlewareAlertsDTO.getAnnotations().put("name", "memory-used");
            middlewareAlertsDTO.getAnnotations().put("message",middlewareAlertsDTO.getContent());
            middlewareAlertsDTO.getAnnotations().put("summary",buildSummary(middlewareAlertsDTO));
            // 写入集群
            middlewareAlertsDTO.getLabels().put("clusterId", clusterId);
            middlewareAlertsDTO.getLabels().put("namespace",NameConstant.MONITORING);
            middlewareAlertsDTO.getLabels().put("service",NameConstant.PROMETHEUS_K8S_RULES);
            middlewareAlertsDTO.setLay(NameConstant.SYSTEM);

            String time = middlewareAlertsDTO.getAlertTime().divide(middlewareAlertsDTO.getAlertTimes(),0, BigDecimal.ROUND_UP).toString();
            middlewareAlertsDTO.setTime(time);
            //告警规则入库
            int alertId = addAlerts2Sql(clusterId, NameConstant.MONITORING, NameConstant.PROMETHEUS_K8S_RULES, middlewareAlertsDTO);
            QueryWrapper<AlertRuleId> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("alert_id", alertId);
            AlertRuleId alertRuleId = alertRuleIdMapper.selectOne(queryWrapper);
            updateAlerts2Prometheus(clusterId, middlewareAlertsDTO, alertRuleId);
        });
    }

    @Override
    public void deleteSystemRules(String clusterId, String alert, String alertRuleId) {
        deletePrometheusRules(clusterId,alert);
        QueryWrapper<AlertRuleId> wrapper = new QueryWrapper<>();
        wrapper.eq("alert",alert);
        alertRuleIdMapper.delete(wrapper);
    }

    /**
     * 从prometheusRule删除规则
     * @param clusterId
     * @param alert
     */
    public void deletePrometheusRules(String clusterId, String alert) {
        // 获取cr
        PrometheusRule prometheusRule = prometheusRuleService.get(clusterId, NameConstant.MONITORING, NameConstant.PROMETHEUS_K8S_RULES);
        boolean status = false;
        for (PrometheusRuleGroups prometheusRuleGroups : prometheusRule.getSpec().getGroups()) {
            prometheusRuleGroups.getRules().removeIf(prometheusRules -> !StringUtils.isEmpty(prometheusRules.getAlert())
                    && prometheusRules.getAlert().equals(alert));
            if ("memory-used".equals(prometheusRuleGroups.getName()) && prometheusRuleGroups.getRules().size() == 0) {
                status = true;
            }
        }
        if (status) {
            prometheusRule.getSpec().getGroups().removeIf(prometheusRuleGroups ->
                    "memory-used".equals(prometheusRuleGroups.getName())
            );
        }
        prometheusRuleService.update(clusterId, prometheusRule);
    }

    @Override
    public void updateSystemRules(String clusterId, String ding, String alertRuleId, AlertUserDTO alertUserDTO) {
        MiddlewareAlertsDTO middlewareAlertsDTO = alertUserDTO.getMiddlewareAlertsDTO();
        QueryWrapper<AlertRuleId> wrapper = new QueryWrapper<>();
        wrapper.eq("alert_id", analysisID(middlewareAlertsDTO.getAlertId()));
        AlertRuleId info = alertRuleIdMapper.selectOne(wrapper);
        updateAlerts2Prometheus(clusterId, middlewareAlertsDTO, info);
        updateAlerts2Mysql(clusterId,NameConstant.MONITORING,NameConstant.PROMETHEUS_K8S_RULES,middlewareAlertsDTO);
    }

    @Override
    public MiddlewareAlertsDTO alertRuleDetail(String alertRuleId) {
        QueryWrapper<AlertRuleId> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("alert_id",analysisID(alertRuleId));
        AlertRuleId middlewareAlertInfo = alertRuleIdMapper.selectById(analysisID(alertRuleId));
        MiddlewareAlertsDTO middlewareAlertsDTO = new MiddlewareAlertsDTO();
        BeanUtils.copyProperties(middlewareAlertInfo,middlewareAlertsDTO);
        Map<String, String > labels = JSON.parseObject(middlewareAlertInfo.getLabels(), HashMap.class);
        Map<String, String> annotations = JSON.parseObject(middlewareAlertInfo.getAnnotations(), HashMap.class);
        middlewareAlertsDTO.setLabels(labels);
        middlewareAlertsDTO.setAnnotations(annotations);
        middlewareAlertsDTO.setAlertId(calculateID(middlewareAlertInfo.getAlertId()));
        return middlewareAlertsDTO;
    }

    /**
     * 更新服务告警至prometheus
     * @param clusterId
     * @param namespace
     * @param middlewareName
     * @param prometheusRulesName
     * @param middlewareAlertsDTO
     */
    private void updateServiceAlerts2Prometheus(String clusterId, String namespace, String middlewareName,
                                                String prometheusRulesName, MiddlewareAlertsDTO middlewareAlertsDTO) {
        //获取cr
        PrometheusRule prometheusRule = prometheusRuleService.get(clusterId, namespace, prometheusRulesName);
        //组装prometheusRule
        assemblePrometheusrule(clusterId, middlewareName, middlewareAlertsDTO, prometheusRule);
        prometheusRuleService.update(clusterId, prometheusRule);
    }

    /**
     * 更新系统告警规则至prometheus
     * @param clusterId
     * @param middlewareAlertsDTO
     * @param alertRuleId
     */
    public void updateAlerts2Prometheus(String clusterId, MiddlewareAlertsDTO middlewareAlertsDTO, AlertRuleId alertRuleId) {
        //获取cr
        PrometheusRule prometheusRule = prometheusRuleService.get(clusterId, NameConstant.MONITORING, NameConstant.PROMETHEUS_K8S_RULES);
        //组装prometheusRule
        if (!alertRuleId.getAlert().equals(middlewareAlertsDTO.getAlert())) {
            // 修改指标时需把原来的告警规则删除掉
            boolean status = false;
            for (PrometheusRuleGroups prometheusRuleGroups : prometheusRule.getSpec().getGroups()) {
                prometheusRuleGroups.getRules().removeIf(prometheusRules -> !StringUtils.isEmpty(prometheusRules.getAlert())
                        && prometheusRules.getAlert().equals(alertRuleId.getAlert()));
                if ("memory-used".equals(prometheusRuleGroups.getName()) && prometheusRuleGroups.getRules().size() == 0) {
                    status = true;
                }
            }
            if (status) {
                prometheusRule.getSpec().getGroups().removeIf(prometheusRuleGroups ->
                        "memory-used".equals(prometheusRuleGroups.getName())
                );
            }
            prometheusRuleService.update(clusterId, prometheusRule);
            // 重新构建一条规则
            String expr = buildExpr(middlewareAlertsDTO);
            middlewareAlertsDTO.setAlert(alertRuleId.getAlert());
            middlewareAlertsDTO.setExpr(expr);
            middlewareAlertsDTO.getLabels().put("namespace", NameConstant.MONITORING);
            middlewareAlertsDTO.getLabels().put("service", NameConstant.PROMETHEUS_K8S_RULES);
            middlewareAlertsDTO.getLabels().put("alertname", alertRuleId.getAlert());
        }
        assemblePrometheusrule(clusterId, NameConstant.PROMETHEUS_K8S_RULES, middlewareAlertsDTO, prometheusRule);
        prometheusRuleService.update(clusterId, prometheusRule);
    }

    public void updateAlerts2Mysql(String clusterId, String namespace, String middlewareName, MiddlewareAlertsDTO middlewareAlertsDTO) {
        AlertRuleId alertRuleId = new AlertRuleId();
        BeanUtils.copyProperties(middlewareAlertsDTO, alertRuleId);
        int alertId = analysisID(middlewareAlertsDTO.getAlertId());
        alertRuleId.setAlertId(alertId);
        alertRuleId.setClusterId(clusterId);
        alertRuleId.setNamespace(namespace);
        alertRuleId.setMiddlewareName(middlewareName);
        alertRuleId.setAnnotations(JSONUtil.toJsonStr(middlewareAlertsDTO.getAnnotations()));
        alertRuleId.setLabels(JSONUtil.toJsonStr(middlewareAlertsDTO.getLabels()));
        alertRuleId.setName(clusterId);
        String expr = middlewareAlertsDTO.getDescription() +middlewareAlertsDTO.getSymbol()
               + middlewareAlertsDTO.getThreshold() + "%"  + "且" + middlewareAlertsDTO.getAlertTime()
                + "分钟内触发" + middlewareAlertsDTO.getAlertTimes() + "次";
        alertRuleId.setAlertExpr(expr);
        alertRuleIdMapper.updateById(alertRuleId);
    }
    /**
     * 构建执行规则
     */
    private String buildExpr(MiddlewareAlertsDTO middlewareAlertsDTO) {
        String expr = "";
        if (CPU_USING_RATE.equals(middlewareAlertsDTO.getAlert())) {
            expr =
                "sum(sum(irate(node_cpu_seconds_total{mode!=\"idle\"}[5m])) by (kubernetes_pod_node_name))/sum(count(node_cpu_seconds_total{ mode='system'}) by (kubernetes_pod_node_name)) * 100 ";
        } else if (MEMORY_USING_RATE.equals(middlewareAlertsDTO.getAlert())) {
            expr =
                "sum(((node_memory_MemTotal_bytes - node_memory_MemFree_bytes - node_memory_Cached_bytes - node_memory_Buffers_bytes - node_memory_Slab_bytes)/1024/1024/1024))"
                    + "/sum(node_memory_MemTotal_bytes/1024/1024/1024) * 100 ";
        } else if (PVC_USING_RATE.equals(middlewareAlertsDTO.getAlert())) {
            expr =
                "sum(kubelet_volume_stats_used_bytes) / sum(kube_persistentvolumeclaim_resource_requests_storage_bytes) * 100 ";
        }
        expr = expr + middlewareAlertsDTO.getSymbol() + " " + middlewareAlertsDTO.getThreshold();
        return expr;
    }

    private String buildSummary(MiddlewareAlertsDTO middlewareAlertsDTO) {
        String summary = "";
        if (CPU_USING_RATE.equals(middlewareAlertsDTO.getAlert())) {
            summary = "node CPU alert warning";
        } else if (MEMORY_USING_RATE.equals(middlewareAlertsDTO.getAlert())) {
            summary = "node memory alert warning";
        } else if (PVC_USING_RATE.equals(middlewareAlertsDTO.getAlert())) {
            summary = "node pv alert warning";
        }
        return summary;
    }
    /**
     * 更新告警规则至数据库
     */
    @Override
    public String updateAlerts2Mysql(HelmChartFile helmChart) {
        QueryWrapper<BeanAlertRule> wrapper = new QueryWrapper<BeanAlertRule>()
                .eq("chart_name", helmChart.getChartName()).eq("chart_version", helmChart.getChartVersion());
        BeanAlertRule alertRule = beanAlertRuleMapper.selectOne(wrapper);
        return updateAlerts2Mysql(helmChart, !ObjectUtil.isEmpty(alertRule));
    }

    /**
     * 更新告警规则至数据库
     */
    @Override
    public String updateAlerts2Mysql(HelmChartFile helmChart, Boolean update) {
        JSONObject data = new JSONObject();
        for (String key : helmChart.getYamlFileMap().keySet()) {
            if (helmChart.getYamlFileMap().get(key).contains("PrometheusRule")) {
                Yaml yaml = new Yaml();
                data = yaml.loadAs(changeYaml(helmChart.getYamlFileMap().get(key)), JSONObject.class);
            }
        }
        BeanAlertRule beanAlertRule = new BeanAlertRule();
        beanAlertRule.setChartName(helmChart.getChartName());
        beanAlertRule.setChartVersion(helmChart.getChartVersion());
        beanAlertRule.setAlert(JSONObject.toJSONString(data));
        if (update) {
            QueryWrapper<BeanAlertRule> wrapper = new QueryWrapper<BeanAlertRule>()
                    .eq("chart_name", helmChart.getChartName()).eq("chart_version", helmChart.getChartVersion());
            beanAlertRuleMapper.update(beanAlertRule, wrapper);
        } else {
            beanAlertRuleMapper.insert(beanAlertRule);
        }
        return JSONObject.toJSONString(data);
    }

    @Override
    public void saveServiceAlertSetting(AlertSettingDTO alertSettingDTO) {
        BeanAlertSetting alertSetting;
        QueryWrapper<BeanAlertSetting> alertSettingWrapper = new QueryWrapper<>();
        alertSettingWrapper.eq("cluster_id", alertSettingDTO.getClusterId());
        alertSettingWrapper.eq("namespace", alertSettingDTO.getNamespace());
        alertSettingWrapper.eq("middleware_name", alertSettingDTO.getMiddlewareName());
        alertSetting = alertSettingMapper.selectOne(alertSettingWrapper);
        if (alertSetting == null) {
            alertSetting = new BeanAlertSetting();
            alertSetting.setClusterId(alertSettingDTO.getClusterId());
            alertSetting.setNamespace(alertSettingDTO.getNamespace());
            alertSetting.setMiddlewareName(alertSettingDTO.getMiddlewareName());
        }
        alertSetting.setEnableDingAlert(alertSettingDTO.getEnableDingAlert().toString());
        alertSetting.setEnableMailAlert(alertSettingDTO.getEnableMailAlert().toString());
        alertSetting.setLay(LAY_SERVICE);
        if (alertSetting.getId() == null) {
            alertSettingMapper.insert(alertSetting);
        } else {
            alertSettingMapper.updateById(alertSetting);
        }
        saveMailAlertReceiver(alertSettingDTO.getUserIds(), alertSetting.getId());
    }

    @Override
    public void saveSystemAlertSetting(AlertSettingDTO alertSettingDTO) {
        BeanAlertSetting alertSetting;
        QueryWrapper<BeanAlertSetting> alertSettingWrapper = new QueryWrapper<>();
        alertSettingWrapper.eq("lay", LAY_SYSTEM);
        alertSetting = alertSettingMapper.selectOne(alertSettingWrapper);
        if (alertSetting == null) {
            alertSetting = new BeanAlertSetting();
        }
        alertSetting.setEnableMailAlert(alertSettingDTO.getEnableMailAlert().toString());
        alertSetting.setEnableDingAlert(alertSettingDTO.getEnableDingAlert().toString());
        alertSetting.setLay(LAY_SYSTEM);
        if (alertSetting.getId() == null) {
            alertSettingMapper.insert(alertSetting);
        } else {
            alertSettingMapper.updateById(alertSetting);
        }
        // 保存用户信息
        saveMailAlertReceiver(alertSettingDTO.getUserIds(), alertSetting.getId());
    }

    @Override
    public AlertSettingDTO queryAlertSetting(String... args) {
        QueryWrapper<BeanAlertSetting> alertSettingWrapper = new QueryWrapper<>();
        if (args.length == 0) {
            alertSettingWrapper.eq("lay", LAY_SYSTEM);
        } else if (args.length == 3) {
            alertSettingWrapper.eq("cluster_id", args[0]);
            alertSettingWrapper.eq("namespace", args[1]);
            alertSettingWrapper.eq("middleware_name", args[2]);
        } else {
            return null;
        }
        BeanAlertSetting alertSetting = alertSettingMapper.selectOne(alertSettingWrapper);
        if(alertSetting == null){
            return null;
        }
        AlertSettingDTO alertSettingDTO = new AlertSettingDTO();
        alertSettingDTO.setEnableMailAlert(Boolean.parseBoolean(alertSetting.getEnableMailAlert()));
        alertSettingDTO.setEnableDingAlert(Boolean.parseBoolean(alertSetting.getEnableDingAlert()));
        alertSettingDTO.setUserList(queryMailAlertReceiver(alertSetting.getId()));
        return alertSettingDTO;
    }

    /**
     * 查询邮箱告警接受人
     * @param alertSettingId
     * @return
     */
    private List<UserDto> queryMailAlertReceiver(int alertSettingId) {
        QueryWrapper<BeanMailToUser> mailToUserQuery = new QueryWrapper<>();
        mailToUserQuery.eq("alert_setting_id", alertSettingId);
        List<BeanMailToUser> userIds = beanMailToUserMapper.selectList(mailToUserQuery);
        if (CollectionUtils.isEmpty(userIds)) {
            return Collections.emptyList();
        }
        return userMapper.selectUserList(userIds);
    }

    /**
     * 保存邮箱告警接受人
     * @param userIds
     * @param alertSettingId
     */
    private void saveMailAlertReceiver(Set<Integer> userIds, int alertSettingId) {
        if (!CollectionUtils.isEmpty(userIds)) {
            QueryWrapper<BeanMailToUser> oldUserWrapper = new QueryWrapper<>();
            oldUserWrapper.eq("alert_setting_id", alertSettingId);
            Set<Integer> oldUsers = beanMailToUserMapper.selectList(oldUserWrapper).stream().map(BeanMailToUser::getUserId).collect(Collectors.toSet());
            Set<Integer> oldUsersCopy = new HashSet<>(oldUsers);
            // oldUsers移除所有接受人，剩下需要删除的
            oldUsers.removeAll(userIds);
            // oldUsersCopy移除所有需要删除的，剩下不需要删除的
            oldUsersCopy.remove(oldUsers);
            // 将移除的用户从数据库删除
            oldUsers.forEach(userId -> {
                QueryWrapper<BeanMailToUser> queryWrapper = new QueryWrapper<>();
                queryWrapper.eq("alert_setting_id", alertSettingId);
                queryWrapper.eq("user_id", userId);
                beanMailToUserMapper.delete(queryWrapper);
            });
            // 添加用户
            userIds.forEach(userId -> {
                if (!oldUsersCopy.contains(userId)) {
                    BeanMailToUser mailToUser = new BeanMailToUser();
                    mailToUser.setUserId(userId);
                    mailToUser.setAlertSettingId(alertSettingId);
                    beanMailToUserMapper.insert(mailToUser);
                }
            });
        } else {
            QueryWrapper<BeanMailToUser> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("alert_setting_id", alertSettingId);
            beanMailToUserMapper.delete(queryWrapper);
        }
    }

    /**
     * 从prometheus中获取规则
     */
    public List<PrometheusGroups> getPrometheusRule(String clusterId, String namespace, String middlewareName)
            throws Exception {
        PrometheusRulesResponse prometheusRulesResponse =
                prometheusWrapper.getRules(clusterId, NameConstant.PROMETHEUS_API_VERSION_RULES);
        return prometheusRulesResponse.getData().getGroups().stream()
                .filter(prometheusGroups -> prometheusGroups.getFile().contains(namespace + "-" + middlewareName + ".yaml"))
                .collect(Collectors.toList());
    }

    /**
     * 组装PrometheusRules
     */
    public PrometheusRules convertPrometheusRules(MiddlewareAlertsDTO middlewareAlertsDTO, String clusterId) {
        String time = "";
        if (middlewareAlertsDTO.getAlertTime() != null && middlewareAlertsDTO.getAlertTimes() != null) {
            time = String.valueOf(middlewareAlertsDTO.getAlertTime().divide(middlewareAlertsDTO.getAlertTimes(),0, BigDecimal.ROUND_UP));
            time.replace("\"","");
        } else if (middlewareAlertsDTO.getTime() != null) {
            time = middlewareAlertsDTO.getTime();
        }
        middlewareAlertsDTO.setTime(time + "m");
        PrometheusRules prometheusRules =
                new PrometheusRules().setAlert(middlewareAlertsDTO.getAlert())
                        .setTime(middlewareAlertsDTO.getTime()).setLabels(middlewareAlertsDTO.getLabels())
                        .setAnnotations(middlewareAlertsDTO.getAnnotations());
        // 替换{{``}}
        if (prometheusRules.getAnnotations().containsKey("summary")) {
            prometheusRules.getAnnotations().put("summary",
                    replaceValue(prometheusRules.getAnnotations().get("summary")));
        }
        if (prometheusRules.getAnnotations().containsKey("message")) {
            prometheusRules.getAnnotations().put("message",
                    replaceValue(prometheusRules.getAnnotations().get("message")));
        }
        if (prometheusRules.getLabels().containsKey("value")) {
            prometheusRules.getLabels().put("value", replaceValue(prometheusRules.getLabels().get("value")));
        }
        // 写入通道沉默时间
        prometheusRules.getAnnotations().put("silence", middlewareAlertsDTO.getSilence());
        // 写入创建时间
        prometheusRules.getAnnotations().put("createTime",
                DateUtils.dateToString(new Date(), DateUtils.YYYY_MM_DD_T_HH_MM_SS_Z));
        // 写入集群
        prometheusRules.getLabels().put("clusterId", clusterId);
        // 构造expr
        String expr = "";
        if ("kafka".equals(middlewareAlertsDTO.getType())) {
            expr = middlewareAlertsDTO.getExpr().replace(
                    "{{ include \"" + middlewareAlertsDTO.getType() + "-hc" + ".fullname\" . }}", middlewareAlertsDTO.getName());
        } else {
            expr = middlewareAlertsDTO.getExpr().replace(
                    "{{ include \"" + middlewareAlertsDTO.getType() + ".fullname\" . }}", middlewareAlertsDTO.getName());
        }
                String symbol = getSymbol(expr);
        String threshold = getThreshold(expr);
        expr = expr.replace(symbol, middlewareAlertsDTO.getSymbol()).replace(threshold,
                middlewareAlertsDTO.getThreshold());
        prometheusRules.setExpr(expr);
        return prometheusRules;
    }

    /**
     * 添加告警规则至数据库
     */
    public int addAlerts2Sql(String clusterId, String namespace, String middlewareName,
                              MiddlewareAlertsDTO middlewareAlertsDTO) {
        Date date = new Date();
        AlertRuleId alertRuleId = new AlertRuleId();
        String alert = middlewareAlertsDTO.getAlert() + "-" + UUIDUtils.get8UUID();
        if ("system".equals(middlewareAlertsDTO.getLay())) {
            middlewareAlertsDTO.getLabels().put("alertname", alert);
        } else {
            middlewareAlertsDTO.getLabels().put("namespace", namespace);
            middlewareAlertsDTO.getLabels().put("service", middlewareName);
            middlewareAlertsDTO.getLabels().put("middleware", middlewareAlertsDTO.getType());
        }
        BeanUtils.copyProperties(middlewareAlertsDTO, alertRuleId);
        alertRuleId.setAnnotations(JSONUtil.toJsonStr(middlewareAlertsDTO.getAnnotations()));
        alertRuleId.setLabels(JSONUtil.toJsonStr(middlewareAlertsDTO.getLabels()));
        alertRuleId.setClusterId(clusterId);
        alertRuleId.setNamespace(namespace);
        alertRuleId.setMiddlewareName(middlewareName);
        alertRuleId.setCreateTime(date);
        alertRuleId.setName(clusterId);
        alertRuleId.setAlert(alert);
        if (MiddlewareTypeEnum.ZOOKEEPER.getType().equals(middlewareAlertsDTO.getType())) {
            alertRuleId.setDescription(middlewareAlertsDTO.getAlert());
        }
        String expr = alertRuleId.getDescription() + middlewareAlertsDTO.getSymbol()
                + middlewareAlertsDTO.getThreshold() + "%" + "且" + middlewareAlertsDTO.getAlertTime()
                + "分钟内触发" + middlewareAlertsDTO.getAlertTimes() + "次";
        alertRuleId.setAlertExpr(expr);

        alertRuleIdMapper.insert(alertRuleId);
        return alertRuleId.getAlertId();
    }

    /**
     * 替换prometheusRule内容
     */
    public void assemblePrometheusrule(String clusterId, String middlewareName,
                                       MiddlewareAlertsDTO middlewareAlertsDTO,
                                       PrometheusRule prometheusRule) {
        prometheusRule.getSpec().getGroups().forEach(prometheusRuleGroups -> {
            prometheusRuleGroups.getRules().removeIf(prometheusRules -> !StringUtils.isEmpty(prometheusRules.getAlert())
                    && prometheusRules.getAlert().equals(middlewareAlertsDTO.getAlert()));
        });

        String group = middlewareAlertsDTO.getAnnotations().get("group");
        // 创建prometheusRules
        middlewareAlertsDTO.setName(middlewareName);
        PrometheusRules prometheusRules = convertPrometheusRules(middlewareAlertsDTO, clusterId);

        // 判断group组是否已存在
        if (prometheusRule.getSpec().getGroups().stream().anyMatch(prometheusRuleGroups -> prometheusRuleGroups
                .getName().equals(prometheusRules.getAnnotations().get("group")))) {
            prometheusRule.getSpec().getGroups().forEach(prometheusRuleGroups -> {
                if (prometheusRuleGroups.getName().equals(prometheusRules.getAnnotations().get("group"))) {
                    prometheusRules.getAnnotations().remove("group");
                    prometheusRuleGroups.getRules().add(prometheusRules);
                }
            });
        } else {
            PrometheusRuleGroups prometheusRuleGroups =
                    new PrometheusRuleGroups().setName(prometheusRules.getAnnotations().get("name"));
            List<PrometheusRules> prometheusRulesList = new ArrayList<>();
            prometheusRulesList.add(prometheusRules);
            prometheusRuleGroups.setRules(prometheusRulesList);
            prometheusRule.getSpec().getGroups().add(prometheusRuleGroups);
        }
        middlewareAlertsDTO.getAnnotations().put("group",group);
    }

    /**
     * 校验规则格式
     */
    public boolean checkFormat(String expr) {
        String filter = ".+[<|>|>=|<=|!=|==](\\s)?\\d+(\\.\\d+)?";
        // 匹配科学计数法
        String science = ".+[<|>|>=|<=|!=|==](\\s)?\\d+\\.\\d+e\\+\\d+";
        return Pattern.matches(filter, expr) || Pattern.matches(science, expr);
    }

    /**
     * 过滤expr
     */
    public boolean filterExpr(String expr) {
        return expr.contains(" and ") || expr.contains(" or ") || !checkFormat(expr);
    }

    /**
     * 获取符号:>,<,!=...
     */
    public String getSymbol(String expr) {
        expr = Pattern.compile(".+}").matcher(expr).replaceAll("");
        return Pattern.compile("[^<|^>|^>=|^<=|^!=|^==]").matcher(expr).replaceAll("");
    }

    /**
     * 获取阈值
     */
    public String getThreshold(String expr) {
        String symbol = getSymbol(expr);
        String[] threshold = expr.split(symbol);
        return threshold[threshold.length - 1];
    }

    public String replaceValue(String str) {
        return str.replace("{{`", "").replace("`}}", "");
    }

    /**
     * 计算周期时间
     */
    public String calculateTime(double duration) {
        double time = duration / 60;
        if (Math.ceil(time) == time) {
            return String.valueOf(time).split("\\.")[0] + "分钟";
        } else {
            return time + "分钟";
        }
    }

    /**
     * 生成一个告警规则ID
     * @param id
     */
    public String calculateID(int id) {
        String alertId = "";
        if (id < 10000) {
            alertId = alertId + "GJ" + String.format("%05d", id);
        } else {
            alertId = alertId + "GJ" + id;
        }
        return alertId;
    }

    public String createId(int id) {
        String alertId = "";
        if (id < 100000) {
            alertId = alertId + String.format("%06d", id);
        } else {
            alertId = alertId + id;
        }
        return alertId;
    }

    /**
     * 解析告警ID
     * @param id
     */
    public int analysisID(String id) {
        String alertID = id.replaceAll("GJ","");
        if (isNumeric(alertID)) {
            return Integer.parseInt(alertID);
        }
        return Integer.parseInt(alertID);
    }

    /**
     * 利用正则表达式判断字符串是否是数字
     * @param str
     * @return
     */
    public boolean isNumeric(String str){
        Pattern pattern = Pattern.compile("[0-9]*");
        Matcher isNum = pattern.matcher(str);
        if( !isNum.matches() ){
            return false;
        }
        return true;
    }

    private String changeYaml(String yaml) {
        if (yaml.indexOf("{{- end }}") == -1) {
            return yaml;
        }
        return yaml.substring(yaml.indexOf("apiVersion"),yaml.indexOf("{{- end }}"));
    }
}
