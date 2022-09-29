package com.harmonycloud.zeus.service.middleware.impl;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.harmonycloud.caas.common.enums.ErrorMessage;
import com.harmonycloud.caas.common.exception.BusinessException;
import com.harmonycloud.caas.common.model.registry.HelmChartFile;
import com.harmonycloud.zeus.bean.BeanMiddlewareCrType;
import com.harmonycloud.zeus.bean.BeanMiddlewareInfo;
import com.harmonycloud.zeus.dao.BeanMiddlewareCrTypeMapper;
import com.harmonycloud.zeus.service.middleware.MiddlewareCrTypeService;
import com.harmonycloud.zeus.service.middleware.MiddlewareInfoService;
import com.harmonycloud.zeus.service.registry.HelmChartService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.yaml.snakeyaml.Yaml;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.harmonycloud.caas.common.constants.middleware.MiddlewareConstant.*;

/**
 * @author xutianhong
 * @Date 2022/6/9 2:10 下午
 */
@Service
@Slf4j
public class MiddlewareCrTypeServiceImpl implements MiddlewareCrTypeService {

    private static final Map<String, String> MIDDLEWARE_CR_TYPE = new HashMap<>();

    @Autowired
    private BeanMiddlewareCrTypeMapper beanMiddlewareCrTypeMapper;
    @Autowired
    private HelmChartService helmChartService;
    @Autowired
    private MiddlewareInfoService middlewareInfoService;

    @PostConstruct
    @Override
    public void init() {
        // 通过数据库初始化
        QueryWrapper<BeanMiddlewareCrType> wrapper = new QueryWrapper<>();
        List<BeanMiddlewareCrType> beanMiddlewareCrTypeList = beanMiddlewareCrTypeMapper.selectList(wrapper);
        if (!CollectionUtils.isEmpty(beanMiddlewareCrTypeList)){
            for (BeanMiddlewareCrType beanMiddlewareCrType : beanMiddlewareCrTypeList){
                MIDDLEWARE_CR_TYPE.put(beanMiddlewareCrType.getChartName(), beanMiddlewareCrType.getCrType());
            }
        }
        // 通过chart包初始化
        List<BeanMiddlewareInfo> beanMwInfoList = middlewareInfoService.list(false);
        // 过滤数据库中已存在的type
        beanMwInfoList = beanMwInfoList.stream().filter(mwInfo -> beanMiddlewareCrTypeList.stream().noneMatch(mwCrType -> mwCrType.getChartName().equals(mwInfo.getChartName()))).collect(Collectors.toList());
        for (BeanMiddlewareInfo mwInfo : beanMwInfoList) {
            // 解析chart包 获取crType
            String crType = getCrTypeByChart(mwInfo.getChartName(), mwInfo.getChartVersion());
            // 更新map
            MIDDLEWARE_CR_TYPE.put(mwInfo.getChartName(), crType);
            // 更新数据库
            BeanMiddlewareCrType beanMiddlewareCrType = new BeanMiddlewareCrType();
            beanMiddlewareCrType.setChartName(mwInfo.getChartName());
            beanMiddlewareCrType.setCrType(crType);
            beanMiddlewareCrTypeMapper.insert(beanMiddlewareCrType);
        }
    }

    @Override
    public void put(String type, String crType) {
        MIDDLEWARE_CR_TYPE.put(type, crType);
        // 更新数据库
        QueryWrapper<BeanMiddlewareCrType> wrapper = new QueryWrapper<BeanMiddlewareCrType>().eq("chart_name", type);
        if (beanMiddlewareCrTypeMapper.selectOne(wrapper) == null){
            BeanMiddlewareCrType beanMiddlewareCrType = new BeanMiddlewareCrType();
            beanMiddlewareCrType.setChartName(type);
            beanMiddlewareCrType.setCrType(crType);
            beanMiddlewareCrTypeMapper.insert(beanMiddlewareCrType);
        }
    }

    @Override
    public String findByType(String type) {
        // 首先从map中获取
        if (!MIDDLEWARE_CR_TYPE.containsKey(type)){
            init();
        }
        return MIDDLEWARE_CR_TYPE.getOrDefault(type, type);
    }

    @Override
    public String findTypeByCrType(String crType) {
        if (!MIDDLEWARE_CR_TYPE.containsValue(crType)){
            init();
        }
        for (String key : MIDDLEWARE_CR_TYPE.keySet()){
            if (crType.equals(MIDDLEWARE_CR_TYPE.get(key))){
                return key;
            }
        }
        return crType;
    }

    @Override
    public Boolean isType(String type){
        return MIDDLEWARE_CR_TYPE.containsKey(type);
    }

    @Override
    public Boolean isCrType(String crType){
        return MIDDLEWARE_CR_TYPE.containsValue(crType);
    }

    @Override
    public String getCrTypeByChart(String chartName, String chartVersion){
        // 自动设置chart-version
        if (chartVersion == null){
            List<BeanMiddlewareInfo> mwInfoList = middlewareInfoService.listByType(chartName);
            if (CollectionUtils.isEmpty(mwInfoList)){
                log.error("数据库查找{}类型中间件失败，未找到", chartName);
                throw new BusinessException(ErrorMessage.NOT_EXIST);
            }
            chartVersion = mwInfoList.get(0).getChartVersion();
        }
        // 查询指定chart包
        HelmChartFile helmChartFile =
                helmChartService.getHelmChartFromMysql(chartName, chartVersion);
        Yaml yaml = new Yaml();
        String crType = chartName;
        // 寻找crd中的crType
        for (String key : helmChartFile.getYamlFileMap().keySet()) {
            if (key.contains("crd")) {
                JSONObject values = yaml.loadAs(helmChartFile.getYamlFileMap().get(key), JSONObject.class);
                if (values.containsKey(METADATA) && values.getJSONObject(METADATA).containsKey(LABELS)) {
                    JSONObject labels = values.getJSONObject(METADATA).getJSONObject(LABELS);
                    if (labels.containsKey(MIDDLEWARE_CLUSTER)) {
                        crType = labels.getString(MIDDLEWARE_CLUSTER);
                        break;
                    }
                }
            }
        }
        return crType;
    }

    public void tryInit(){

    }
}
