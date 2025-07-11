package com.middleware.zeus.service.system.impl;

import static com.middleware.zeus.common.constants.AlertConstant.*;
import static com.middleware.zeus.common.constants.CommonConstant.ASC;
import static com.middleware.zeus.common.constants.CommonConstant.DESC;
import static com.middleware.zeus.common.constants.NameConstant.ZEUS;
import static com.middleware.zeus.common.constants.user.UserConstant.ADMIN;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.middleware.zeus.common.model.middleware.MiddlewareClusterDTO;
import com.middleware.zeus.util.date.DateUtils;
import com.middleware.zeus.integration.cluster.bean.prometheus.PrometheusRuleGroups;
import com.middleware.zeus.integration.cluster.bean.prometheus.PrometheusRules;
import com.skyview.language.annotations.TranslateAfterResult;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.middleware.zeus.common.enums.AlertTargetEnum;
import com.middleware.zeus.common.enums.ErrorMessage;
import com.middleware.zeus.common.enums.middleware.MiddlewareOfficialNameEnum;
import com.middleware.zeus.common.exception.BusinessException;
import com.middleware.zeus.common.model.*;
import com.middleware.zeus.common.model.middleware.MiddlewareAlertsDTO;
import com.middleware.zeus.common.model.user.UserDto;
import com.middleware.zeus.util.uuid.UUIDUtils;
import com.middleware.zeus.bean.BeanAlertRecord;
import com.middleware.zeus.dao.BeanAlertRecordMapper;
import com.middleware.zeus.integration.cluster.bean.prometheus.PrometheusRule;
import com.middleware.zeus.service.k8s.ClusterService;
import com.middleware.zeus.service.k8s.PrometheusRuleService;
import com.middleware.zeus.service.system.AlertService;
import com.middleware.zeus.service.system.AlertUserService;
import com.middleware.zeus.service.user.UserService;

import lombok.extern.slf4j.Slf4j;

/**
 * @author xutianhong
 * @Date 2023/5/6 11:03 上午
 */
@Service
@Slf4j
public class AlertServiceImpl implements AlertService {

    @Value("${system.alertRecordLimit:1000}")
    private String alertRecordLimit;

    @Autowired
    private BeanAlertRecordMapper beanAlertRecordMapper;
    @Autowired
    private ClusterService clusterService;
    @Autowired
    private PrometheusRuleService prometheusRuleService;
    @Autowired
    private UserService userService;
    @Autowired
    private AlertUserService alertUserService;


    @Override
    public List<AlertRecordIndex> alertRecordIndex(String alertType) {
        // 封装数据库查询逻辑
        QueryWrapper<BeanAlertRecord> wrapper =
            new QueryWrapper<BeanAlertRecord>().ne("cluster_id", "").ne("name", "").eq("lay", alertType);
        if (alertType.equals(SERVICE)) {
            wrapper.isNotNull("type");
        }
        //wrapper.last("limit " + alertRecordLimit);

        // 查询告警记录
        List<BeanAlertRecord> beanAlertRecordList = beanAlertRecordMapper.selectList(wrapper);

        List<AlertRecordIndex> alertRecordIndexList = new ArrayList<>();
        if (alertType.equals(CLUSTER)) {
            // 获取集群别名map
            Map<String, String> clusterAlisaNameMap = clusterService.getClusterAliasName();
            // 根据集群id group 告警记录
            Map<String, List<BeanAlertRecord>> beanAlertRecordMap =
                beanAlertRecordList.stream().collect(Collectors.groupingBy(BeanAlertRecord::getClusterId));
            // 封装数据
            for (String key : beanAlertRecordMap.keySet()) {
                AlertRecordIndex alertRecordIndex = new AlertRecordIndex();
                alertRecordIndex.setClusterId(key);
                alertRecordIndex.setClusterAliasName(clusterAlisaNameMap.get(key));
                alertRecordIndex.setCount(beanAlertRecordMap.get(key).size());
                alertRecordIndexList.add(alertRecordIndex);
            }
        } else if (alertType.equals(SERVICE)) {
            // 根据type group 告警记录
            Map<String, List<BeanAlertRecord>> beanAlertRecordMap =
                beanAlertRecordList.stream().collect(Collectors.groupingBy(BeanAlertRecord::getType));
            // 封装数据
            for (String key : beanAlertRecordMap.keySet()) {
                AlertRecordIndex alertRecordIndex = new AlertRecordIndex();
                alertRecordIndex.setMiddlewareName(MiddlewareOfficialNameEnum.findByChartName(key));
                alertRecordIndex.setMiddlewareType(key);
                alertRecordIndex.setCount(beanAlertRecordMap.get(key).size());
                alertRecordIndexList.add(alertRecordIndex);
            }
        }
        return alertRecordIndexList;
    }

    @Override
    @TranslateAfterResult
    public List<AlertRecordIndex> alertRecordFilter(String alertType, String clusterId) {
        // 封装数据库查询逻辑
        QueryWrapper<BeanAlertRecord> wrapper =
                new QueryWrapper<BeanAlertRecord>().eq("lay", alertType);
        if (StringUtils.isNotEmpty(clusterId)){
            wrapper.eq("cluster_id", clusterId);
        } else {
            wrapper.ne("cluster_id", "");
        }
        if (alertType.equals(SERVICE)) {
            wrapper.isNotNull("type");
        }
        wrapper.groupBy("alias_name");

        // 查询告警记录
        List<BeanAlertRecord> beanAlertRecordList = beanAlertRecordMapper.selectList(wrapper);
        return beanAlertRecordList.stream().map(beanAlertRecord -> {
            AlertRecordIndex alertRecordIndex = new AlertRecordIndex();
            alertRecordIndex.setTargetName(beanAlertRecord.getName());
            alertRecordIndex.setTargetAliasName(beanAlertRecord.getAliasName());
            return alertRecordIndex;
        }).collect(Collectors.toList());
    }

    @Override
    @TranslateAfterResult
    public List<BeanAlertRecord> searchAlertRecord(AlertRecordQueryDto query) {
        // 封装数据库查询逻辑
        QueryWrapper<BeanAlertRecord> wrapper = new QueryWrapper<>();
        // 根据告警记录对象查询
        wrapper.eq("lay", query.getAlertType());
        // 根据集群id查询
        if (StringUtils.isNotEmpty(query.getClusterId())) {
            wrapper.eq("cluster_id", query.getClusterId());
        } else {
            wrapper.ne("cluster_id", "");
        }
        // 根据中间件类型查询
        if (StringUtils.isNotEmpty(query.getMiddlewareType())) {
            wrapper.eq("type", query.getMiddlewareType());
        }
        // 根据告警等级查询
        if (StringUtils.isNotEmpty(query.getAlertLevel())) {
            wrapper.eq("level", query.getAlertLevel());
        }
        // 根据告警对象查询
        if (StringUtils.isNotEmpty(query.getAlertTarget())) {
            wrapper.eq("name", query.getAlertTarget());
        } else {
            wrapper.ne("name", "");
        }
        // 根据告警时间排序
        if (StringUtils.isNotEmpty(query.getAlertTime())) {
            if (query.getAlertTime().equals(ASC)) {
                wrapper.orderByAsc("alert_time");
            } else if (query.getAlertTime().equals(DESC)) {
                wrapper.orderByDesc("alert_time");
            }
        }
        // 根据告警接收时间排序
        if (StringUtils.isNotEmpty(query.getReceiveTime()) && query.getReceiveTime().equals(ASC)) {
            wrapper.orderByAsc("alert_receive_time");
        } else {
            wrapper.orderByDesc("alert_receive_time");
        }
        // keyword根据告警信息进行查询
        if (StringUtils.isNotEmpty(query.getKeyword())){
            wrapper.like("message", "%" + query.getKeyword() + "%");
        }
        // 查询告警记录数据
        List<BeanAlertRecord> alertRecordList = beanAlertRecordMapper.selectList(wrapper);
        return alertRecordList;
    }

    @Override
    public PageInfo<AlertDTO> pageAlertRecord(List<BeanAlertRecord> alertRecordList) {
        // 封装数据
        PageInfo<AlertDTO> alertDtoPageInfo = new PageInfo<>();

        PageInfo<BeanAlertRecord> alertRecordPageInfo = new PageInfo<>(alertRecordList);
        BeanUtils.copyProperties(alertRecordPageInfo, alertDtoPageInfo);

        // 获取集群别名
        Map<String, String> clusterAliasNameMap = clusterService.getClusterAliasName();
        // 处理告警信息为空的告警记录,并添加集群别名
        alertDtoPageInfo.setList(alertRecordPageInfo.getList().stream().map(beanAlertRecord -> {
            AlertDTO alertDTO = new AlertDTO();
            BeanUtils.copyProperties(beanAlertRecord, alertDTO);
            if (StringUtils.isEmpty(alertDTO.getMessage())) {
                alertDTO.setMessage(alertDTO.getSummary());
            }
            if (StringUtils.isNotEmpty(alertDTO.getClusterId())
                    && clusterAliasNameMap.containsKey(alertDTO.getClusterId())) {
                alertDTO.setNickname(clusterAliasNameMap.get(alertDTO.getClusterId()));
            }
            return alertDTO;
        }).collect(Collectors.toList()));
        return alertDtoPageInfo;
    }

    @Override
    public void alertTarget(AlertTargetDto alertTargetDto) {
        // 存在名称即为接入，不存在名称为新增；为新增添加随机UID
        if (StringUtils.isEmpty(alertTargetDto.getName())) {
            alertTargetDto.setName(UUIDUtils.get16UUID());
        }
        // 查询prometheusRule文件
        PrometheusRule prometheusRule = prometheusRuleService.get(alertTargetDto.getClusterId(), alertTargetDto.getNamespace(), alertTargetDto.getPrometheusRuleName());
        if(prometheusRule == null){
            throw new BusinessException(ErrorMessage.PROMETHEUS_RULES_NOT_EXIST);
        }
        // 更新labels
        Map<String, String> labels = new HashMap<>();
        if (prometheusRule.getMetadata().getLabels() != null){
            labels.putAll(prometheusRule.getMetadata().getLabels());
        }
        labels.put("platform", "zeus");

        prometheusRule.getMetadata().setLabels(labels);

        // 更新annotations
        Map<String, String> annotations = new HashMap<>();
        if (prometheusRule.getMetadata().getAnnotations() != null){
            annotations.putAll(prometheusRule.getMetadata().getAnnotations());
        }
        // 获取告警对象类型
        AlertTargetEnum alertTargetEnum = AlertTargetEnum.getByName(alertTargetDto.getName());
        String alertType = alertTargetEnum != null ? alertTargetEnum.getType() : CLUSTER;

        annotations.put("target_type", alertType);
        annotations.put("target_name", alertTargetDto.getName());
        annotations.put("target_alias_name", alertTargetDto.getAliasName());

        prometheusRule.getMetadata().setAnnotations(annotations);

        // 更新所有规则
        prometheusRule.getSpec().getGroups().forEach(prometheusRuleGroups -> {
            prometheusRuleGroups.getRules().forEach(prometheusRules -> {
                if (StringUtils.isNotEmpty(prometheusRules.getRecord())){
                    return;
                }
                // 修改labels
                Map<String, String> lab = new HashMap<>();
                if (prometheusRules.getLabels() != null){
                    lab.putAll(prometheusRules.getLabels());
                }
                lab.put("clusterId", alertTargetDto.getClusterId());
                lab.put("namespace", alertTargetDto.getNamespace());
                prometheusRules.setLabels(lab);

                // 修改annotations
                Map<String, String> ann = new HashMap<>();
                if (prometheusRules.getAnnotations() != null){
                    ann.putAll(prometheusRules.getAnnotations());
                }

                ann.put("target_type", alertType);
                ann.put("target_name", alertTargetDto.getName());
                ann.put("target_alias_name", alertTargetDto.getAliasName());
                prometheusRules.setAnnotations(ann);
            });
        });
        // update
        prometheusRuleService.update(alertTargetDto.getClusterId(), prometheusRule);
    }

    @Override
    @TranslateAfterResult
    public List<AlertTargetDto> alertTargetList(String clusterId) {
        // 初始化平台默认告警对象
        List<AlertTargetDto> alertTargetDtoList = Arrays.stream(AlertTargetEnum.values()).map(alertTargetEnum -> {
            AlertTargetDto alertTargetDto = new AlertTargetDto();
            alertTargetDto.setClusterId(clusterId);
            alertTargetDto.setName(alertTargetEnum.getName());
            alertTargetDto.setAliasName(alertTargetEnum.getAliasName());
            alertTargetDto.setAlertType(alertTargetEnum.getType());
            return alertTargetDto;
        }).collect(Collectors.toList());

        // 查询所有平台标记了的告警规则文件
        Map<String, String> labels = new HashMap<>();
        labels.put("platform", "zeus");
        List<PrometheusRule> prometheusRuleList = prometheusRuleService.list(clusterId, null, labels);
        // 检查告警规则中的标识字段
        checkPlatformLabels(clusterId, prometheusRuleList);
        // 根据告警对象名称转化为map结构
        Map<String,
            List<PrometheusRule>> prometheusRuleMap = prometheusRuleList.stream()
                .filter(prometheusRule -> prometheusRule.getMetadata().getAnnotations() != null
                    && prometheusRule.getMetadata().getAnnotations().containsKey("target_name"))
                .collect(Collectors
                    .groupingBy(prometheusRule -> prometheusRule.getMetadata().getAnnotations().get("target_name")));

        // 完善alertTargetDtoList数据
        for (AlertTargetDto alertTargetDto : alertTargetDtoList) {
            if (prometheusRuleMap.containsKey(alertTargetDto.getName())) {
                // 封装数据
                List<PrometheusRule> prList = prometheusRuleMap.get(alertTargetDto.getName());
                if (!CollectionUtils.isEmpty(prList)) {
                    PrometheusRule prometheusRule = prList.get(0);
                    alertTargetDto.setNamespace(prometheusRule.getMetadata().getNamespace());
                    alertTargetDto.setPrometheusRuleName(prometheusRule.getMetadata().getName());
                    alertTargetDto.setExist(true);
                    // 从map中移除该条数据
                    prometheusRuleMap.remove(alertTargetDto.getName());
                }
            }
        }

        // 存在非平台初始化告警对象
        if (!CollectionUtils.isEmpty(prometheusRuleMap)) {
            for (String key : prometheusRuleMap.keySet()) {
                AlertTargetDto alertTargetDto = new AlertTargetDto();
                // 获取prometheus数据对象
                List<PrometheusRule> prList = prometheusRuleMap.get(key);
                if (!CollectionUtils.isEmpty(prList)) {
                    PrometheusRule prometheusRule = prList.get(0);
                    // 设置集群id
                    alertTargetDto.setClusterId(clusterId);
                    // 设置告警类型
                    alertTargetDto.setAlertType(CLUSTER);
                    // 通过annotations获取别名
                    if (!CollectionUtils.isEmpty(prometheusRule.getMetadata().getAnnotations())) {
                        alertTargetDto.setName(prometheusRule.getMetadata().getAnnotations().get("target_name"));
                        alertTargetDto
                            .setAliasName(prometheusRule.getMetadata().getAnnotations().get("target_alias_name"));
                    }
                    // 设置分区和prometheusRule名称
                    alertTargetDto.setNamespace(prometheusRule.getMetadata().getNamespace());
                    alertTargetDto.setPrometheusRuleName(prometheusRule.getMetadata().getName());
                    alertTargetDto.setExist(true);

                    alertTargetDtoList.add(alertTargetDto);
                }
            }
        }

        return alertTargetDtoList;
    }

    @Override
    public List<MiddlewareAlertsDTO> alertRule(String targetName, String clusterId, String namespace,
        String prometheusRuleName) {
        // 构建查询labels
        Map<String, String> labels = new HashMap<>();
        labels.put(PLATFORM, ZEUS);
        // 查询 prometheusRule文件
        List<PrometheusRule> prometheusRuleList = prometheusRuleService.list(clusterId, null, labels);
        // 获取指定prometheusRule
        prometheusRuleList = prometheusRuleList.stream()
            .filter(prometheusRule -> prometheusRule.getMetadata().getAnnotations() != null
                && prometheusRule.getMetadata().getAnnotations().containsKey("target_name")
                && prometheusRule.getMetadata().getAnnotations().get("target_name").equals(targetName))
            .collect(Collectors.toList());
        if (CollectionUtils.isEmpty(prometheusRuleList)) {
            throw new BusinessException(ErrorMessage.PROMETHEUS_RULES_NOT_EXIST);
        }

        // 封装返回数据
        List<MiddlewareAlertsDTO> middlewareAlertsDTOList = new ArrayList<>();
        for (PrometheusRule prometheusRule : prometheusRuleList) {
            middlewareAlertsDTOList.addAll(prometheusRuleService.convertPrometheusRule(prometheusRule));
        }
        return middlewareAlertsDTOList;
    }

    @Override
    public List<AlertUserDto> alertUser(String clusterId, Boolean allocatable, Integer roleId) {
        if (allocatable) {
            return listAllocatableAlertUser(clusterId, roleId);
        } else {
            return listAlertUser(clusterId);
        }
    }

    @Override
    public void addAlertUser(AlertUserListDto alertUserListDto) {
        // 循环用户列表添加数据
        for (AlertUserDto alertUserDto : alertUserListDto.getAlertUserDtoList()) {
            AlertUserDo alertUserDo = new AlertUserDo();
            alertUserDo.setUsername(alertUserDto.getUsername());
            alertUserDo.setClusterId(alertUserListDto.getClusterId());
            alertUserDo.setMailAlert(alertUserDto.getMailAlert());
            alertUserDo.setMessageAlert(alertUserDto.getMessageAlert());
            // 添加平台告警用户
            alertUserService.add(alertUserDo.setAlertType(SYSTEM));
            // 添加集群告警用户
            alertUserService.add(alertUserDo.setAlertType(CLUSTER));
        }
    }

    @Override
    public void removeAlertUser(String username, String clusterId) {
        // 删除平台告警规则绑定的告警用户
        alertUserService.delete(username, clusterId, null, null, SYSTEM);
        // 删除集群告警规则绑定的告警用户
        alertUserService.delete(username, clusterId, null, null, CLUSTER);
    }

    @Override
    public void refreshPrometheusRulesLabels() {
        List<MiddlewareClusterDTO> middlewareClusterDTOList = clusterService.listClusters();
        for (MiddlewareClusterDTO middlewareClusterDTO : middlewareClusterDTOList) {
            String clusterId = middlewareClusterDTO.getId();
            // 查询所有平台标记了的告警规则文件
            Map<String, String> labels = new HashMap<>();
            labels.put("platform", "zeus");
            List<PrometheusRule> prometheusRuleList = prometheusRuleService.list(clusterId, null, labels);
            // 检查告警规则中的标识字段
            checkPlatformLabels(clusterId, prometheusRuleList);
        }
    }

    public List<AlertUserDto> listAlertUser(String clusterId){
        // 获取告警用户列表
        List<AlertUserDo> alertUserDoList = alertUserService.list(clusterId, null, null, CLUSTER);
        // 返回封装数据
        return alertUserDoList.stream().map(alertUserDo -> {
            AlertUserDto alertUserDto = new AlertUserDto();
            alertUserDto.setUsername(alertUserDo.getUsername());
            alertUserDto.setMailAlert(alertUserDo.getMailAlert());
            alertUserDto.setMessageAlert(alertUserDo.getMessageAlert());
            return alertUserDto;
        }).collect(Collectors.toList());
    }

    public List<AlertUserDto> listAllocatableAlertUser(String clusterId, Integer roleId) {
        // 获取告警用户列表
        List<AlertUserDo> alertUserDoList = alertUserService.list(clusterId, null, null, CLUSTER);
        // 获取用户集，并过滤掉已分配的用户
        List<UserDto> userDtoList = userService.list(null).stream()
            .filter(userDto -> alertUserDoList.stream()
                .noneMatch(alertUserDo -> alertUserDo.getUsername().equals(userDto.getUserName())))
            .filter(userDto -> !userDto.getUserName().equals(ADMIN)).collect(Collectors.toList());
        if (roleId != null) {
            userDtoList = userService.getUserRole(userDtoList);
            userDtoList = userDtoList.stream()
                .filter(userDto -> !CollectionUtils.isEmpty(userDto.getUserRoleList()) && userDto.getUserRoleList()
                    .stream().anyMatch(userRole -> userRole.getRoleId() != null && userRole.getRoleId().equals(roleId)))
                .collect(Collectors.toList());
        }
        // 返回封装数据
        return userDtoList.stream().map(userDto -> {
            AlertUserDto alertUserDto = new AlertUserDto();
            BeanUtils.copyProperties(userDto, alertUserDto);
            alertUserDto.setMail(userDto.getEmail());
            alertUserDto.setUsername(userDto.getUserName());
            return alertUserDto;
        }).collect(Collectors.toList());
    }

    public void checkPlatformLabels(String clusterId, List<PrometheusRule> prometheusRuleList) {
        for (PrometheusRule prometheusRule : prometheusRuleList) {
            boolean flag = false;
            for (PrometheusRuleGroups prometheusRuleGroups : prometheusRule.getSpec().getGroups()) {
                for (PrometheusRules prometheusRules : prometheusRuleGroups.getRules()) {
                    if (StringUtils.isEmpty(prometheusRules.getAlert())
                        || StringUtils.isNotEmpty(prometheusRules.getRecord())) {
                        continue;
                    }
                    Map<String, String> labels = prometheusRules.getLabels();
                    if (labels == null) {
                        labels = new HashMap<>();
                    }
                    if (!labels.containsKey("clusterId") || !labels.containsKey("namespace")) {
                        labels.put("clusterId", clusterId);
                        labels.put("namespace", prometheusRule.getMetadata().getNamespace());
                        flag = true;
                    }
                    prometheusRules.setLabels(labels);

                    Map<String, String> annotations = prometheusRules.getAnnotations();
                    if (annotations == null) {
                        annotations = new HashMap<>();
                    }
                    if (!annotations.containsKey("target_type") || !annotations.containsKey("target_name")
                        || !annotations.containsKey("target_alias_name")) {
                        if (!CollectionUtils.isEmpty(prometheusRule.getMetadata().getAnnotations())) {
                            Map<String, String> parentAnn = prometheusRule.getMetadata().getAnnotations();
                            if (parentAnn.containsKey("target_type")) {
                                annotations.put("target_type", parentAnn.get("target_type"));
                            }
                            if (parentAnn.containsKey("target_name")) {
                                annotations.put("target_name", parentAnn.get("target_name"));
                            }
                            if (parentAnn.containsKey("target_alias_name")) {
                                annotations.put("target_alias_name", parentAnn.get("target_alias_name"));
                            }
                            flag = true;
                        }
                    }
                    prometheusRules.setAnnotations(annotations);
                }
            }
            if (flag) {
                prometheusRuleService.update(clusterId, prometheusRule);
            }
        }
    }
}
