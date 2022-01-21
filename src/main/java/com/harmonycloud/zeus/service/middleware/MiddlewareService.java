package com.harmonycloud.zeus.service.middleware;

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
    void create(Middleware middleware);

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

    PageObject<MysqlSlowSqlDTO> slowsql(SlowLogQuery slowLogQuery) throws Exception;

    void slowsqlExcel(SlowLogQuery slowLogQuery, HttpServletResponse response, HttpServletRequest request) throws Exception;

    /**
     * 查询中间件管理控制台地址(仅kafka、es、rocket-mq支持)
     *
     * @param middleware
     * @return
     */
    void setManagePlatformAddress(Middleware middleware, String clusterId);

    /**
     * 获取集群下中间件简要信息
     * @param clusterDTOList
     * @return
     */
    List<MiddlewareBriefInfoDTO> getMiddlewareBriefInfoList(List<MiddlewareClusterDTO> clusterDTOList);

    /**
     * 查询所有中间件并作为服务列表的子菜单
     * @param clusterId
     * @return
     */
    List<ResourceMenuDto> listAllMiddlewareAsMenu(String clusterId);

    /**
     * 查询指定类型的所有服务
     * @param clusterId 集群
     * @param namespace 分区
     * @param type 服务类型，例如：mysql
     * @param keyword 搜索关键词
     * @return
     */
    List<MiddlewareBriefInfoDTO> listAllMiddleware(String clusterId, String namespace,String type, String keyword);

    /**
     * 查询服务版本
     * @param clusterId
     * @param namespace
     * @param name
     * @param type
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
    void upgradeChart(String clusterId, String namespace, String name, String type, String chartName, String upgradeChartVersion);

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
}
