package com.harmonycloud.zeus.service.middleware;

import com.harmonycloud.caas.common.base.BaseResult;
import com.harmonycloud.caas.common.model.middleware.MysqlSlowSqlDTO;
import com.harmonycloud.caas.common.model.middleware.SlowLogQuery;
import com.harmonycloud.tool.page.PageObject;

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
     * @param slowLogQuery 慢日志查询对象
     *
     * @return PageObject<MysqlSlowSqlDTO>
     */
    PageObject<MysqlSlowSqlDTO> slowsql(SlowLogQuery slowLogQuery) throws Exception;

    /**
     * 慢日志导出
     * @param slowLogQuery 慢日志查询对象
     * @param response     返回体
     * @param request      请求体
     *
     */
    void slowsqlExcel(SlowLogQuery slowLogQuery, HttpServletResponse response, HttpServletRequest request) throws Exception;
}
