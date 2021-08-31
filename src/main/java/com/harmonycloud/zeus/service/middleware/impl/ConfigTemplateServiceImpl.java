package com.harmonycloud.zeus.service.middleware.impl;

import java.util.ArrayList;
import java.util.List;

import com.harmonycloud.zeus.bean.BeanCustomConfigTemplate;
import com.harmonycloud.zeus.dao.BeanCustomConfigTemplateMapper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.harmonycloud.caas.common.model.middleware.CustomConfig;
import com.harmonycloud.caas.common.model.middleware.CustomConfigTemplateDTO;
import com.harmonycloud.zeus.service.middleware.ConfigTemplateService;

import lombok.extern.slf4j.Slf4j;

/**
 * @author xutianhong
 * @Date 2021/4/25 10:36 上午
 */
@Service
@Slf4j
public class ConfigTemplateServiceImpl implements ConfigTemplateService {

    @Autowired
    private BeanCustomConfigTemplateMapper beanCustomConfigTemplateMapper;

    @Override
    public List<CustomConfigTemplateDTO> list(String type) {
        QueryWrapper<BeanCustomConfigTemplate> wrapper = new QueryWrapper<BeanCustomConfigTemplate>().eq("type", type);
        List<BeanCustomConfigTemplate> list = beanCustomConfigTemplateMapper.selectList(wrapper);

        List<CustomConfigTemplateDTO> customConfigTemplateDTOList = new ArrayList<>();
        list.forEach(template -> {
            CustomConfigTemplateDTO customConfigTemplateDTO = new CustomConfigTemplateDTO();
            BeanUtils.copyProperties(template, customConfigTemplateDTO, "config");
            customConfigTemplateDTOList.add(customConfigTemplateDTO);
        });
        return customConfigTemplateDTOList;
    }

    @Override
    public CustomConfigTemplateDTO get(String type, String name) {
        QueryWrapper<BeanCustomConfigTemplate> wrapper = new QueryWrapper<>();
        if (!StringUtils.isEmpty(name)) {
            wrapper.eq("name", name).eq("type", type);
        }
        BeanCustomConfigTemplate template = beanCustomConfigTemplateMapper.selectOne(wrapper);
        // 封装数据
        CustomConfigTemplateDTO customConfigTemplateDTO = new CustomConfigTemplateDTO();
        BeanUtils.copyProperties(template, customConfigTemplateDTO, "id", "config");
        String[] config = template.getConfig().split(",");
        List<CustomConfig> customConfigList = new ArrayList<>();
        for (String s : config) {
            CustomConfig customConfig = new CustomConfig();
            customConfig.setName(s.split("=")[0]);
            customConfig.setValue(s.split("=")[1]);
            customConfigList.add(customConfig);
        }
        customConfigTemplateDTO.setCustomConfigList(customConfigList);
        return customConfigTemplateDTO;
    }
}
