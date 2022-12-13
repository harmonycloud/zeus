package com.middleware.zeus.service.middleware;

import com.middleware.caas.common.base.BaseResult;
import com.middleware.caas.common.model.middleware.MysqlLogDTO;
import com.middleware.caas.common.model.middleware.MysqlLogQuery;
import com.middleware.tool.page.PageObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author dengyulong
 * @date 2021/03/23
 */
public interface MysqlService {

    /**
     * 灾备切换
     *
     * @param clusterId      集群id
     * @param namespace      命名空间
     * @param middlewareName 中间件名称
     */
    BaseResult switchDisasterRecovery(String clusterId, String namespace, String middlewareName);

    /**
     * 查询mysql访问信息
     *
     * @param clusterId      集群id
     * @param namespace      命名空间
     * @param middlewareName 中间件名称
     */
    BaseResult queryAccessInfo(String clusterId, String namespace, String middlewareName);

    /**
     * mysql慢日志查询
     * @param mysqlLogQuery 慢日志查询对象
     *
     * @return PageObject<MysqlSlowSqlDTO>
     */
    PageObject<MysqlLogDTO> slowsql(MysqlLogQuery mysqlLogQuery) throws Exception;

    /**
     * 慢日志导出
     * @param mysqlLogQuery 慢日志查询对象
     * @param response     返回体
     * @param request      请求体
     *
     */
    void slowsqlExcel(MysqlLogQuery mysqlLogQuery, HttpServletResponse response, HttpServletRequest request) throws Exception;

    /**
     * 查询审计日志
     * @param auditLogQuery
     * @return
     * @throws Exception
     */
    PageObject<MysqlLogDTO> auditSql(MysqlLogQuery auditLogQuery);

}
