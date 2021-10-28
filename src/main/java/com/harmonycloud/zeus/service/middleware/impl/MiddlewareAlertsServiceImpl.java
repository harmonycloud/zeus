package com.harmonycloud.zeus.service.middleware.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.harmonycloud.zeus.integration.cluster.PrometheusWrapper;
import com.harmonycloud.zeus.integration.cluster.bean.prometheus.PrometheusRule;
import com.harmonycloud.zeus.integration.cluster.bean.prometheus.PrometheusRuleGroups;
import com.harmonycloud.zeus.integration.cluster.bean.prometheus.PrometheusRules;
import com.harmonycloud.zeus.service.k8s.PrometheusRuleService;
import com.harmonycloud.zeus.service.middleware.MiddlewareAlertsService;
import com.harmonycloud.zeus.service.middleware.MiddlewareService;
import com.harmonycloud.zeus.service.registry.HelmChartService;
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

    @Override
    public List<MiddlewareAlertsDTO> listUsedRules(String clusterId, String namespace, String middlewareName,
        String keyword) throws Exception {
        // 获取prometheus中的rules(用于获取status
        List<PrometheusGroups> prometheusGroupsList = this.getPrometheusRule(clusterId, namespace, middlewareName);
        // 获取configmap中的rules(用于判断是否删除
        PrometheusRule prometheusRule = prometheusRuleService.get(clusterId, namespace, middlewareName);
        List<PrometheusRules> rulesList = new ArrayList<>();
        prometheusRule.getSpec().getGroups()
            .forEach(prometheusRuleGroups -> rulesList.addAll(prometheusRuleGroups.getRules()));
        Map<String, PrometheusRules> prometheusRulesMap =
            rulesList.stream().collect(Collectors.toMap(PrometheusRules::getAlert, alert -> alert));
        // 组装数据
        List<MiddlewareAlertsDTO> middlewareAlertsDTOList = new ArrayList<>();
        prometheusGroupsList.forEach(prometheusGroups -> prometheusGroups.getRules().forEach(rule -> {
            if ("recording".equals(rule.getType()) || filterExpr(rule.getQuery())) {
                return;
            }
            MiddlewareAlertsDTO middlewareAlertsDTO = new MiddlewareAlertsDTO().setAlert(rule.getName());
            // 获取中文描述
            middlewareAlertsDTO.setDescription(rule.getAnnotations().containsKey("description")
                ? rule.getAnnotations().get("description") : rule.getName().split("-")[0]);
            if (!StringUtils.isEmpty(keyword)
                && !StringUtils.containsIgnoreCase(middlewareAlertsDTO.getDescription(), keyword)) {
                return;
            }
            // 获取状态
            middlewareAlertsDTO
                .setStatus(prometheusRulesMap.containsKey(rule.getName()) ? rule.getHealth() : "deleting");
            // 获取符号
            middlewareAlertsDTO.setSymbol(getSymbol(rule.getQuery()));
            // 获取阈值
            middlewareAlertsDTO.setThreshold(getThreshold(rule.getQuery()));
            //获取单位
            middlewareAlertsDTO.setUnit(rule.getAnnotations().getOrDefault("unit", ""));
            // 获取时间
            middlewareAlertsDTO.setTime(calculateTime(rule.getDuration()));
            // 获取创建时间
            if (rule.getAnnotations().containsKey("createTime")) {
                middlewareAlertsDTO.setCreateTime(
                    DateUtils.parseDate(rule.getAnnotations().get("createTime"), DateUtils.YYYY_MM_DD_T_HH_MM_SS_Z));
            }
            // 获取沉默时间
            middlewareAlertsDTO.setSilence(rule.getAnnotations().getOrDefault("silence", ""));
            prometheusRulesMap.remove(rule.getName());
            middlewareAlertsDTOList.add(middlewareAlertsDTO);
        }));
        // 获取创建中状态
        for (String key : prometheusRulesMap.keySet()) {
            PrometheusRules rule = prometheusRulesMap.get(key);
            if (StringUtils.isNotEmpty(rule.getRecord()) || filterExpr(rule.getExpr())) {
                continue;
            }
            MiddlewareAlertsDTO middlewareAlertsDTO = new MiddlewareAlertsDTO().setAlert(rule.getAlert());
            // 获取中文描述
            middlewareAlertsDTO.setDescription(rule.getAnnotations().containsKey("description")
                ? rule.getAnnotations().get("description") : rule.getAlert().split("-")[0]);
            if (!StringUtils.isEmpty(keyword)
                && !StringUtils.containsIgnoreCase(middlewareAlertsDTO.getDescription(), keyword)) {
                continue;
            }
            // 获取创建时间
            if (rule.getAnnotations().containsKey("createTime")) {
                middlewareAlertsDTO.setCreateTime(
                        DateUtils.parseDate(rule.getAnnotations().get("createTime"), DateUtils.YYYY_MM_DD_T_HH_MM_SS_Z));
            }
            // 设置状态
            middlewareAlertsDTO.setStatus("creating");
            middlewareAlertsDTOList.add(middlewareAlertsDTO);
        }
        middlewareAlertsDTOList.sort((o1, o2) -> {
            if (o1.getCreateTime() == null) {
                if (o2.getCreateTime() == null) {
                    return 0;
                }
                return 1;
            }
            if (o2.getCreateTime() == null) {
                return -1;
            }
            return o2.getCreateTime().compareTo(o1.getCreateTime());
        });
        return middlewareAlertsDTOList;
    }

    @Override
    public List<MiddlewareAlertsDTO> listRules(String clusterId, String namespace, String middlewareName, String type)
        throws Exception {
        String data;
        try {
            Middleware middleware = middlewareService.detail(clusterId, namespace, middlewareName, type);
            QueryWrapper<BeanAlertRule> wrapper = new QueryWrapper<BeanAlertRule>().eq("chart_name", type)
                .eq("chart_version", middleware.getChartVersion());
            data = beanAlertRuleMapper.selectOne(wrapper).getAlert();
        } catch (Exception e) {
            log.info("数据库中无此类中间件告警规则，开始数据同步");
            try {
                HelmChartFile helmChart =
                    helmChartService.getHelmChart(clusterId, namespace, middlewareName, type);
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
        List<MiddlewareAlertsDTO> middlewareAlertsDTOList) throws Exception {
        // 获取cr
        PrometheusRule prometheusRule = prometheusRuleService.get(clusterId, namespace, middlewareName);
        middlewareAlertsDTOList.forEach(middlewareAlertsDTO -> {
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
        });
        prometheusRuleService.update(clusterId, prometheusRule);
    }

    @Override
    public void deleteRules(String clusterId, String namespace, String middlewareName, String alert) {
        // 获取cr
        PrometheusRule prometheusRule = prometheusRuleService.get(clusterId, namespace, middlewareName);
        prometheusRule.getSpec().getGroups().forEach(prometheusRuleGroups -> {
            prometheusRuleGroups.getRules().removeIf(prometheusRules -> !StringUtils.isEmpty(prometheusRules.getAlert())
                && prometheusRules.getAlert().equals(alert));
        });
        prometheusRuleService.update(clusterId, prometheusRule);
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
                data = yaml.loadAs(helmChart.getYamlFileMap().get(key), JSONObject.class);
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
        PrometheusRules prometheusRules =
            new PrometheusRules().setAlert(middlewareAlertsDTO.getAlert() + "-" + UUIDUtils.get8UUID())
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
        String expr = middlewareAlertsDTO.getExpr().replace(
            "{{ include \"" + middlewareAlertsDTO.getType() + ".fullname\" . }}", middlewareAlertsDTO.getName());
        String symbol = getSymbol(expr);
        String threshold = getThreshold(expr);
        expr = expr.replace(symbol, middlewareAlertsDTO.getSymbol()).replace(threshold,
            middlewareAlertsDTO.getThreshold());
        prometheusRules.setExpr(expr);
        return prometheusRules;
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
