package com.middleware.zeus.service.middleware.impl;

import static com.middleware.zeus.common.constants.AlertConstant.BACKUP;
import static com.middleware.zeus.common.constants.AlertConstant.SERVICE;
import static com.middleware.zeus.common.constants.user.UserConstant.ADMIN;

import java.math.BigDecimal;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.alibaba.fastjson.JSONObject;
import com.middleware.zeus.common.enums.middleware.MiddlewareTypeEnum;
import com.middleware.zeus.common.model.AlertUserDo;
import com.middleware.zeus.common.model.AlertUserDto;
import com.middleware.zeus.common.model.AlertUserListDto;
import com.middleware.zeus.common.model.MiddlewareAlertsListDto;
import com.middleware.zeus.common.model.middleware.Middleware;
import com.middleware.zeus.common.model.middleware.MiddlewareAlertsDTO;
import com.middleware.zeus.common.model.middleware.MiddlewareClusterDTO;
import com.middleware.zeus.common.model.user.UserDto;
import com.middleware.zeus.integration.cluster.bean.prometheus.PrometheusRule;
import com.middleware.zeus.integration.cluster.bean.prometheus.PrometheusRuleGroups;
import com.middleware.zeus.integration.cluster.bean.prometheus.PrometheusRules;
import com.middleware.zeus.service.k8s.ClusterService;
import com.middleware.zeus.service.k8s.PrometheusRuleService;
import com.middleware.zeus.service.middleware.MiddlewareAlertsService;
import com.middleware.zeus.service.registry.HelmChartService;
import com.middleware.zeus.service.system.AlertUserService;
import com.middleware.zeus.service.user.ProjectService;
import com.middleware.zeus.service.user.UserService;
import com.middleware.zeus.util.date.DateUtils;
import com.middleware.zeus.util.uuid.UUIDUtils;

import lombok.extern.slf4j.Slf4j;

/**
 * @author xutianhong
 * @Date 2021/4/26 10:23 上午
 */
@Service
@Slf4j
public class MiddlewareAlertsServiceImpl implements MiddlewareAlertsService {

    @Autowired
    private PrometheusRuleService prometheusRuleService;
    @Autowired
    private AlertUserService alertUserService;
    @Autowired
    private ProjectService projectService;
    @Autowired
    private UserService userService;

    private final String SYSTEM_ALERT = "system_alert";
    private final String MIDDLEWARE_BACKUP_FAILED = "middlewareBackupFailed";
    private final String CUSTOM_ALERT_RULES = "customAlertRules";

    @Autowired
    private HelmChartService helmChartService;
    @Autowired
    private ClusterService clusterService;

    @Override
    public List<MiddlewareAlertsDTO> listUsedRules(String clusterId, String namespace, String middlewareName,
        String type, String keyword) {
        // 获取所有告警规则
        List<MiddlewareAlertsDTO> middlewareAlertsDTOList = getAllRules(clusterId, namespace, middlewareName);
        // 过滤备份告警规则
        middlewareAlertsDTOList = middlewareAlertsDTOList.stream()
            .filter(middlewareAlertsDTO -> !MIDDLEWARE_BACKUP_FAILED.equals(middlewareAlertsDTO.getName()))
            .collect(Collectors.toList());
        // 添加符号和阈值信息
        for (MiddlewareAlertsDTO middlewareAlertsDTO : middlewareAlertsDTOList) {
            middlewareAlertsDTO.setSymbol(getSymbol(middlewareAlertsDTO.getExpr()));
            middlewareAlertsDTO.setThreshold(getThreshold(middlewareAlertsDTO.getExpr()));
        }
        //校验备份告警规则是否存在
        checkBackupAlert(clusterId, namespace, middlewareName, type);
        // 根据创建时间排序
        middlewareAlertsDTOList.sort((o1, o2) -> o1.getCreateTime() == null ? -1
            : o2.getCreateTime() == null ? -1 : o2.getCreateTime().compareTo(o1.getCreateTime()));
        return middlewareAlertsDTOList;
    }

    @Override
    public List<MiddlewareAlertsDTO> listRules(String clusterId, String namespace, String middlewareName, String type) {
        // 查询prometheus文件
        PrometheusRule prometheusRule = prometheusRuleService.get(clusterId, namespace, middlewareName);
        List<PrometheusRules> rules = new ArrayList<>();
        // 获取所有规则
        for (PrometheusRuleGroups prometheusRuleGroups : prometheusRule.getSpec().getGroups()) {
            // 过滤自定义告警规则
            if ("custom-alert-rules".equals(prometheusRuleGroups.getName())) {
                continue;
            }
            // 记录group组
            prometheusRuleGroups.getRules().forEach(rule -> {
                if (!StringUtils.isEmpty(rule.getAlert())) {
                    rule.getAnnotations().put("group", prometheusRuleGroups.getName());
                }
            });
            rules.addAll(prometheusRuleGroups.getRules());
        }
        rules
            .removeIf(rule -> StringUtils.isEmpty(rule.getAlert()) || MIDDLEWARE_BACKUP_FAILED.equals(rule.getAlert()));
        // 封装数据
        List<MiddlewareAlertsDTO> middlewareAlertsDTOList = new ArrayList<>();
        rules.forEach(rule -> {
            if (filterExpr(rule.getExpr())) {
                return;
            }
            if (rule.getAlert().contains("-")) {
                return;
            }
            MiddlewareAlertsDTO middlewareAlertsDTO = new MiddlewareAlertsDTO();
            BeanUtils.copyProperties(rule, middlewareAlertsDTO);
            middlewareAlertsDTO.setDescription(rule.getAlert());
            middlewareAlertsDTO.setUnit(rule.getAnnotations().getOrDefault("unit", ""));
            middlewareAlertsDTO.setType(type);
            middlewareAlertsDTOList.add(middlewareAlertsDTO);
        });
        return middlewareAlertsDTOList;
    }

    @Override
    public void createRules(String clusterId, String namespace, String middlewareName,
        MiddlewareAlertsListDto middlewareAlertsListDto) {
        // 获取集群对象
        MiddlewareClusterDTO cluster = clusterService.findById(clusterId);
        // 获取中间件部署配置
        JSONObject values = helmChartService.getInstalledValues(middlewareName, namespace, cluster);

        if (values == null) {
            return;
        }
        if (values.getJSONObject(CUSTOM_ALERT_RULES) == null) {
            values.put(CUSTOM_ALERT_RULES, new JSONObject());
        }

        middlewareAlertsListDto.getMiddlewareAlertsDTOList().forEach(middlewareAlertsDTO -> {
            // 获取自定义告警规则对象
            JSONObject customAlertRules = values.getJSONObject(CUSTOM_ALERT_RULES);
            // 获取告警规则annotations
            Map<String, String> annotations = middlewareAlertsDTO.getAnnotations();
            // 生成告警规则名称，避免重名
            String alertName = middlewareAlertsDTO.getAlert() + "_" + UUIDUtils.get8UUID();
            JSONObject alert = new JSONObject();
            alert.put("alertLevel", annotations.getOrDefault("alertLevel", "warning"));
            alert.put("threshold", Integer.valueOf(middlewareAlertsDTO.getThreshold()));
            alert.put("silence", middlewareAlertsDTO.getSilence());

            String time = "";
            if (middlewareAlertsDTO.getAlertTime() != null && middlewareAlertsDTO.getAlertTimes() != null) {
                time = String.valueOf(middlewareAlertsDTO.getAlertTime().divide(middlewareAlertsDTO.getAlertTimes(),0, BigDecimal.ROUND_UP));
                time.replace("\"","");
            } else if (middlewareAlertsDTO.getTime() != null) {
                time = middlewareAlertsDTO.getTime();
            }
            alert.put("interval", Integer.valueOf(time));
            alert.put("alertText", annotations.getOrDefault("message", ""));
            alert.put("alertExpr", middlewareAlertsDTO.getExpr());

            customAlertRules.put(alertName, alert);
        });
        // 更新helm values
        String type = middlewareAlertsListDto.getMiddlewareAlertsDTOList().get(0).getType();
        Middleware middleware = new Middleware(clusterId, namespace, middlewareName, type);
        middleware.setChartName(type);
        middleware.setChartVersion(helmChartService.getChartVersion(values, type));
        helmChartService.upgrade(middleware, values, values, cluster);
    }

    @Override
    public void deleteRules(String clusterId, String namespace, String middlewareName, String type, String alert) {
        // 获取集群对象
        MiddlewareClusterDTO cluster = clusterService.findById(clusterId);
        // 获取中间件部署配置
        JSONObject values = helmChartService.getInstalledValues(middlewareName, namespace, cluster);

        if (values == null || !values.containsKey(CUSTOM_ALERT_RULES)){
            return;
        }
        JSONObject customAlertRules = values.getJSONObject(CUSTOM_ALERT_RULES);
        customAlertRules.remove(alert);
        // 更新helm values
        Middleware middleware = new Middleware(clusterId, namespace, middlewareName, type);
        middleware.setChartName(type);
        middleware.setChartVersion(helmChartService.getChartVersion(values, type));
        helmChartService.upgrade(middleware, values, values, cluster);
    }

    @Override
    public MiddlewareAlertsDTO detail(String clusterId, String namespace, String middlewareName, String alert) {
        // 获取所有告警规则
        List<MiddlewareAlertsDTO> middlewareAlertsDTOList = getAllRules(clusterId, namespace, middlewareName);

        // 根据告警名称进行过滤
        middlewareAlertsDTOList = middlewareAlertsDTOList.stream()
            .filter(middlewareAlertsDTO -> middlewareAlertsDTO.getAlert().equals(alert)).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(middlewareAlertsDTOList)) {
            return null;
        }
        // 添加符号和阈值信息
        for (MiddlewareAlertsDTO middlewareAlertsDTO : middlewareAlertsDTOList){
            middlewareAlertsDTO.setSymbol(getSymbol(middlewareAlertsDTO.getExpr()));
            middlewareAlertsDTO.setThreshold(getThreshold(middlewareAlertsDTO.getExpr()));
            if (StringUtils.isNotEmpty(middlewareAlertsDTO.getTime())){
                String time = middlewareAlertsDTO.getTime();
                if (time.contains("m")){
                    middlewareAlertsDTO.setAlertTime(new BigDecimal(time.replace("m", "")));
                    middlewareAlertsDTO.setAlertTimes(new BigDecimal(1));
                } else if (time.contains("s")){
                    String alertTime = time.replace("s", "");
                    middlewareAlertsDTO.setAlertTime(new BigDecimal(1));
                    middlewareAlertsDTO.setAlertTimes(new BigDecimal(60 / Integer.parseInt(alertTime)));
                } else if (time.contains("h")){
                    String alertTime = time.replace("h", "");
                    middlewareAlertsDTO.setAlertTime(new BigDecimal(Integer.parseInt(alertTime) * 60));
                    middlewareAlertsDTO.setAlertTimes(new BigDecimal(1));
                }
            }
        }
        return middlewareAlertsDTOList.get(0);
    }

    @Override
    public void updateRules(String clusterId, String namespace, String middlewareName,
        MiddlewareAlertsDTO middlewareAlertsDTO) {
        // 获取集群对象
        MiddlewareClusterDTO cluster = clusterService.findById(clusterId);
        // 获取中间件部署配置
        JSONObject values = helmChartService.getInstalledValues(middlewareName, namespace, cluster);

        if (values == null) {
            return;
        }
        // 计算告警频率
        String time = "";
        if (middlewareAlertsDTO.getAlertTime() != null && middlewareAlertsDTO.getAlertTimes() != null) {
            time = String.valueOf(
                middlewareAlertsDTO.getAlertTime().divide(middlewareAlertsDTO.getAlertTimes(), 0, BigDecimal.ROUND_UP));
            time.replace("\"", "");
        } else if (middlewareAlertsDTO.getTime() != null) {
            time = middlewareAlertsDTO.getTime();
        }

        // 修改非自定义告警规则对象
        if (middlewareAlertsDTO.getCustom() == null && !middlewareAlertsDTO.getCustom()) {
            JSONObject alertRules = values.getJSONObject("alertRules");
            if (alertRules == null) {
                alertRules = new JSONObject();
            }
            JSONObject alert = new JSONObject();
            alert.put("alertLevel", middlewareAlertsDTO.getLevel());
            alert.put("threshold", middlewareAlertsDTO.getThreshold());
            alert.put("silence", middlewareAlertsDTO.getSilence());
            alert.put("interval", Integer.valueOf(time));

            alertRules.put(middlewareAlertsDTO.getAlert(), alert);
            values.put("alertRules", alertRules);
        } else {
            // 获取自定义告警规则对象
            JSONObject customAlertRules = values.getJSONObject(CUSTOM_ALERT_RULES);
            if (customAlertRules == null) {
                customAlertRules = new JSONObject();
            }
            // 获取告警规则annotations
            Map<String, String> annotations = middlewareAlertsDTO.getAnnotations();
            JSONObject alert = customAlertRules.getJSONObject(middlewareAlertsDTO.getAlert());
            if (alert == null) {
                alert = new JSONObject();
            }
            alert.put("alertLevel", middlewareAlertsDTO.getLevel());
            alert.put("threshold", middlewareAlertsDTO.getThreshold());
            alert.put("silence", middlewareAlertsDTO.getSilence());

            alert.put("interval", Integer.valueOf(time));
            alert.put("alertText", annotations.getOrDefault("message", ""));
            alert.put("alertExpr", middlewareAlertsDTO.getExpr());

            customAlertRules.put(middlewareAlertsDTO.getAlert(), alert);
            values.put(CUSTOM_ALERT_RULES, cluster);
        }
        // 更新helm values
        Middleware middleware = new Middleware(clusterId, namespace, middlewareName, middlewareAlertsDTO.getType());
        middleware.setChartName(middlewareAlertsDTO.getType());
        middleware.setChartVersion(helmChartService.getChartVersion(values, middlewareAlertsDTO.getType()));
        helmChartService.upgrade(middleware, values, values, cluster);
    }

    @Override
    public List<AlertUserDto> alertUser(String clusterId, String namespace, String middlewareName, Boolean allocatable,
                                        String organId, String projectId) {
        if (allocatable) {
            return listAllocatableAlertUser(clusterId, namespace, middlewareName, organId, projectId);
        } else {
            return listAlertUser(clusterId, namespace, middlewareName);
        }
    }

    @Override
    public void addAlertUser(String clusterId, String namespace, String middlewareName, AlertUserListDto alertUserListDto) {
        for (AlertUserDto alertUserDto : alertUserListDto.getAlertUserDtoList()) {
            AlertUserDo alertUserDo = new AlertUserDo();
            alertUserDo.setUsername(alertUserDto.getUsername());
            alertUserDo.setClusterId(clusterId);
            alertUserDo.setNamespace(namespace);
            alertUserDo.setName(middlewareName);
            alertUserDo.setMailAlert(alertUserDto.getMailAlert());
            alertUserDo.setMessageAlert(alertUserDto.getMessageAlert());
            alertUserDo.setAlertType(SERVICE);
            // 添加平台告警用户
            alertUserService.add(alertUserDo);
        }
    }

    @Override
    public void removeAlertUser(String clusterId, String namespace, String middlewareName, String username) {
        alertUserService.delete(username, clusterId, namespace, middlewareName, SERVICE);
    }

    @Override
    public Boolean getBackupAlert(String clusterId, String namespace, String middlewareName) {
        List<AlertUserDo> alertUserDoList = alertUserService.list(clusterId, namespace, middlewareName, BACKUP);
        return !CollectionUtils.isEmpty(alertUserDoList);
    }

    @Override
    public void editBackupAlert(String clusterId, String namespace, String middlewareName, String type, Boolean enable) {
        // 备份告警通知开关更新
        alertUserService.delete(null, clusterId, namespace, middlewareName, BACKUP);
        if (enable){
            AlertUserDo alertUserDo = new AlertUserDo();
            alertUserDo.setClusterId(clusterId);
            alertUserDo.setNamespace(namespace);
            alertUserDo.setName(middlewareName);
            alertUserDo.setAlertType(BACKUP);
            alertUserService.add(alertUserDo);
        }

        //校验备份告警规则是否存在
        checkBackupAlert(clusterId, namespace, middlewareName, type);
    }

    /**
     * 组装PrometheusRules
     */
    public PrometheusRules convertMiddlewareAlerts(MiddlewareAlertsDTO middlewareAlertsDTO, String clusterId) {
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
        if (StringUtils.isNotEmpty(middlewareAlertsDTO.getSilence())){
            prometheusRules.getAnnotations().put("silence", middlewareAlertsDTO.getSilence());
        }
        // 写入创建时间
        prometheusRules.getAnnotations().put("createTime",
                DateUtils.dateToString(new Date(), DateUtils.YYYY_MM_DD_T_HH_MM_SS_Z));
        // 写入集群
        prometheusRules.getLabels().put("clusterId", clusterId);
        prometheusRules.getLabels().put("alertname", middlewareAlertsDTO.getAlert());
        // 构造expr
        String expr = "";
        if (MiddlewareTypeEnum.KAFKA.getType().equals(middlewareAlertsDTO.getType())) {
            expr = middlewareAlertsDTO.getExpr().replace(
                    "{{ include \"" + middlewareAlertsDTO.getType() + "-hc" + ".fullname\" . }}",
                    middlewareAlertsDTO.getName());
        } else if (MiddlewareTypeEnum.POSTGRESQL.getType().equals(middlewareAlertsDTO.getType())) {
            expr = middlewareAlertsDTO.getExpr().replace("{{ include \"pgsql.fullname\" . }}",
                    middlewareAlertsDTO.getName());
        } else {
            expr = middlewareAlertsDTO.getExpr().replace(
                    "{{ include \"" + middlewareAlertsDTO.getType() + ".fullname\" . }}", middlewareAlertsDTO.getName());
        }
        String symbol = getSymbol(expr);
        String threshold = getThreshold(expr);
        if (StringUtils.isNotEmpty(middlewareAlertsDTO.getSymbol())) {
            expr = expr.replace(symbol, middlewareAlertsDTO.getSymbol());
        }
        if (StringUtils.isNotEmpty(middlewareAlertsDTO.getThreshold())) {
            expr = expr.replace(threshold, middlewareAlertsDTO.getThreshold());
        }
        prometheusRules.setExpr(expr);
        return prometheusRules;
    }

    /**
     * 替换prometheusRule内容
     */
    public void assemblePrometheusRule(String clusterId, String middlewareName, MiddlewareAlertsDTO middlewareAlertsDTO,
        PrometheusRule prometheusRule) {
        prometheusRule.getSpec().getGroups().forEach(prometheusRuleGroups -> {
            prometheusRuleGroups.getRules().removeIf(prometheusRules -> !StringUtils.isEmpty(prometheusRules.getAlert())
                && prometheusRules.getAlert().equals(middlewareAlertsDTO.getAlert()));
        });

        // 创建prometheusRules
        middlewareAlertsDTO.setName(middlewareName);
        PrometheusRules prometheusRules = convertMiddlewareAlerts(middlewareAlertsDTO, clusterId);

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
                    new PrometheusRuleGroups().setName(prometheusRules.getAnnotations().get("group"));
            List<PrometheusRules> prometheusRulesList = new ArrayList<>();
            prometheusRulesList.add(prometheusRules);
            prometheusRuleGroups.setRules(prometheusRulesList);
            prometheusRule.getSpec().getGroups().add(prometheusRuleGroups);
        }
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
        String[] thresholds = expr.split(symbol);
        return thresholds[thresholds.length - 1].replaceAll("[^0-9]", "");
    }

    public String replaceValue(String str) {
        if (StringUtils.isNotEmpty(str)) {
            return str.replace("{{`", "").replace("`}}", "");
        }
        return str;
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
    public List<AlertUserDto> listAlertUser(String clusterId, String namespace, String middlewareName) {
        // 获取告警用户列表
        List<AlertUserDo> alertUserDoList = alertUserService.list(clusterId, namespace, middlewareName, SERVICE);
        // 获取用户集
        List<UserDto> userDtoList = userService.list(null);
        // 返回封装数据并过滤未分配的告警用户
        return userDtoList.stream().filter(userDto -> alertUserDoList.stream()
                .anyMatch(alertUserDo -> alertUserDo.getUsername().equals(userDto.getUserName()))).map(userDto -> {
            AlertUserDto alertUserDto = new AlertUserDto();
            BeanUtils.copyProperties(userDto, alertUserDto);
            alertUserDto.setMail(userDto.getEmail());
            alertUserDto.setUsername(userDto.getUserName());
            return alertUserDto;
        }).collect(Collectors.toList());
    }

    public List<AlertUserDto> listAllocatableAlertUser(String clusterId, String namespace, String middlewareName,
        String organId, String projectId) {
        // 获取告警用户列表
        List<AlertUserDo> alertUserDoList = alertUserService.list(clusterId, namespace, middlewareName, SERVICE);
        // 获取用户集
        List<UserDto> userDtoList = projectService.getUser(organId, projectId, false);
        // 获取超级管理员用户,并过滤admin用户和同时存在于项目中的超级管理员角色用户
        List<UserDto> adminUserList = userService.list(null).stream()
            .filter(
                userDto -> userDto.getIsAdmin() != null && userDto.getIsAdmin() && !userDto.getUserName().equals(ADMIN))
            .filter(userDto -> userDtoList.stream()
                .noneMatch(existUser -> existUser.getUserName().equals(userDto.getUserName())))
            .collect(Collectors.toList());
        // 合并可添加用户
        userDtoList.addAll(adminUserList);
        // 返回封装数据并过滤已分配的告警用户
        return userDtoList.stream().filter(userDto -> alertUserDoList.stream()
            .noneMatch(alertUserDo -> alertUserDo.getUsername().equals(userDto.getUserName()))).map(userDto -> {
                AlertUserDto alertUserDto = new AlertUserDto();
                BeanUtils.copyProperties(userDto, alertUserDto);
                alertUserDto.setMail(userDto.getEmail());
                alertUserDto.setUsername(userDto.getUserName());
                return alertUserDto;
            }).collect(Collectors.toList());
    }

    public void checkBackupAlert(String clusterId, String namespace, String middlewareName, String type){
        String alertName = "middlewareBackupFailed";
        if (detail(clusterId, namespace, middlewareName, alertName) == null){
            // 添加告警规则
            MiddlewareAlertsDTO middlewareAlertsDTO = new MiddlewareAlertsDTO();
            middlewareAlertsDTO.setAlert(alertName);
            middlewareAlertsDTO.setAlertTime(new BigDecimal(1));
            middlewareAlertsDTO.setAlertTimes(new BigDecimal(1));
            middlewareAlertsDTO.setExpr("rate(backup_failed_total{middleware_name=\"" + middlewareName + "\"}[3m]) > 0");
            middlewareAlertsDTO.setLay(SERVICE);
            middlewareAlertsDTO.setName(alertName);
            middlewareAlertsDTO.setLevel("warning");

            Map<String, String> labels = new HashMap<>();
            labels.put("severity", "warning");
            labels.put("clusterId", clusterId);
            labels.put("namespace", namespace);
            labels.put("middleware", type);
            labels.put("service", middlewareName);
            middlewareAlertsDTO.setLabels(labels);

            Map<String, String> annotations = new HashMap<>();
            annotations.put("alertLevel", "warning");
            annotations.put("message", "{{ $labels.middleware }} database backup task failed. (Namespace: {{ $labels.namespace }}, ServiceName: {{ $labels.service }}, BackupName: {{ $labels.name }})");
            annotations.put("summary", "{{ $labels.middleware }} database backup task failed. (Namespace: {{ $labels.namespace }}, ServiceName: {{ $labels.service }}, BackupName: {{ $labels.name }})");
            annotations.put("group", "backup");
            annotations.put("target_type", "backup");
            middlewareAlertsDTO.setAnnotations(annotations);

            // todo
            //updateServiceAlerts2Prometheus(clusterId, namespace, middlewareName, middlewareAlertsDTO);
        }
    }

    /**
     * 查询指定中间件的所有告警规则
     * @param clusterId 集群id
     * @param namespace 命名空间
     * @param middlewareName 中间件名称
     * @return List<MiddlewareAlertsDTO>
     */
    public List<MiddlewareAlertsDTO> getAllRules(String clusterId, String namespace, String middlewareName){
        // 查询原生prometheus文件
        PrometheusRule prometheusRule = prometheusRuleService.get(clusterId, namespace, middlewareName);
        // 查询新增告警规则文件
        //PrometheusRule zeusPrometheusRule = prometheusRuleService.get(clusterId, namespace, ZEUS + LINE + middlewareName);
        // 封装告警规则文件
        List<MiddlewareAlertsDTO> middlewareAlertsDTOList = prometheusRuleService.convertPrometheusRule(prometheusRule);
        // 封装额外告警规则文件
//        if (zeusPrometheusRule != null){
//            middlewareAlertsDTOList.addAll(prometheusRuleService.convertPrometheusRule(zeusPrometheusRule));
//        }
        return middlewareAlertsDTOList;
    }
}
