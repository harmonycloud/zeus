package com.middleware.zeus.service.system.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.middleware.caas.common.model.FeatureDto;
import com.middleware.zeus.config.FeatureConfig;
import com.middleware.zeus.service.system.FeatureService;

import lombok.extern.slf4j.Slf4j;

/**
 * @author xutianhong
 * @Date 2023/5/4 7:21 下午
 */
@Service
@Slf4j
public class FeatureServiceImpl implements FeatureService {

    @Autowired
    private FeatureConfig featureConfig;

    @Override
    public List<FeatureDto> list() {
        List<FeatureDto> featureDtoList = new ArrayList<>();

        // 获取配置文件中feature字段
        Map<String, Boolean> features = featureConfig.getFeature();

        if(CollectionUtils.isEmpty(features)){
            return featureDtoList;
        }
        // 封装数据
        for (String key : features.keySet()){
            FeatureDto featureDto = new FeatureDto();
            featureDto.setName(key);
            featureDto.setEnabled(features.get(key));
            featureDtoList.add(featureDto);
        }
        return featureDtoList;
    }
}
