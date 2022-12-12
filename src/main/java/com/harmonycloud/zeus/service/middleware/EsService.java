package com.harmonycloud.zeus.service.middleware;

import com.harmonycloud.caas.common.model.middleware.MiddlewareClusterDTO;
import com.harmonycloud.caas.common.model.middleware.MysqlLogDTO;
import com.harmonycloud.caas.common.model.middleware.MysqlLogQuery;
import com.harmonycloud.tool.page.PageObject;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;

import java.io.IOException;
import java.util.List;

/**
 * @author dengyulong
 * @date 2021/03/23
 */
public interface EsService {
    /**
     * 根据集群id查询集群的es组件连接Client
     *
     * @param cluster 集群信息
     * @return
     */
    RestHighLevelClient getEsClient(String clusterId) throws Exception;

    /**
     * 重置es客户端（集群信息修改，需要重置）
     *
     * @param cluster 集群信息
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
     * @param cluster
     * @return
     * @throws Exception
     */
    boolean isExistIndex(String indexName, String clusterId) throws Exception;

    /**
     * 删除索引
     *
     * @param indexName
     * @param cluster
     * @return
     * @throws Exception
     */
    boolean deleteIndex(String indexName, String clusterId) throws Exception;

    PageObject<MysqlLogDTO> getSlowSql(String clusterId, MysqlLogQuery slowLogQuery) throws Exception;

    /**
     * 校验es
     *
     * @param cluster 集群信息
     * @return
     */
    Boolean checkEsConnection(String clusterId);

    String resultByGetRestClient(RestHighLevelClient client, String clusterId, String endPoint) throws Exception;

    String getLogIndexPrefix(boolean isPodLog);

    void updateIndexMaxResultWindow(RestHighLevelClient client, String[] index, int maxResultWindow)throws IOException;

    List<String> getIndexes(String clusterId) throws Exception;

    void initEsIndexTemplate();

    /**
     * 初始化es索引模板
     * @param clusterId 集群id（可选）
     * @return
     * @throws Exception
     */
    boolean initEsIndexTemplate(String clusterId);
}
