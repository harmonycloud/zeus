package com.middleware.zeus.service.translate;

import com.middleware.zeus.bean.user.BeanSysResourceTranslateConfig;

import java.util.List;

/**
 * @author xutianhong
 * @Date 2024/10/25 10:47 AM
 */
public interface TranslateService {

    /**
     * 获取资源翻译配置
     * @param groupName 分组名称
     * @param uniqueValue 唯一值
     * @param propertyList 属性
     * @param languageList 语言列表
     *
     * @return List<BeanSysResourceTranslateConfig>
     */
    List<BeanSysResourceTranslateConfig> list(String groupName, String uniqueValue, List<String> propertyList, List<String> languageList);

}
