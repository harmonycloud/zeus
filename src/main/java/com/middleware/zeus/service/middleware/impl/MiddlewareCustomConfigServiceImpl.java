package com.middleware.zeus.service.middleware.impl;

import java.text.MessageFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.middleware.tool.collection.MapUtils;
import com.middleware.zeus.integration.cluster.bean.MiddlewareCR;
import com.middleware.zeus.integration.cluster.bean.MiddlewareInfo;
import com.middleware.zeus.operator.BaseOperator;
import com.middleware.zeus.service.k8s.*;
import com.middleware.caas.common.model.middleware.*;
import com.middleware.zeus.service.AbstractBaseService;
import com.middleware.zeus.service.k8s.*;
import com.middleware.zeus.service.middleware.CustomConfigHistoryService;
import com.middleware.zeus.service.middleware.MiddlewareCustomConfigService;
import com.middleware.zeus.service.middleware.MiddlewareService;
import com.middleware.zeus.service.registry.HelmChartService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.yaml.snakeyaml.Yaml;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.middleware.caas.common.enums.ErrorMessage;
import com.middleware.caas.common.enums.middleware.MiddlewareTypeEnum;
import com.middleware.caas.common.exception.BusinessException;
import com.middleware.caas.common.exception.CaasRuntimeException;
import com.middleware.caas.common.model.registry.HelmChartFile;
import com.middleware.caas.common.util.ThreadPoolExecutorFactory;
import com.middleware.tool.date.DateUtils;
import com.middleware.zeus.bean.BeanCustomConfig;
import com.middleware.zeus.bean.BeanCustomConfigHistory;
import com.middleware.zeus.bean.BeanMiddlewareParamTop;
import com.middleware.zeus.dao.BeanCustomConfigMapper;
import com.middleware.zeus.dao.BeanMiddlewareParamTopMapper;

import lombok.extern.slf4j.Slf4j;

import static com.middleware.caas.common.constants.NameConstant.PROXY;
import static com.middleware.caas.common.constants.NameConstant.SENTINEL;
import static com.middleware.caas.common.constants.middleware.MiddlewareConstant.ASCEND;

/**
 * @author xutianhong
 * @Date 2021/4/23 4:32 下午
 */
@Service
@Slf4j
public class MiddlewareCustomConfigServiceImpl extends AbstractBaseService implements MiddlewareCustomConfigService {
    
    @Autowired
    private PodService podService;
    @Autowired
    protected ConfigMapService configMapService;
    @Autowired
    private BeanCustomConfigMapper beanCustomConfigMapper;
    @Autowired
    private HelmChartService helmChartService;
    @Autowired
    private ClusterService clusterService;
    @Autowired
    private K8sExecService k8sExecService;
    @Autowired
    private CustomConfigHistoryService customConfigHistoryService;
    @Autowired
    private BeanMiddlewareParamTopMapper beanMiddlewareParamTopMapper;
    @Autowired
    private MiddlewareCRService middlewareCRService;
    @Autowired
    private MiddlewareService middlewareService;

    @Override
    public List<CustomConfig> listCustomConfig(String clusterId, String namespace, String middlewareName, String type, String order, String role)
        throws Exception {
        Middleware middleware = new Middleware(clusterId, namespace, middlewareName, type);
        MiddlewareClusterDTO cluster = clusterService.findById(clusterId);
        // 获取values
        JSONObject values = helmChartService.getInstalledValues(middlewareName, namespace, cluster);
        // 获取configs
        String podType = getOperator(BaseOperator.class, BaseOperator.class, middleware).getPodType(role);
        Map<String, String> data = getConfigFromValues(middleware, values, podType);
        // 取出chartVersion
        middleware.setChartVersion(values.getString("chart-version"));
        // 获取数据库数据
        QueryWrapper<BeanCustomConfig> wrapper = new QueryWrapper<BeanCustomConfig>()
            .eq("chart_name", middleware.getType()).eq("chart_version", middleware.getChartVersion()).eq("role",role);
        List<BeanCustomConfig> beanCustomConfigList = beanCustomConfigMapper.selectList(wrapper);
        // 查询修改历史
        Map<String, List<BeanCustomConfigHistory>> beanCustomConfigHistoryListMap =
            customConfigHistoryService.get(clusterId, namespace, middlewareName, role).stream()
                .collect(Collectors.groupingBy(BeanCustomConfigHistory::getItem));
        orderByUpdateTime(beanCustomConfigHistoryListMap);
        //查询置顶参数
//        QueryWrapper<BeanMiddlewareParamTop> wrapper1 = new QueryWrapper<BeanMiddlewareParamTop>()
//            .eq("cluster_id", clusterId).eq("namespace", namespace).eq("name", middlewareName);
//        List<BeanMiddlewareParamTop> beanMiddlewareParamTopList = beanMiddlewareParamTopMapper.selectList(wrapper1);
        // 封装customConfigList
        List<CustomConfig> customConfigList = new ArrayList<>();
        beanCustomConfigList.forEach(beanCustomConfig -> {
            CustomConfig customConfig = new CustomConfig();
            BeanUtils.copyProperties(beanCustomConfig, customConfig);
            customConfig.setValue(data.getOrDefault(customConfig.getName(), ""));
            customConfig.setParamType(customConfig.getRanges().contains("|") ? "select" : "input");
            // 设置最近一次修改时间
            if (beanCustomConfigHistoryListMap.containsKey(customConfig.getName())) {
                customConfig.setUpdateTime(beanCustomConfigHistoryListMap.get(customConfig.getName()).get(0).getDate());
            }
            // 特殊处理mysql相关内容
            if ("sql_mode".equals(beanCustomConfig.getName())) {
                customConfig.setParamType("multiSelect");
            }
//            if (beanMiddlewareParamTopList.stream()
//                .anyMatch(beanMiddlewareParamTop -> beanMiddlewareParamTop.getParam().equals(customConfig.getName()) && beanMiddlewareParamTop.getRole().equals(role))) {
//                customConfig.setTopping(true);
//            }
            customConfigList.add(customConfig);
        });
        customConfigList.sort((o1, o2) -> o1.getTopping() == null && o2.getTopping() == null ? 0 : o1.getTopping() == null ? 1 : o2.getTopping() == null ? -1 : 0);
        return sortByOrder(customConfigList, order);
    }

    @Override
    public void updateCustomConfig(MiddlewareCustomConfig config) {
        check(config);
        MiddlewareClusterDTO cluster = clusterService.findById(config.getClusterId());
        Middleware middleware =
            new Middleware(config.getClusterId(), config.getNamespace(), config.getName(), config.getType());
        // 获取节点类型
        String podType = getOperator(BaseOperator.class, BaseOperator.class, middleware).getPodType(config.getRole());
        // 获取values
        JSONObject values = helmChartService.getInstalledValues(config.getName(), config.getNamespace(), cluster);
        // 获取configs
        Map<String, String> data = getConfigFromValues(middleware, values, podType);
        if (CollectionUtils.isEmpty(data)) {
            // 从parameter.yaml文件创建一份
            QueryWrapper<BeanCustomConfig> wrapper = new QueryWrapper<BeanCustomConfig>()
                .eq("chart_name", middleware.getType()).eq("chart_version", values.getString("chart-version")).eq("role", config.getRole());
            List<BeanCustomConfig> beanCustomConfigList = beanCustomConfigMapper.selectList(wrapper);
            beanCustomConfigList.forEach(c -> data.put(c.getName(), c.getDefaultValue()));
        }
        // 取出chartVersion
        middleware.setChartVersion(values.getString("chart-version"));
        middleware.setChartName(config.getType());
        // 记录当前数据
        Map<String, String> oldDate = new HashMap<>(data);
        // 更新配置，并记录是否重启
        for (CustomConfig customConfig : config.getCustomConfigList()) {
            // 确认正则匹配
            if (!checkPattern(customConfig)) {
                log.error("集群{} 分区{} 中间件{} 参数{} 正则校验失败", config.getClusterId(), config.getNamespace(), config.getName(),
                    customConfig.getName());
                throw new CaasRuntimeException(ErrorMessage.VALIDATE_FAILED);
            }
            data.put(customConfig.getName(), customConfig.getValue());
        }
        // mysql和redis手动执行参数设置
        if ((config.getType().equals(MiddlewareTypeEnum.MYSQL.getType())
            || config.getType().equals(MiddlewareTypeEnum.REDIS.getType())) && config.getRole().equalsIgnoreCase("major")) {
            doUpdateCustomConfig(config, cluster, config.getType());
        }

        updateValues(middleware, data, cluster, values, podType);
        // 添加修改历史
        customConfigHistoryService.insert(config.getName(), oldDate, config);
        // 重启服务
        if (config.getReboot() != null && config.getReboot()
            && !config.getType().equals(MiddlewareTypeEnum.POSTGRESQL.getType())) {
            middlewareService.reboot(config.getClusterId(), config.getNamespace(), config.getName(), config.getType(), podType);
        }
    }

    @Override
    public List<CustomConfigHistoryDTO> getCustomConfigHistory(String clusterId, String namespace,
        String middlewareName, String type, String item, String startTime, String endTime, String role) {
        // 更新状态
        updateStatus(clusterId, namespace, middlewareName, type, role);

        // 查询数据库历史
        List<BeanCustomConfigHistory> beanCustomConfigHistoryList =
                customConfigHistoryService.get(clusterId, namespace, middlewareName, role);

        // 筛选名称
        if (StringUtils.isNotEmpty(item)) {
            beanCustomConfigHistoryList = beanCustomConfigHistoryList.stream()
                .filter(
                    beanCustomConfigHistory -> StringUtils.containsIgnoreCase(beanCustomConfigHistory.getItem(), item))
                .collect(Collectors.toList());
        }
        // 过滤时间
        if (StringUtils.isNotEmpty(startTime) && StringUtils.isNotEmpty(endTime)) {
            Date start = DateUtils.parseUTCDate(startTime);
            Date end = DateUtils.parseUTCDate(endTime);
            beanCustomConfigHistoryList = beanCustomConfigHistoryList.stream()
                .filter(beanCustomConfigHistory -> beanCustomConfigHistory.getDate().after(start)
                    && beanCustomConfigHistory.getDate().before(end))
                .collect(Collectors.toList());
        }
        List<CustomConfigHistoryDTO> customConfigHistoryDTOList = new ArrayList<>();
        beanCustomConfigHistoryList.forEach(beanCustomConfigHistory -> {
            CustomConfigHistoryDTO customConfigHistoryDTO = new CustomConfigHistoryDTO();
            BeanUtils.copyProperties(beanCustomConfigHistory, customConfigHistoryDTO);
            if (!customConfigHistoryDTO.getStatus()) {
                if (!customConfigHistoryDTO.getRestart()
                    && DateUtils.getIntervalDays(new Date(), customConfigHistoryDTO.getDate()) > 15) {
                    customConfigHistoryDTO.setStatus(true);
                }
            }
            customConfigHistoryDTOList.add(customConfigHistoryDTO);
        });
        customConfigHistoryDTOList.sort(
            (o1, o2) -> o1.getDate() == null ? -1 : o2.getDate() == null ? -1 : o2.getDate().compareTo(o1.getDate()));
        return customConfigHistoryDTOList;
    }

    @Override
    public List<BeanCustomConfig> updateConfig2MySQL(HelmChartFile helmChartFile) throws Exception {
        List<BeanCustomConfig> resultList = new ArrayList<>();
        try {
            QueryWrapper<BeanCustomConfig> wrapper = new QueryWrapper<BeanCustomConfig>()
                    .eq("chart_name", helmChartFile.getChartName()).eq("chart_version", helmChartFile.getChartVersion());
            // 清除旧数据
            beanCustomConfigMapper.delete(wrapper);
            Yaml yaml = new Yaml();
            String parameters = helmChartFile.getYamlFileMap().get("parameters");
            if (StringUtils.isNotBlank(parameters)) {
                Map<String, Object> paramMap = MapUtils.stringToHashMap(parameters);
                for (String key: paramMap.keySet()) {
                    JSONObject data = yaml.loadAs((String) paramMap.get(key), JSONObject.class);
                    resultList.addAll(updateConfig2MySQL(data, key, helmChartFile.getChartName(),
                        helmChartFile.getChartVersion()));
                }
            }
        } catch (Exception e) {
            throw new CaasRuntimeException(ErrorMessage.MIDDLEWARE_UPDATE_MYSQL_CONFIG_FAILED);
        }
        return resultList;
    }

    @Override
    public List<BeanCustomConfig> updateConfig2MySQL(JSONObject data, String role, String chartName, String chartVersion) {
        // 转换为对象
        CustomConfigParameters parameters =
                JSONObject.parseObject(JSONObject.toJSONString(data), CustomConfigParameters.class);
        List<BeanCustomConfig> beanCustomConfigList = new ArrayList<>();
        parameters.getParameters().forEach(map -> {
            for (String key : map.keySet()) {
                Map<String, String> param = map.get(key).stream()
                        .collect(Collectors.toMap(CustomConfigParameter::getName, CustomConfigParameter::getValue));
                // 封装数据库对象
                BeanCustomConfig beanCustomConfig = new BeanCustomConfig();
                beanCustomConfig.setName(key);
                beanCustomConfig.setDefaultValue(param.get("default"));
                beanCustomConfig.setRestart("y".equals(param.get("isReboot")));
                beanCustomConfig.setRanges(param.get("range"));
                beanCustomConfig.setDescription(param.get("describe"));
                beanCustomConfig.setChartName(chartName);
                beanCustomConfig.setChartVersion(chartVersion);
                beanCustomConfig.setRole(role);
                if (param.containsKey("pattern")) {
                    beanCustomConfig.setPattern(param.get("pattern"));
                }
                // 存入数据库
                beanCustomConfigMapper.insert(beanCustomConfig);
                beanCustomConfigList.add(beanCustomConfig);
            }
        });
        return beanCustomConfigList;
    }


    @Override
    public void deleteHistory(String clusterId, String namespace, String name) {
        customConfigHistoryService.delete(clusterId, namespace, name);
    }

//    @Override
//    public void topping(String clusterId, String namespace, String name, String configName, String type, String role) {
//        QueryWrapper<BeanMiddlewareParamTop> wrapper = new QueryWrapper<BeanMiddlewareParamTop>()
//            .eq("cluster_id", clusterId).eq("namespace", namespace).eq("name", name).eq("param", configName).eq("role", role);
//        List<BeanMiddlewareParamTop> exist = beanMiddlewareParamTopMapper.selectList(wrapper);
//        if (CollectionUtils.isEmpty(exist)) {
//            BeanMiddlewareParamTop beanMiddlewareParamTop = new BeanMiddlewareParamTop();
//            beanMiddlewareParamTop.setClusterId(clusterId);
//            beanMiddlewareParamTop.setNamespace(namespace);
//            beanMiddlewareParamTop.setName(name);
//            beanMiddlewareParamTop.setParam(configName);
//            beanMiddlewareParamTop.setRole(role);
//            beanMiddlewareParamTopMapper.insert(beanMiddlewareParamTop);
//        } else {
//            beanMiddlewareParamTopMapper.delete(wrapper);
//        }
//    }

    @Override
    public List<String> getRoles(String clusterId, String namespace, String middlewareName, String type) throws Exception {
        Middleware middleware = new Middleware(clusterId, namespace, middlewareName, type);
        MiddlewareClusterDTO cluster = clusterService.findById(clusterId);
        // 获取values
        JSONObject values = helmChartService.getInstalledValues(middlewareName, namespace, cluster);
        // 取出chartVersion
        middleware.setChartVersion(values.getString("chart-version"));
        QueryWrapper<BeanCustomConfig> wrapper = new QueryWrapper<BeanCustomConfig>()
                .eq("chart_name", type).eq("chart_version", middleware.getChartVersion())
                .select("distinct role").orderByAsc("role");
        List<String> resultList = beanCustomConfigMapper.selectObjs(wrapper).stream().map(Object::toString).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(resultList)) {
            HelmChartFile helmChart = helmChartService.getHelmChartFromMysql(type, middleware.getChartVersion());
            resultList.addAll(updateConfig2MySQL(helmChart).stream().map(BeanCustomConfig::getRole).distinct().collect(Collectors.toList()));
        }
        if (CollectionUtils.isEmpty(resultList)) {
            throw new BusinessException(ErrorMessage.GET_CUSTOM_CONFIG_ROLE_FAILED);
        }
        Set<String> roleSet = getMiddlewareCustomConfigRole(clusterId, namespace, middlewareName, type);
        return resultList.stream().filter(roleSet::contains).collect(Collectors.toList());
    }

    public Set<String> getMiddlewareCustomConfigRole(String clusterId, String namespace, String name, String type) {
        Middleware middleware = new Middleware(clusterId, namespace, name, type);
        MiddlewareCR cr = middlewareCRService.getCR(clusterId, namespace, type, name);
        BaseOperator operator = getOperator(BaseOperator.class, BaseOperator.class, middleware);
        HashSet<String> resultSet = new HashSet<>();
        if (cr.getStatus() != null && cr.getStatus().getInclude() != null
                && cr.getStatus().getInclude().containsKey("pods")
                && !CollectionUtils.isEmpty(cr.getStatus().getInclude().get("pods"))) {
            cr.getStatus().getInclude().get("pods").forEach(pod -> {
                String role = operator.getCustomConfigRole(pod.getType());
                if (role != null) {
                    resultSet.add(role);
                }
            });
        }
        return resultSet;
    }

    /**
     * 拉一个线程去更新是否已启用的状态
     */
    public void updateStatus(String clusterId, String namespace, String middlewareName, String type, String customConfigRole) {
        // 查询数据库历史
        List<BeanCustomConfigHistory> beanCustomConfigHistoryList =
                customConfigHistoryService.get(clusterId, namespace, middlewareName, customConfigRole);
        // 过滤获取状态未生效的参数历史
        beanCustomConfigHistoryList =
                beanCustomConfigHistoryList.stream().filter(ch -> !ch.getStatus()).collect(Collectors.toList());
        // 状态皆为已生效 直接返回
        if (CollectionUtils.isEmpty(beanCustomConfigHistoryList)) {
            return;
        }
        // 存在需重启的参数
        List<PodInfo> podInfoList = new ArrayList<>();
        if (beanCustomConfigHistoryList.stream().anyMatch(BeanCustomConfigHistory::getRestart)) {
            podInfoList.addAll(podService.listMiddlewarePods(clusterId, namespace, middlewareName, type));
        }
        // 状态处理
        Middleware middleware = new Middleware(clusterId, namespace, middlewareName, type);

        beanCustomConfigHistoryList.forEach(customConfigHistoryDTO -> {
            String podType = getOperator(BaseOperator.class, BaseOperator.class, middleware).getPodType(customConfigHistoryDTO.getRole());
            boolean status = true;
            // 处理需重启的参数，根据pod重启时间判断
            if (customConfigHistoryDTO.getRestart()) {
                for (PodInfo podInfo : podInfoList) {
                    if (StringUtils.isNotEmpty(podInfo.getRole()) || !podInfo.getRole().equals(podType)) {
                        continue;
                    }
                    Date date = DateUtils.addInteger(
                        DateUtils.parseDate(StringUtils.isEmpty(podInfo.getLastRestartTime()) ? podInfo.getCreateTime()
                            : podInfo.getLastRestartTime(), DateUtils.YYYY_MM_DD_T_HH_MM_SS_Z),
                        Calendar.HOUR_OF_DAY, 8);
                    if (customConfigHistoryDTO.getDate().after(date)) {
                        status = false;
                    }
                }
                // 处理不需重启的参数  15s生效
            } else {
                Date now = new Date();
                status = DateUtils.getIntervalDays(now, customConfigHistoryDTO.getDate()) > 15;
            }
            if (status) {
                BeanCustomConfigHistory beanCustomConfigHistory = new BeanCustomConfigHistory();
                BeanUtils.copyProperties(customConfigHistoryDTO, beanCustomConfigHistory);
                beanCustomConfigHistory.setStatus(true);
                customConfigHistoryService.update(beanCustomConfigHistory);
            }
        });
    }

    /**
     * 正则匹配
     */
    public boolean checkPattern(CustomConfig customConfig) {
        if (StringUtils.isNotEmpty(customConfig.getPattern())) {
            return Pattern.matches(customConfig.getPattern(), customConfig.getValue());
        }
        return true;
    }

    public Map<String, String> getConfigFromValues(Middleware middleware, JSONObject values, String podType) {
        Map<String, String> data = new HashMap<>();
        JSONObject args;
        if (podType.equalsIgnoreCase("Master") && values.containsKey("args")) {
            args = values.getJSONObject("args");
        } else if (values.containsKey(podType) && values.getJSONObject(podType).containsKey("args")) {
            args = values.getJSONObject(podType).getJSONObject("args");
        } else {
            return data;
        }
        for (String key : args.keySet()) {
            data.put(key, args.getString(key));
        }
        return data;
    }

    /**
     * 更新values.yaml
     */
    public void updateValues(Middleware middleware, Map<String, String> dataMap, MiddlewareClusterDTO cluster,
        JSONObject values, String podType) {
        JSONObject newValues = JSONObject.parseObject(values.toJSONString());
        JSONObject args;
        if (podType.equalsIgnoreCase("Master")) {
            args = newValues.getJSONObject("args");
        } else if (!newValues.containsKey(podType)) {
            args = new JSONObject();
            newValues.put(podType, args);
        } else {
            args = newValues.getJSONObject(podType).getJSONObject("args");
        }
        if (args == null) {
            args = new JSONObject();
        }
        for (String key : dataMap.keySet()) {
            if (StringUtils.isEmpty(dataMap.get(key))) {
                continue;
            }
            args.put(key, dataMap.get(key));
        }
        if (podType.equalsIgnoreCase("Master")) {
            newValues.put("args", args);
        } else {
            newValues.getJSONObject(podType).put("args", args);
        }
        helmChartService.upgrade(middleware, values, newValues, cluster);
    }

    public void doUpdateCustomConfig(MiddlewareCustomConfig config, MiddlewareClusterDTO cluster, String type) {
        if ((config.getType().equals(MiddlewareTypeEnum.MYSQL.getType()))){
            try{
                doSetGlobalMysql(config, cluster);
            }catch (Exception e){
                log.error(ErrorMessage.MYSQL_CONFIG_UPDATE_FAILED.getZhMsg(), e);
                throw new BusinessException(ErrorMessage.MYSQL_CONFIG_UPDATE_FAILED);
            }
        }else if((config.getType().equals(MiddlewareTypeEnum.REDIS.getType()))){
            try{
                doConfigSetRedis(config, cluster);
            }catch (Exception e){
                log.error(ErrorMessage.REDIS_CONFIG_UPDATE_FAILED.getZhMsg(), e);
                throw new BusinessException(ErrorMessage.REDIS_CONFIG_UPDATE_FAILED);
            }
        }
    }

    private void doConfigSetRedis(MiddlewareCustomConfig config, MiddlewareClusterDTO cluster) {
        // 获取数据库密码
        JSONObject values = helmChartService.getInstalledValues(config.getName(), config.getNamespace(), cluster);
        String password = values.getString("redisPassword");
        // 获取端口
        String port;
        if (values.containsKey("redisServicePort")) {
            port = values.getString("redisServicePort");
        } else if (values.containsKey("redis") && values.getJSONObject("redis").containsKey("port")) {
            port = values.getJSONObject("redis").getString("port");
        } else {
            port = "6379";
        }
        // 获取pod列表
        MiddlewareCR middlewareCr = middlewareCRService.getCR(cluster.getId(), config.getNamespace(),
                MiddlewareTypeEnum.REDIS.getType(), config.getName());
        // 拼接redis-cli命令
        StringBuilder sb = new StringBuilder();
        config.getCustomConfigList().forEach(customConfig -> {
            if (!customConfig.getRestart()) {
                sb.append("config set ").append(customConfig.getName()).append(" ").append(" '").append(customConfig.getValue()).append("' ").append("\\n");
            }
        });
        // 没有要更新的则不执行
        if ("''".equals(sb.toString())) {
            return;
        }
        // 主从节点执行命令
        if (middlewareCr.getStatus() == null || middlewareCr.getStatus().getInclude() == null
                || !middlewareCr.getStatus().getInclude().containsKey("pods")) {
            throw new BusinessException(ErrorMessage.FIND_POD_IN_MIDDLEWARE_FAIL);
        }
        middlewareCr.getStatus().getInclude().get("pods").forEach(pods -> {
            if ("master".equals(pods.getType()) || "slave".equals(pods.getType())) {
                String execCommand = MessageFormat.format(
                        "kubectl exec {0} -n {1} -c redis-cluster --server={2} --token={3} --insecure-skip-tls-verify=true " +
                                "-- bash -c '' redis-cli -h $PODIP -p $REDIS_INSTANCE_PORT -a {4} {5} ''",
                        pods.getName(), config.getNamespace(), cluster.getAddress(), cluster.getAccessToken(),
                        password, sb.toString());
                k8sExecService.exec(execCommand);
            }
        });
    }

    public void doSetGlobalMysql(MiddlewareCustomConfig config, MiddlewareClusterDTO cluster){
        // 获取数据库密码
        JSONObject values = helmChartService.getInstalledValues(config.getName(), config.getNamespace(), cluster);
        String password = values.getJSONObject("args").getString("root_password");
        // 获取pod列表
        MiddlewareCR middlewareCr = middlewareCRService.getCR(cluster.getId(), config.getNamespace(),
                MiddlewareTypeEnum.MYSQL.getType(), config.getName());
        // 拼接数据库语句
        StringBuilder sb = new StringBuilder();
        config.getCustomConfigList().forEach(customConfig -> {
            if (!customConfig.getRestart()) {
                sb.append("set global ").append(customConfig.getName()).append("=");
                if (isNum(customConfig.getValue())) {
                    sb.append(customConfig.getValue()).append(";");
                } else {
                    sb.append("'").append(customConfig.getValue()).append("'").append(";");
                }
            }
        });
        // 主从节点执行命令
        if (middlewareCr.getStatus() == null || middlewareCr.getStatus().getInclude() == null
                || !middlewareCr.getStatus().getInclude().containsKey("pods")) {
            throw new BusinessException(ErrorMessage.FIND_POD_IN_MIDDLEWARE_FAIL);
        }
        middlewareCr.getStatus().getInclude().get("pods").forEach(pods -> {
            if ("Master".equals(pods.getType()) || "Slave".equals(pods.getType())) {
                String execCommand = MessageFormat.format(
                        "kubectl exec {0} -n {1} -c mysql --server={2} --token={3} --insecure-skip-tls-verify=true -- mysql -uroot -p{4} -S /data/mysql/db_{5}/conf/mysql.sock -e \"{6}\"",
                        pods.getName(), config.getNamespace(), cluster.getAddress(), cluster.getAccessToken(), password,
                        config.getName(), sb.toString());
                k8sExecService.exec(execCommand);
            }
        });
    }

    /**
     * 自定义参数校验
     */
    public void check(MiddlewareCustomConfig config) {
        if (CollectionUtils.isEmpty(config.getCustomConfigList())) {
            throw new BusinessException(ErrorMessage.CUSTOM_CONFIG_IS_EMPTY);
        }
        config.getCustomConfigList().forEach(customConfig -> {
            if (StringUtils.isEmpty(customConfig.getValue())) {
                log.error(ErrorMessage.CUSTOM_CONFIG_VALUE_IS_EMPTY.getZhMsg() + " :" + customConfig.getName());
                throw new BusinessException(ErrorMessage.CUSTOM_CONFIG_VALUE_IS_EMPTY, customConfig.getName());
            }
        });
    }

    /**
     * 校验字符串是否为纯数字
     */
    public Boolean isNum(String str){
        Pattern pattern = Pattern.compile("[0-9]*");
        Matcher isNum = pattern.matcher(str);
        return isNum.matches();
    }

    /**
     * 更具修改时间进行排序
     */
    public void orderByUpdateTime(Map<String, List<BeanCustomConfigHistory>> beanCustomConfigHistoryListMap) {
        for (String key : beanCustomConfigHistoryListMap.keySet()) {
            beanCustomConfigHistoryListMap.get(key).sort((o1, o2) -> o1.getDate() == null ? -1
                : o2.getDate() == null ? -1 : o2.getDate().compareTo(o1.getDate()));
        }
    }
    
    public List<CustomConfig> sortByOrder(List<CustomConfig> customConfigList, String order) {
        if (StringUtils.isEmpty(order)) {
            return customConfigList;
        }
        List<CustomConfig> top = customConfigList.stream()
            .filter(customConfig -> customConfig.getTopping() != null && customConfig.getTopping())
            .collect(Collectors.toList());
        customConfigList.removeAll(top);
        top.sort((o1, o2) -> o1.getUpdateTime() == null && o2.getUpdateTime() == null ? 0
            : o1.getUpdateTime() == null ? 1 : o2.getUpdateTime() == null ? -1 : ASCEND.equals(order)
                ? o1.getUpdateTime().compareTo(o2.getUpdateTime()) : o2.getUpdateTime().compareTo(o1.getUpdateTime()));
        customConfigList.sort((o1, o2) -> o1.getUpdateTime() == null && o2.getUpdateTime() == null ? 0
            : o1.getUpdateTime() == null ? 1 : o2.getUpdateTime() == null ? -1 : ASCEND.equals(order)
                ? o1.getUpdateTime().compareTo(o2.getUpdateTime()) : o2.getUpdateTime().compareTo(o1.getUpdateTime()));
        top.addAll(customConfigList);
        return top;
    }

}
