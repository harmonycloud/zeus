package com.middleware.zeus.service.middleware.impl;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.json.JSONUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.github.pagehelper.PageInfo;
import com.middleware.zeus.common.constants.NameConstant;
import com.middleware.zeus.common.enums.ErrorMessage;
import com.middleware.zeus.common.enums.middleware.MiddlewareTypeEnum;
import com.middleware.zeus.common.exception.CaasRuntimeException;
import com.middleware.zeus.common.model.*;
import com.middleware.zeus.common.model.middleware.Middleware;
import com.middleware.zeus.common.model.middleware.MiddlewareAlertsDTO;
import com.middleware.zeus.common.model.registry.HelmChartFile;
import com.middleware.zeus.common.model.user.UserDto;
import com.middleware.zeus.service.user.UserService;
import com.middleware.zeus.util.date.DateUtils;
import com.middleware.zeus.util.uuid.UUIDUtils;
import com.middleware.zeus.bean.AlertRuleId;
import com.middleware.zeus.bean.BeanAlertRule;
import com.middleware.zeus.bean.BeanAlertSetting;
import com.middleware.zeus.bean.BeanMailToUser;
import com.middleware.zeus.dao.AlertRuleIdMapper;
import com.middleware.zeus.dao.BeanAlertRuleMapper;
import com.middleware.zeus.dao.BeanAlertSettingMapper;
import com.middleware.zeus.dao.BeanMailToUserMapper;
import com.middleware.zeus.dao.user.BeanUserMapper;
import com.middleware.zeus.integration.cluster.PrometheusWrapper;
import com.middleware.zeus.integration.cluster.bean.MiddlewareCluster;
import com.middleware.zeus.integration.cluster.bean.prometheus.PrometheusRule;
import com.middleware.zeus.integration.cluster.bean.prometheus.PrometheusRuleGroups;
import com.middleware.zeus.integration.cluster.bean.prometheus.PrometheusRules;
import com.middleware.zeus.service.k8s.MiddlewareClusterService;
import com.middleware.zeus.service.k8s.PrometheusRuleService;
import com.middleware.zeus.service.middleware.MiddlewareAlertsService;
import com.middleware.zeus.service.middleware.MiddlewareService;
import com.middleware.zeus.service.registry.HelmChartService;
import com.middleware.zeus.service.system.AlertUserService;
import com.middleware.zeus.service.user.ProjectService;
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

import static com.middleware.zeus.common.constants.AlertConstant.*;
import static com.middleware.zeus.common.constants.CommonConstant.NUM_FOUR;
import static com.middleware.zeus.common.constants.user.UserConstant.ADMIN;

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
    private BeanAlertRuleMapper beanAlertRuleMapper;
    @Autowired
    private AlertRuleIdMapper alertRuleIdMapper;
    @Autowired
    private AlertUserService alertUserService;
    @Autowired
    private ProjectService projectService;
    @Autowired
    private UserService userService;

    private final String SYSTEM_ALERT = "system_alert";

    @Override
    public List<MiddlewareAlertsDTO> listUsedRules(String clusterId, String namespace, String middlewareName,
        String keyword) {
        // 查询prometheus文件
        PrometheusRule prometheusRule = prometheusRuleService.get(clusterId, namespace, middlewareName);
        // 封装告警规则文件
        List<MiddlewareAlertsDTO> middlewareAlertsDTOList = prometheusRuleService.convertPrometheusRule(prometheusRule);
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
            if (rule.getAlert().contains("-")){
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
        //告警规则入库
        middlewareAlertsListDto.getMiddlewareAlertsDTOList().forEach(middlewareAlertsDTO -> {
            middlewareAlertsDTO.setAlert(middlewareAlertsDTO.getAlert() + "-" + UUIDUtils.get8UUID());
            updateServiceAlerts2Prometheus(clusterId, namespace, middlewareName, middlewareName, middlewareAlertsDTO);
            //addAlerts2Sql(clusterId, namespace, middlewareName, middlewareAlertsDTO);
        });
    }

    @Override
    public void deleteRules(String clusterId, String namespace, String middlewareName, String alert, String alertRuleId) {
        // 获取cr
        PrometheusRule prometheusRule = prometheusRuleService.get(clusterId, namespace, middlewareName);
        prometheusRule.getSpec().getGroups().forEach(prometheusRuleGroups -> {
            prometheusRuleGroups.getRules().removeIf(prometheusRules -> !StringUtils.isEmpty(prometheusRules.getAlert())
                    && prometheusRules.getAlert().equals(alert));
        });
        prometheusRuleService.update(clusterId, prometheusRule);
        /*QueryWrapper<AlertRuleId> deleteWrapper = new QueryWrapper<>();
        deleteWrapper.eq("alert",alert);
        alertRuleIdMapper.delete(deleteWrapper);*/
    }

    @Override
    public void updateRules(String clusterId, String namespace, String middlewareName, String ding, String alertRuleId,
        MiddlewareAlertsDTO middlewareAlertsDTO) {
        // 更新至prometheus
        updateServiceAlerts2Prometheus(clusterId, namespace, middlewareName, middlewareName, middlewareAlertsDTO);
        // updateAlerts2Mysql(clusterId, namespace, middlewareName, middlewareAlertsDTO);
    }

    /**
     * 从prometheusRule删除规则
     * @param clusterId
     * @param alert
     */
    public void deletePrometheusRules(String clusterId, List<String> alert) {
        // 获取cr
        PrometheusRule prometheusRule = prometheusRuleService.get(clusterId, NameConstant.MONITORING, NameConstant.PROMETHEUS_K8S_RULES);
        boolean status = false;
        for (PrometheusRuleGroups prometheusRuleGroups : prometheusRule.getSpec().getGroups()) {
            prometheusRuleGroups.getRules().removeIf(prometheusRules -> !StringUtils.isEmpty(prometheusRules.getAlert())
                    && alert.stream().anyMatch(al -> al.equals(prometheusRules.getAlert())));
            if (SYSTEM_ALERT.equals(prometheusRuleGroups.getName()) && prometheusRuleGroups.getRules().size() == 0) {
                status = true;
            }
        }
        if (status) {
            prometheusRule.getSpec().getGroups().removeIf(prometheusRuleGroups ->
                    SYSTEM_ALERT.equals(prometheusRuleGroups.getName())
            );
        }
        prometheusRuleService.update(clusterId, prometheusRule);
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
        assemblePrometheusRule(clusterId, middlewareName, middlewareAlertsDTO, prometheusRule);
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
    public void editBackupAlert(String clusterId, String namespace, String middlewareName, Boolean enable) {
        alertUserService.delete(null, clusterId, namespace, middlewareName, BACKUP);
        if (enable){
            AlertUserDo alertUserDo = new AlertUserDo();
            alertUserDo.setClusterId(clusterId);
            alertUserDo.setNamespace(namespace);
            alertUserDo.setName(middlewareName);
            alertUserDo.setAlertType(BACKUP);
            alertUserService.add(alertUserDo);
        }
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
        prometheusRules.getAnnotations().put("silence", middlewareAlertsDTO.getSilence());
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
        expr = expr.replace(symbol, middlewareAlertsDTO.getSymbol()).replace(threshold,
                middlewareAlertsDTO.getThreshold());
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

        String group = middlewareAlertsDTO.getAnnotations().get("group");
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
        if (StringUtils.isNotEmpty(str)) {
            return str.replace("{{`", "").replace("`}}", "");
        }
        return str;
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

    public List<AlertUserDto> listAlertUser(String clusterId, String namespace, String middlewareName) {
        // 获取告警用户列表
        List<AlertUserDo> alertUserDoList = alertUserService.list(clusterId, namespace, middlewareName, SERVICE);
        // 返回封装数据
        return alertUserDoList.stream().map(alertUserDo -> {
            AlertUserDto alertUserDto = new AlertUserDto();
            alertUserDto.setUsername(alertUserDo.getUsername());
            alertUserDto.setMailAlert(alertUserDo.getMailAlert());
            alertUserDto.setMessageAlert(alertUserDo.getMessageAlert());
            return alertUserDto;
        }).collect(Collectors.toList());
    }

    public List<AlertUserDto> listAllocatableAlertUser(String clusterId, String namespace, String middlewareName,
                                                       String organId, String projectId) {
        // 获取告警用户列表
        List<AlertUserDo> alertUserDoList = alertUserService.list(clusterId, namespace, middlewareName, SERVICE);
        // 获取用户集，并过滤掉已分配的用户和普通用户
        List<UserDto> userDtoList = projectService.getUser(organId, projectId, false).stream()
                .filter(userDto -> alertUserDoList.stream()
                        .noneMatch(alertUserDo -> alertUserDo.getUsername().equals(userDto.getUserName())))
                .filter(userDto -> !userDto.getRoleId().equals(NUM_FOUR))
                .collect(Collectors.toList());
        // 获取超级管理员用户
        userDtoList.addAll(userService.list(null).stream()
            .filter(
                userDto -> userDto.getIsAdmin() != null && userDto.getIsAdmin() && !userDto.getUserName().equals(ADMIN))
            .collect(Collectors.toList()));
        // 返回封装数据
        return userDtoList.stream().map(userDto -> {
            AlertUserDto alertUserDto = new AlertUserDto();
            BeanUtils.copyProperties(userDto, alertUserDto);
            alertUserDto.setMail(userDto.getEmail());
            alertUserDto.setUsername(userDto.getUserName());
            return alertUserDto;
        }).collect(Collectors.toList());
    }
}
