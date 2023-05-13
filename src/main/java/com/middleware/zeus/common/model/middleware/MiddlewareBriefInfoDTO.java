package com.middleware.zeus.common.model.middleware;

import lombok.Data;

import java.util.List;

/**
 * 服务列表中间件简要信息
 * @author liyinlong
 * @since 2021/9/23 3:04 下午
 */
@Data
public class MiddlewareBriefInfoDTO {

    /**
     * 中间件名称
     */
    private String name;

    /**
     * 中间件别名
     */
    private String aliasName;

    /**
     * 中间件图片路径
     */
    private String imagePath;

    /**
     * 中间件chart名称
     */
    private String chartName;

    /**
     * 中间件chart版本
     */
    private String chartVersion;

    /**
     * 中间件版本
     */
    private String version;

    /**
     * 中间件实例列表
     */
    private List<Middleware> serviceList;

    /**
     * 中间件实例数量
     */
    private int serviceNum;

    /**
     * 异常中间件实例数量
     */
    private int errServiceNum;

    /**
     * 是否是官方中间件
     */
    private Boolean official;

    /**
     * 服务列表菜单url
     */
    private String url;

}
