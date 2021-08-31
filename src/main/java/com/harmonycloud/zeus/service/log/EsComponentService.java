package com.harmonycloud.zeus.service.log;

import com.harmonycloud.caas.common.model.middleware.MiddlewareClusterDTO;
import com.harmonycloud.caas.common.model.middleware.MysqlSlowSqlDTO;
import com.harmonycloud.caas.common.model.middleware.SlowLogQuery;
import com.harmonycloud.zeus.bean.BeanOperationAudit;
import com.harmonycloud.tool.page.PageObject;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;

public interface EsComponentService {

    /**
     * 根据集群id查询集群的es组件连接Client
     *
     * @param cluster 集群信息
     * @return
     */
    RestHighLevelClient getEsClient(MiddlewareClusterDTO cluster) throws Exception;

    /**
     * 重置es客户端（集群信息修改，需要重置）
     *
     * @param cluster 集群信息
     * @return
     */
    RestHighLevelClient resetEsClient(MiddlewareClusterDTO cluster);

    /**
     * 获取低水平客户端
     *
     * @return
     */
    RestClient getLowLevelClient(MiddlewareClusterDTO cluster);

    /**
     * 判断某个集群的es组件是否已经存在索引
     *
     * @param indexName
     * @param cluster
     * @return
     * @throws Exception
     */
    boolean isExistIndex(String indexName, MiddlewareClusterDTO cluster) throws Exception;

    /**
     * 删除索引
     *
     * @param indexName
     * @param cluster
     * @return
     * @throws Exception
     */
    boolean deleteIndex(String indexName, MiddlewareClusterDTO cluster) throws Exception;

    PageObject<MysqlSlowSqlDTO> getSlowSql(MiddlewareClusterDTO cluster, SlowLogQuery slowLogQuery) throws Exception;

    /**
     * 校验es
     *
     * @param cluster 集群信息
     * @return
     */
    Boolean checkEsConnection(MiddlewareClusterDTO cluster);

    String resultByGetRestClient(RestHighLevelClient client, MiddlewareClusterDTO cluster, String endPoint) throws Exception;

    IndexResponse saveAuditRepository(BeanOperationAudit beanRequest, String clusterId);
}
