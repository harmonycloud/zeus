package com.harmonycloud.zeus.service.middleware.impl;

import java.math.BigDecimal;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import cn.hutool.json.JSONUtil;
import com.alibaba.fastjson.JSON;
import com.github.pagehelper.PageInfo;
import com.harmonycloud.caas.common.enums.middleware.MiddlewareTypeEnum;
import com.harmonycloud.caas.common.model.AlertUserDTO;
import com.harmonycloud.caas.common.model.AlertsUserDTO;
import com.harmonycloud.caas.common.model.user.UserDto;
import com.harmonycloud.zeus.bean.MailToUser;
import com.harmonycloud.zeus.bean.MiddlewareAlertInfo;
import com.harmonycloud.zeus.bean.user.BeanUser;
import com.harmonycloud.zeus.dao.MailToUserMapper;
import com.harmonycloud.zeus.dao.MiddlewareAlertInfoMapper;
import com.harmonycloud.zeus.integration.cluster.PrometheusWrapper;
import com.harmonycloud.zeus.integration.cluster.bean.prometheus.PrometheusRule;
import com.harmonycloud.zeus.integration.cluster.bean.prometheus.PrometheusRuleGroups;
import com.harmonycloud.zeus.integration.cluster.bean.prometheus.PrometheusRules;
import com.harmonycloud.zeus.service.k8s.PrometheusRuleService;
import com.harmonycloud.zeus.service.middleware.MiddlewareAlertsService;
import com.harmonycloud.zeus.service.middleware.MiddlewareService;
import com.harmonycloud.zeus.service.registry.HelmChartService;
import com.harmonycloud.zeus.service.user.DingRobotService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.Yaml;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.harmonycloud.caas.common.constants.NameConstant;
import com.harmonycloud.caas.common.enums.ErrorMessage;
import com.harmonycloud.caas.common.exception.CaasRuntimeException;
import com.harmonycloud.caas.common.model.PrometheusGroups;
import com.harmonycloud.caas.common.model.PrometheusRulesResponse;
import com.harmonycloud.caas.common.model.middleware.Middleware;
import com.harmonycloud.caas.common.model.middleware.MiddlewareAlertsDTO;
import com.harmonycloud.caas.common.model.registry.HelmChartFile;
import com.harmonycloud.zeus.bean.BeanAlertRule;
import com.harmonycloud.zeus.dao.BeanAlertRuleMapper;
import com.harmonycloud.tool.date.DateUtils;
import com.harmonycloud.tool.uuid.UUIDUtils;

import cn.hutool.core.util.ObjectUtil;
import lombok.extern.slf4j.Slf4j;

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
    private MiddlewareAlertInfoMapper middlewareAlertInfoMapper;
    @Autowired
    private MailToUserMapper mailToUserMapper;
    @Autowired
    private DingRobotService dingRobotService;

    @Override
    public PageInfo<MiddlewareAlertsDTO> listUsedRules(String clusterId, String namespace, String middlewareName,
                                                       String lay, String keyword) {
        QueryWrapper<MiddlewareAlertInfo> queryWrapper = new QueryWrapper<>();
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
        List<MiddlewareAlertInfo> alertInfos = middlewareAlertInfoMapper.selectList(queryWrapper);
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
            return middlewareAlertsDTO;
        }).collect(Collectors.toList()));
        alertsDTOPageInfo.getList().sort(
                (o1, o2) -> o1.getCreateTime() == null ? -1 : o2.getCreateTime() == null ? -1 : o2.getCreateTime().compareTo(o1.getCreateTime()));

        return alertsDTOPageInfo;
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
                    ? rule.getAnnotations().get("description") : rule.getAlert());
            middlewareAlertsDTO.setUnit(rule.getAnnotations().getOrDefault("unit", ""));
            middlewareAlertsDTO.setType(type);
            middlewareAlertsDTOList.add(middlewareAlertsDTO);
        });
        return middlewareAlertsDTOList;
    }

    @Override
    public void createRules(String clusterId, String namespace, String middlewareName,
                            String ding, AlertsUserDTO alertsUserDTO) {
        //告警规则入库
        alertsUserDTO.getMiddlewareAlertsDTOList().stream().forEach(middlewareAlertsDTO -> {
            addAlerts2Sql(clusterId,namespace,middlewareName,middlewareAlertsDTO, ding, alertsUserDTO.getUsers());
        });
    }

    @Override
    public void deleteRules(String clusterId, String namespace, String middlewareName, String alert, String alertRuleId) {
        QueryWrapper<MiddlewareAlertInfo> wrapper = new QueryWrapper<>();
        wrapper.eq("alert_id", analysisID(alertRuleId));
        MiddlewareAlertInfo info = middlewareAlertInfoMapper.selectOne(wrapper);
        // 特殊处理pg
        if ("postgresql".equals(info.getType())){
            middlewareName = "harmonycloud-" + middlewareName;
        }
        // 获取cr
        PrometheusRule prometheusRule = prometheusRuleService.get(clusterId, namespace, middlewareName);
        prometheusRule.getSpec().getGroups().forEach(prometheusRuleGroups -> {
            prometheusRuleGroups.getRules().removeIf(prometheusRules -> !StringUtils.isEmpty(prometheusRules.getAlert())
                    && prometheusRules.getAlert().equals(alert));
        });
        prometheusRuleService.update(clusterId, prometheusRule);
        QueryWrapper<MiddlewareAlertInfo> deleteWrapper = new QueryWrapper<>();
        deleteWrapper.eq("alert",alert);
        middlewareAlertInfoMapper.delete(deleteWrapper);
        removeMail(alertRuleId);
    }

    @Override
    public void updateRules(String clusterId, String namespace, String middlewareName,
                            String ding, String alertRuleId,
                            AlertUserDTO alertUserDTO) {
        MiddlewareAlertsDTO middlewareAlertsDTO = alertUserDTO.getMiddlewareAlertsDTO();
        QueryWrapper<MiddlewareAlertInfo> wrapper = new QueryWrapper<>();
        wrapper.eq("alert_id", analysisID(middlewareAlertsDTO.getAlertId()));
        MiddlewareAlertInfo info = middlewareAlertInfoMapper.selectOne(wrapper);
        String group = middlewareAlertsDTO.getAnnotations().get("group");

        // 特殊处理pg
        String prometheusRulesName = "postgresql".equals(info.getType()) ? "harmonycloud-" + middlewareName : middlewareName;

        //获取cr
        PrometheusRule prometheusRule = prometheusRuleService.get(clusterId, namespace, prometheusRulesName);
        //不启用该规则时，判断该规则是否已写入prometheusRule
        if ("0".equals(middlewareAlertsDTO.getEnable()) && "1".equals(info.getEnable())) {
            //从prometheusRule删除规则
            prometheusRule.getSpec().getGroups().forEach(prometheusRuleGroups -> {
                prometheusRuleGroups.getRules().removeIf(prometheusRules -> !StringUtils.isEmpty(prometheusRules.getAlert())
                        && prometheusRules.getAlert().equals(middlewareAlertsDTO.getAlert()));
                prometheusRuleService.update(clusterId, prometheusRule);
            });
            middlewareAlertsDTO.getAnnotations().put("group", group);
            updateAlerts2Mysql(clusterId, namespace, middlewareName, middlewareAlertsDTO);
            return;
        }
        //组装prometheusRule
        assemblePrometheusrule(clusterId, middlewareName, middlewareAlertsDTO, prometheusRule);
        prometheusRuleService.update(clusterId, prometheusRule);
        updateAlerts2Mysql(clusterId, namespace, middlewareName, middlewareAlertsDTO);
        if (!alertUserDTO.getUsers().isEmpty()) {
            List<BeanUser> beanUsers = alertUserDTO.getUsers().stream().map(userDto -> {
                BeanUser beanUser = new BeanUser();
                BeanUtils.copyProperties(userDto, beanUser);
                return beanUser;
            }).collect(Collectors.toList());
            addMail2Sql(beanUsers, ding, analysisID(alertRuleId));
        }
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

            String time = middlewareAlertsDTO.getAlertTime().divide(middlewareAlertsDTO.getAlertTimes(),0, BigDecimal.ROUND_UP).toString();
            middlewareAlertsDTO.setTime(time);
            //告警规则入库
            addAlerts2Sql(clusterId,NameConstant.MONITORING,NameConstant.PROMETHEUS_K8S_RULES,middlewareAlertsDTO,ding,alertsUserDTO.getUsers());
        });
    }

    @Override
    public void deleteSystemRules(String clusterId, String alert, String alertRuleId) {
        deletePrometheusRules(clusterId,alert);
        QueryWrapper<MiddlewareAlertInfo> wrapper = new QueryWrapper<>();
        wrapper.eq("alert",alert);
        middlewareAlertInfoMapper.delete(wrapper);
        removeMail(alertRuleId);
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
        QueryWrapper<MiddlewareAlertInfo> wrapper = new QueryWrapper<>();
        wrapper.eq("alert_id", analysisID(middlewareAlertsDTO.getAlertId()));
        MiddlewareAlertInfo info = middlewareAlertInfoMapper.selectOne(wrapper);
        //获取cr
        PrometheusRule prometheusRule = prometheusRuleService.get(clusterId, NameConstant.MONITORING, NameConstant.PROMETHEUS_K8S_RULES);
        //不启用该规则时，判断该规则是否已写入prometheusRule
        if ("0".equals(middlewareAlertsDTO.getEnable()) && "1".equals(info.getEnable())) {
            deletePrometheusRules(clusterId,middlewareAlertsDTO.getAlert());
            updateAlerts2Mysql(clusterId,NameConstant.MONITORING,NameConstant.PROMETHEUS_K8S_RULES,middlewareAlertsDTO);
            return;
        }
        //组装prometheusRule
        if (!info.getAlert().equals(middlewareAlertsDTO.getAlert())) {
            // 修改指标时需把原来的告警规则删除掉
            boolean status = false;
            for (PrometheusRuleGroups prometheusRuleGroups : prometheusRule.getSpec().getGroups()) {
                prometheusRuleGroups.getRules().removeIf(prometheusRules -> !StringUtils.isEmpty(prometheusRules.getAlert())
                        && prometheusRules.getAlert().equals(info.getAlert()));
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
            String alert = middlewareAlertsDTO.getAlert() + "-" + UUIDUtils.get8UUID();
            middlewareAlertsDTO.setAlert(alert);
            middlewareAlertsDTO.setExpr(expr);
            middlewareAlertsDTO.getLabels().put("namespace",NameConstant.MONITORING);
            middlewareAlertsDTO.getLabels().put("service",NameConstant.PROMETHEUS_K8S_RULES);
            middlewareAlertsDTO.getLabels().put("alertname",alert);
        }
        assemblePrometheusrule(clusterId,NameConstant.PROMETHEUS_K8S_RULES,middlewareAlertsDTO,prometheusRule);
        prometheusRuleService.update(clusterId, prometheusRule);
        updateAlerts2Mysql(clusterId,NameConstant.MONITORING,NameConstant.PROMETHEUS_K8S_RULES,middlewareAlertsDTO);
        if (!alertUserDTO.getUsers().isEmpty()) {
            List<BeanUser> beanUsers = alertUserDTO.getUsers().stream().map(userDto -> {
                BeanUser beanUser = new BeanUser();
                BeanUtils.copyProperties(userDto,beanUser);
                return beanUser;
            }).collect(Collectors.toList());
            addMail2Sql(beanUsers,ding,analysisID(alertRuleId));
        }
    }

    @Override
    public MiddlewareAlertsDTO alertRuleDetail(String alertRuleId) {
        QueryWrapper<MiddlewareAlertInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("alert_id",analysisID(alertRuleId));
        MiddlewareAlertInfo middlewareAlertInfo = middlewareAlertInfoMapper.selectById(analysisID(alertRuleId));
        MiddlewareAlertsDTO middlewareAlertsDTO = new MiddlewareAlertsDTO();
        BeanUtils.copyProperties(middlewareAlertInfo,middlewareAlertsDTO);
        Map<String, String > labels = JSON.parseObject(middlewareAlertInfo.getLabels(), HashMap.class);
        Map<String, String> annotations = JSON.parseObject(middlewareAlertInfo.getAnnotations(), HashMap.class);
        middlewareAlertsDTO.setLabels(labels);
        middlewareAlertsDTO.setAnnotations(annotations);
        middlewareAlertsDTO.setAlertId(calculateID(middlewareAlertInfo.getAlertId()));

        return middlewareAlertsDTO;
    }

    public void updateAlerts2Mysql(String clusterId, String namespace, String middlewareName, MiddlewareAlertsDTO middlewareAlertsDTO) {
        MiddlewareAlertInfo middlewareAlertInfo = new MiddlewareAlertInfo();
        BeanUtils.copyProperties(middlewareAlertsDTO,middlewareAlertInfo);
        int alertId = analysisID(middlewareAlertsDTO.getAlertId());
        middlewareAlertInfo.setAlertId(alertId);
        middlewareAlertInfo.setClusterId(clusterId);
        middlewareAlertInfo.setNamespace(namespace);
        middlewareAlertInfo.setMiddlewareName(middlewareName);
        middlewareAlertInfo.setAnnotations(JSONUtil.toJsonStr(middlewareAlertsDTO.getAnnotations()));
        middlewareAlertInfo.setLabels(JSONUtil.toJsonStr(middlewareAlertsDTO.getLabels()));
        middlewareAlertInfo.setName(clusterId);
        String expr = middlewareAlertsDTO.getDescription() +middlewareAlertsDTO.getSymbol()
               + middlewareAlertsDTO.getThreshold() + "%"  + "且" + middlewareAlertsDTO.getAlertTime()
                + "分钟内触发" + middlewareAlertsDTO.getAlertTimes() + "次";
        middlewareAlertInfo.setAlertExpr(expr);
        middlewareAlertInfoMapper.updateById(middlewareAlertInfo);
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
    public void addAlerts2Sql(String clusterId, String namespace, String middlewareName,
                              MiddlewareAlertsDTO middlewareAlertsDTO, String ding, List<UserDto> userDtos) {
        Date date = new Date();
        MiddlewareAlertInfo middlewareAlertInfo = new MiddlewareAlertInfo();
        String alert = middlewareAlertsDTO.getAlert() + "-" + UUIDUtils.get8UUID();
        if ("system".equals(middlewareAlertsDTO.getLay())) {
            middlewareAlertsDTO.getLabels().put("alertname", alert);
        } else {
            middlewareAlertsDTO.getLabels().put("namespace", namespace);
            middlewareAlertsDTO.getLabels().put("service", middlewareName);
            middlewareAlertsDTO.getLabels().put("middleware", middlewareAlertsDTO.getType());
        }
        BeanUtils.copyProperties(middlewareAlertsDTO, middlewareAlertInfo);
        middlewareAlertInfo.setAnnotations(JSONUtil.toJsonStr(middlewareAlertsDTO.getAnnotations()));
        middlewareAlertInfo.setLabels(JSONUtil.toJsonStr(middlewareAlertsDTO.getLabels()));
        middlewareAlertInfo.setClusterId(clusterId);
        middlewareAlertInfo.setNamespace(namespace);
        middlewareAlertInfo.setMiddlewareName(middlewareName);
        middlewareAlertInfo.setCreateTime(date);
        middlewareAlertInfo.setName(clusterId);
        middlewareAlertInfo.setAlert(alert);
        if (MiddlewareTypeEnum.ZOOKEEPER.getType().equals(middlewareAlertsDTO.getType())) {
            middlewareAlertInfo.setDescription(middlewareAlertsDTO.getAlert());
        }
        String expr = middlewareAlertInfo.getDescription() + middlewareAlertsDTO.getSymbol()
                + middlewareAlertsDTO.getThreshold() + "%" + "且" + middlewareAlertsDTO.getAlertTime()
                + "分钟内触发" + middlewareAlertsDTO.getAlertTimes() + "次";
        middlewareAlertInfo.setAlertExpr(expr);

        middlewareAlertInfoMapper.insert(middlewareAlertInfo);
        List<BeanUser> beanUsers = userDtos.stream().map(userDto -> {
            BeanUser beanUser = new BeanUser();
            BeanUtils.copyProperties(userDto, beanUser);
            return beanUser;
        }).collect(Collectors.toList());
        addMail2Sql(beanUsers, ding, middlewareAlertInfo.getAlertId());
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
     * 选择邮箱被通知人
     */
    public void addMail2Sql(List<BeanUser> users, String ding, int alertRuleId) {
        QueryWrapper<MailToUser> mailToUserQueryWrapper = new QueryWrapper<>();
        mailToUserQueryWrapper.eq("alert_rule_id",alertRuleId);
        mailToUserMapper.delete(mailToUserQueryWrapper);
        users.stream().forEach(user -> {
            MailToUser mailToUser = new MailToUser();
            mailToUser.setUserId(user.getId());
            mailToUser.setAlertRuleId(alertRuleId);
            mailToUserMapper.insert(mailToUser);
        });
        if (StringUtils.isNotEmpty(ding)) {
            dingRobotService.enableDing();
        }
    }

    /**
     * 移除邮箱被通知人
     */
    public void removeMail(String alertRuleId) {
        QueryWrapper<MailToUser> mailToUserQueryWrapper = new QueryWrapper<>();
        Integer alertId = analysisID(alertRuleId);
        mailToUserQueryWrapper.eq("alert_rule_id",alertId);
        mailToUserMapper.delete(mailToUserQueryWrapper);
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
    public static void main(String[] args){
        String a = "1";
        String b = "2";
        String c = "3";
        String d = null;
        String f = null;
        List<String> test = new ArrayList<>();
        test.add(b);
        test.add(d);
        test.add(f);
        test.add(c);
        test.add(a);
        test.sort((o1,o2) -> o1 == null ? 1 : o2 == null ? -1 : o2.compareTo(o1));
    }
}
