package com.harmonycloud.zeus.service.middleware.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.harmonycloud.tool.uuid.UUIDUtils;
import com.harmonycloud.zeus.bean.BeanCustomConfig;
import com.harmonycloud.zeus.bean.BeanCustomConfigTemplate;
import com.harmonycloud.zeus.dao.BeanCustomConfigMapper;
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
    @Autowired
    private BeanCustomConfigMapper beanCustomConfigMapper;

    @Override
    public void create(CustomConfigTemplateDTO customConfigTemplateDTO) {
        // 封装数据
        BeanCustomConfigTemplate beanCustomConfigTemplate = convert(customConfigTemplateDTO);
        // 写入数据库
        beanCustomConfigTemplateMapper.insert(beanCustomConfigTemplate);
    }

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
    public List<CustomConfig> get(String type, String chartVersion) {
        QueryWrapper<BeanCustomConfig> wrapper =
                new QueryWrapper<BeanCustomConfig>().eq("chart_name", type).eq("chart_version", chartVersion);
        List<BeanCustomConfig> beanCustomConfigList = beanCustomConfigMapper.selectList(wrapper);
        return beanCustomConfigList.stream().map(beanCustomConfig -> {
            CustomConfig customConfig = new CustomConfig();
            BeanUtils.copyProperties(beanCustomConfig, customConfig);
            return customConfig;
        }).collect(Collectors.toList());
    }

    @Override
    public CustomConfigTemplateDTO get(String type, String uid, String chartVersion) {
        // 获取模板
        QueryWrapper<BeanCustomConfigTemplate> wrapper =
            new QueryWrapper<BeanCustomConfigTemplate>().eq("uid", uid).eq("type", type);
        BeanCustomConfigTemplate template = beanCustomConfigTemplateMapper.selectOne(wrapper);
        // 获取所有参数
        List<CustomConfig> customConfigList = this.get(type, chartVersion);
        Map<String, CustomConfig> customConfigMap =
            customConfigList.stream().collect(Collectors.toMap(CustomConfig::getName, customConfig -> customConfig));
        // 封装数据
        CustomConfigTemplateDTO customConfigTemplateDTO = new CustomConfigTemplateDTO();
        BeanUtils.copyProperties(template, customConfigTemplateDTO, "id", "config");
        String[] config = template.getConfig().split("@@@");
        for (String s : config) {
            String[] temp = s.split("###");
            if (customConfigMap.containsKey(temp[0])) {
                customConfigMap.get(temp[0]).setValue(temp[1]);
                customConfigMap.get(temp[0]).setDescription(temp[2]);
            }
        }
        customConfigTemplateDTO.setCustomConfigList(new ArrayList<>(customConfigMap.values()));
        return customConfigTemplateDTO;
    }

    @Override
    public void update(CustomConfigTemplateDTO customConfigTemplateDTO) {
        // 封装数据
        BeanCustomConfigTemplate beanCustomConfigTemplate = convert(customConfigTemplateDTO);
        // update
        QueryWrapper<BeanCustomConfigTemplate> wrapper =
            new QueryWrapper<BeanCustomConfigTemplate>().eq("uid", customConfigTemplateDTO.getUid());
        beanCustomConfigTemplateMapper.update(beanCustomConfigTemplate, wrapper);
    }

    @Override
    public void delete(String type, String uids) {
        String[] uid = uids.split(",");
        for (int i = 0; i < uid.length; ++i){
            QueryWrapper<BeanCustomConfigTemplate> wrapper = new QueryWrapper<BeanCustomConfigTemplate>().eq("type", type).eq("uid", uid[i]);
            beanCustomConfigTemplateMapper.delete(wrapper);
        }
    }
    
    public BeanCustomConfigTemplate convert(CustomConfigTemplateDTO customConfigTemplateDTO){
        StringBuilder sb = new StringBuilder();
        // 连接自定义配置
        customConfigTemplateDTO.getCustomConfigList().forEach(customConfig -> {
            sb.append(customConfig.getName()).append("###").append(customConfig.getValue()).append("###").append(customConfig.getDescription());
            sb.append("@@@");
        });
        // 去除末尾的连接符
        sb.delete(sb.length() - 3, sb.length());
        // 构建数据库对象
        BeanCustomConfigTemplate beanCustomConfigTemplate = new BeanCustomConfigTemplate();
        BeanUtils.copyProperties(customConfigTemplateDTO, beanCustomConfigTemplate);
        beanCustomConfigTemplate.setUid(UUIDUtils.get16UUID());
        beanCustomConfigTemplate.setConfig(sb.toString());
        return beanCustomConfigTemplate;
    }
    
}
