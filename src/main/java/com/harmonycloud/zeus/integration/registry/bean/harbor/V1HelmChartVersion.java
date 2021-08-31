package com.harmonycloud.zeus.integration.registry.bean.harbor;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * @author dengyulong
 * @date 2021/05/18
 */
@Accessors(chain = true)
@Data
public class V1HelmChartVersion {

    /**
     * chart名称
     */
    private String name;
    /**
     * chart版本
     */
    private String version;
    /**
     * api版本
     */
    private String apiVersion;
    /**
     * 应用版本
     */
    private String appVersion;
    /**
     * 创建时间
     */
    private String created;
    /**
     * chart描述
     */
    private String description;
    /**
     * 数字签名
     */
    private String digest;
    /**
     * 标签
     */
    private List<String> labels;
    /**
     * chart地址列表
     */
    private List<String> urls;

}
