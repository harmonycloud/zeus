package com.middleware.zeus.service.log;

import com.middleware.caas.common.model.middleware.MiddlewareClusterDTO;
import com.middleware.caas.common.model.middleware.MysqlLogDTO;
import com.middleware.caas.common.model.middleware.MiddlewareLogQuery;
import com.middleware.tool.page.PageObject;
import com.middleware.zeus.bean.BeanOperationAudit;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;

public interface EsComponentService {

    /**
     * 根据集群id查询集群的es组件连接Client
     *
     * @param clusterId 集群信息
     * @return
     */
    RestHighLevelClient getEsClient(String clusterId) throws Exception;

    /**
     * 重置es客户端（集群信息修改，需要重置）
     *
     * @param clusterId 集群信息
     * @return
     */
    RestHighLevelClient resetEsClient(String clusterId);

    /**
     * 获取低水平客户端
     *
     * @return
     */
    RestClient getLowLevelClient(String clusterId);

    /**
     * 判断某个集群的es组件是否已经存在索引
     *
     * @param indexName
     * @param clusterId
     * @return
     * @throws Exception
     */
    boolean isExistIndex(String indexName, String clusterId) throws Exception;

    /**
     * 删除索引
     *
     * @param indexName
     * @param clusterId
     * @return
     * @throws Exception
     */
    boolean deleteIndex(String indexName, String clusterId) throws Exception;

    PageObject<MysqlLogDTO> getSlowSql(String clusterId, MiddlewareLogQuery slowLogQuery) throws Exception;

    PageObject<MysqlLogDTO> getAuditSql(String clusterId, MiddlewareLogQuery auditLogQuery) throws Exception;

    /**
     * 校验es
     *
     * @param clusterId 集群信息
     * @return
     */
    Boolean checkEsConnection(String clusterId);

    String resultByGetRestClient(RestHighLevelClient client, String clusterId, String endPoint) throws Exception;

    IndexResponse saveAuditRepository(BeanOperationAudit beanRequest, String clusterId);
}
