package com.harmonycloud.zeus.service.middleware;

import com.alibaba.fastjson.JSONObject;
import com.harmonycloud.caas.common.base.BaseResult;
import com.harmonycloud.caas.common.model.MiddlewareServiceNameIndex;
import com.harmonycloud.caas.common.model.middleware.*;
import com.harmonycloud.caas.common.model.user.ResourceMenuDto;
import com.harmonycloud.tool.page.PageObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

/**
 * @author dengyulong
 * @date 2021/03/23
 */
public interface MiddlewareService {

    /**
     * 查询指定类型的所有服务
     * @param clusterId 集群
     * @param namespace 分区
     * @param type 服务类型，例如：mysql
     * @param keyword 搜索关键词
     * @param projectId 项目id
     * @return
     */
    List<MiddlewareBriefInfoDTO> list(String clusterId, String namespace,String type, String keyword, String projectId) throws Exception;

    /**
     * 查询中间件列表
     *
     * @param clusterId 集群id
     * @param namespace 命名空间
     * @param keyword
     * @return
     */
    List<Middleware> simpleList(String clusterId, String namespace, String type, String keyword);

    /**
     * 查询中间件详情
     *
     * @param clusterId 集群id
     * @param namespace 命名空间
     * @param name      中间件名称
     * @param type      中间件类型
     * @return
     */
    Middleware detail(String clusterId, String namespace, String name, String type);

    /**
     * 创建中间件
     *
     * @param middleware 中间件信息
     */
    Middleware create(Middleware middleware);

    /**
     * 恢复中间件
     *
     * @param middleware 中间件信息
     */
    void recovery(Middleware middleware);

    /**
     * 修改中间件
     *
     * @param middleware 中间件信息
     */
    void update(Middleware middleware);

    /**
     * 删除中间件
     *  @param clusterId 集群id
     * @param namespace 命名空间
     * @param name      中间件名称
     * @param type      中间件类型
     */
    void delete(String clusterId, String namespace, String name, String type);

    /**
     * 删除中间件相关存储
     *  @param clusterId 集群id
     * @param namespace 命名空间
     * @param name      中间件名称
     * @param type      中间件类型
     */
    void deleteStorage(String clusterId, String namespace, String name, String type);

    /**
     * 中间件手动切换
     * @param clusterId 集群id
     * @param namespace 命名空间
     * @param name      中间件名称
     * @param type      中间件类型
     * @param isAuto    是否自动切换
     */
    void switchMiddleware(String clusterId, String namespace, String name, String type, Boolean isAuto);

    /**
     * 性能监控
     *  @param clusterId 集群id
     * @param namespace 命名空间
     * @param name      中间件名称
     * @param type
     * @param chartVersion chart包版本
     * @return string
     */
    MonitorDto monitor(String clusterId, String namespace, String name, String type, String chartVersion);

    /**
     * 查询服务版本
     * @param clusterId 集群ID
     * @param namespace 分区
     * @param name      名称
     * @param type      类型
     * @return
     */
    List<MiddlewareInfoDTO> version(String clusterId, String namespace, String name, String type);

    /**
     * 服务版本升级
     * @param clusterId 集群id
     * @param namespace 分区
     * @param name 中间件名称
     * @param type 中间件类型
     * @param upgradeChartVersion 升级目标chartVersion
     */
    BaseResult upgradeChart(String clusterId, String namespace, String name, String type, String chartName, String upgradeChartVersion);

    /**
     * 服务版本升级校验
     * @param clusterId
     * @param namespace
     * @param name
     * @param type
     * @param chartName
     * @param upgradeChartVersion
     * @return
     */
    BaseResult upgradeCheck(String clusterId, String namespace, String name, String type, String chartName, String upgradeChartVersion);

    /**
     * 重启服务
     * @param clusterId 集群id
     * @param namespace 命名空间
     * @param name      中间件名称
     * @param type      中间件类型
     */
    void reboot(String clusterId, String namespace, String name, String type);

    /**
     * pvc扩缩容
     * @param middleware 中间件对象
     */
    void updateStorage(Middleware middleware);

    /**
     * 查询中间件详情
     *
     * @param clusterId 集群id
     * @param namespace 命名空间
     * @param name      中间件名称
     * @param type      中间件类型
     * @return
     */
    MiddlewareTopologyDTO topology(String clusterId, String namespace, String name, String type) throws Exception;

    /**
     * 查询中间件管理控制台
     *
     * @param clusterId 集群id
     * @param namespace 命名空间
     * @param name      中间件名称
     * @param type      中间件类型
     * @return String
     */
    String platform(String clusterId, String namespace, String name, String type);

    /**
     * 查询中间件详情
     *
     * @param clusterId 集群id
     * @param namespace 命名空间
     * @param name      中间件名称
     * @param type      中间件类型
     * @return String
     */
    Integer middlewareCount(String clusterId, String namespace, String name, String type);

    /**
     * 查询中间件图片
     * @param type
     * @param version
     * @return
     */
    String middlewareImage(String type, String version);

    /**
     * 计算中间件cpu申请使用量
     * @param values
     * @return
     */
    Double calculateCpuRequest(Middleware middleware, JSONObject values);

}
