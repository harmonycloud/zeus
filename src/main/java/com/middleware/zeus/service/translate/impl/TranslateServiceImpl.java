package com.middleware.zeus.service.translate.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.middleware.zeus.bean.user.BeanSysResourceTranslateConfig;
import com.middleware.zeus.dao.BeanSysResourceTranslateConfigMapper;
import com.middleware.zeus.service.translate.TranslateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;

/**
 * @author xutianhong
 * @Date 2024/10/25 10:47 AM
 */
@Service
public class TranslateServiceImpl implements TranslateService {

    @Autowired
    private BeanSysResourceTranslateConfigMapper beanSysResourceTranslateConfigMapper;

    @Override
    public List<BeanSysResourceTranslateConfig> list(String groupName, String uniqueValue, List<String> propertyList, List<String> languageList) {

        QueryWrapper<BeanSysResourceTranslateConfig> queryWrapper = new QueryWrapper<>();
        if (groupName != null) {
            queryWrapper.eq("group_name", groupName);
        }
        if (uniqueValue != null) {
            queryWrapper.eq("unique_value", uniqueValue);
        }
        if (!CollectionUtils.isEmpty(propertyList)) {
            queryWrapper.in("property", propertyList);
        }
        if (!CollectionUtils.isEmpty(languageList)) {
            queryWrapper.in("language_code", languageList);
        }

        return beanSysResourceTranslateConfigMapper.selectList(queryWrapper);
    }

}
