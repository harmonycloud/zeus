package com.middleware.zeus.common.model.registry;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.Map;

/**
 * @author dengyulong
 * @date 2020/12/17
 */
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
@Data
public class HelmChartFile {

    private String chartName;

    private String chartVersion;

    private String description;

    private String valueYaml;

    private Map<String,String> yamlFileMap;

    private String fileIndex;

    private String tarFileName;

    private String iconPath;

    private String type;

    private String appVersion;

    private Map<String, String> dependency;

    /**
     * 是否为官方中间件
     */
    private String official;

    /**
     * 升级至当前chart所需的最低版本
     */
    private String compatibleVersions;

    /**
     * 应用自身版本，以逗号分隔
     */
    private String version;
}
