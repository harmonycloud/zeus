package com.harmonycloud.zeus.service.middleware;

import com.harmonycloud.caas.common.model.MiddlewareServiceNameIndex;
import com.harmonycloud.caas.common.model.middleware.*;
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
     * @param type
     */
    void delete(String clusterId, String namespace, String name, String type);

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

    List<Middleware> simpleListAll(String type);

    PageObject<MysqlSlowSqlDTO> slowsql(SlowLogQuery slowLogQuery) throws Exception;

    void slowsqlExcel(SlowLogQuery slowLogQuery, HttpServletResponse response, HttpServletRequest request) throws Exception;

    /**
     * 查询中间件管理控制台地址(仅kafka、es、rocket-mq支持)
     *
     * @param middleware
     * @return
     */
    String getManagePlatformAddress(Middleware middleware, String clusterId);

    /**
     * 查询可用中间件列表及服务列表
     * @param clusterId
     * @param namespace
     * @param keyword
     * @return
     */
    List listAllMiddleware(String clusterId, String namespace,String keyword);
}
