package com.harmonycloud.zeus.service.log.impl;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.harmonycloud.caas.common.constants.CommonConstant;
import com.harmonycloud.caas.common.constants.CoreConstant;
import com.harmonycloud.caas.common.constants.DateStyle;
import com.harmonycloud.caas.common.enums.DictEnum;
import com.harmonycloud.caas.common.enums.ErrorMessage;
import com.harmonycloud.caas.common.enums.EsSearchTypeEnum;
import com.harmonycloud.caas.common.exception.BusinessException;
import com.harmonycloud.caas.common.model.middleware.*;
import com.harmonycloud.tool.api.client.ElasticSearchClient;
import com.harmonycloud.tool.date.DateUtils;
import com.harmonycloud.tool.json.JsonUtil;
import com.harmonycloud.tool.page.PageObject;
import com.harmonycloud.zeus.bean.BeanOperationAudit;
import com.harmonycloud.zeus.service.k8s.ClusterService;
import com.harmonycloud.zeus.service.log.EsComponentService;
import com.harmonycloud.zeus.service.middleware.EsService;
import com.harmonycloud.zeus.util.EsIndexUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.util.EntityUtils;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthRequest;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.admin.indices.get.GetIndexRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.support.IndicesOptions;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.client.*;
import org.elasticsearch.cluster.health.ClusterHealthStatus;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.reindex.BulkByScrollResponse;
import org.elasticsearch.index.reindex.ReindexRequest;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.ConnectException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Service
public class EsComponentServiceImpl implements EsComponentService {

    private Map<String, RestHighLevelClient> esClients = new ConcurrentHashMap<>();

    @Autowired
    private ClusterService clusterService;

    @Autowired
    private EsService esService;

    @Override
    public RestHighLevelClient getEsClient(MiddlewareClusterDTO cluster) throws Exception {
        RestHighLevelClient client = esClients.get(cluster.getId());
        if (client == null) {
            client = this.createEsClient(cluster);
            esClients.put(cluster.getId(), client);
        }
        return client;
    }

    @Override
    public RestHighLevelClient resetEsClient(MiddlewareClusterDTO cluster) {
        RestHighLevelClient client = this.createEsClient(cluster);
        esClients.put(cluster.getId(), client);
        return client;
    }

    @Override
    public RestClient getLowLevelClient(MiddlewareClusterDTO cluster) {
        RestHighLevelClient client = esClients.get(cluster.getId());
        if (client == null) {
            client = this.createEsClient(cluster);
            esClients.put(cluster.getId(), client);
        }
        return client.getLowLevelClient();
    }

    @Override
    public boolean isExistIndex(String index, MiddlewareClusterDTO cluster) throws Exception {
        GetIndexRequest indexRequest = new GetIndexRequest().indices(index);
        boolean exists = this.getEsClient(cluster).indices().exists(indexRequest, RequestOptions.DEFAULT);
        return exists;
    }

    @Override
    public boolean deleteIndex(String indexName, MiddlewareClusterDTO cluster) throws Exception {
        if (!isExistIndex(indexName, cluster)) {
            throw new BusinessException(ErrorMessage.NOT_FOUND, DictEnum.LOG_INDEX.phrase());
        }
        DeleteIndexRequest deleteIndexRequest = new DeleteIndexRequest(indexName);
        deleteIndexRequest.indicesOptions(IndicesOptions.LENIENT_EXPAND_OPEN);
        AcknowledgedResponse response = this.getEsClient(cluster).indices().delete(deleteIndexRequest, RequestOptions.DEFAULT);
        return response.isAcknowledged();

    }

    private RestHighLevelClient createEsClient(MiddlewareClusterDTO cluster) {
        MiddlewareClusterLoggingInfo elasticSearch = Optional.ofNullable(cluster).map(MiddlewareClusterDTO::getLogging).map(MiddlewareClusterLogging::getElasticSearch).orElse(null);
        if (elasticSearch == null || StringUtils.isEmpty(elasticSearch.getHost())) {
            throw new BusinessException(ErrorMessage.CLUSTER_ES_SERVICE_ERROR);
        }
        String userName = elasticSearch.getUser();
        String password = elasticSearch.getPassword();
        String esHost = elasticSearch.getHost();
        Integer port = Integer.valueOf(elasticSearch.getPort());
        if (StringUtils.isEmpty(userName)) {
            // userName不能等于null
            userName = "";
        }
        try {
            RestHighLevelClient highLevelClient =
                    ElasticSearchClient.getHighLevelClient(esHost, userName, password, port);
            return highLevelClient;
        } catch (Exception e) {
            log.error("创建ElasticSearch Client 失败,cluster:{}", JSONObject.toJSONString(cluster), e);
            throw new BusinessException(ErrorMessage.CLUSTER_ES_SERVICE_ERROR);
        }
    }

    @Override
    public PageObject<MysqlLogDTO> getSlowSql(MiddlewareClusterDTO cluster, MysqlLogQuery slowLogQuery) throws Exception {
        if (cluster == null) {
            return new PageObject<>(new ArrayList<>(), CommonConstant.NUM_ZERO);
        }
        String clusterId = cluster.getId();
        RestHighLevelClient esClient = esClients.get(clusterId);
        if (esClient == null) {
            esClient = this.getEsClient(clusterService.findById(clusterId));
        }
        BoolQueryBuilder query = this.getSearchRequestBuilder(slowLogQuery);
        //根据时间范围判断落在哪几个索引
        List<String> indexNameList = getExistIndexNames(esClient, cluster, slowLogQuery.getStartTime(), slowLogQuery.getEndTime());
        if (CollectionUtils.isEmpty(indexNameList)) {
            return new PageObject<>(new ArrayList<>(), CommonConstant.NUM_ZERO);
        }
        PageObject<MysqlLogDTO> mysqlSlowSqlDTOPageObject = searchFromIndex(esClient, query, slowLogQuery.getCurrent(), slowLogQuery.getSize(), indexNameList);
        return mysqlSlowSqlDTOPageObject;
    }

    @Override
    public PageObject<MysqlLogDTO> getAuditSql(MiddlewareClusterDTO cluster, MysqlLogQuery auditLogQuery) throws Exception {
        if (cluster == null) {
            return new PageObject<>(new ArrayList<>(), CommonConstant.NUM_ZERO);
        }
        String clusterId = cluster.getId();
        RestHighLevelClient esClient = esClients.get(clusterId);
        if (esClient == null) {
            esClient = this.getEsClient(clusterService.findById(clusterId));
        }
        if (auditLogQuery.getCurrent() == null) {
            auditLogQuery.setCurrent(1);
        }
        if (auditLogQuery.getSize() == null) {
            auditLogQuery.setSize(10);
        }
        BoolQueryBuilder query = this.getAuditSearchRequestBuilder(auditLogQuery);
        // 获取SQL审计所有索引
        List<String> indexNameList = getExistAuditIndexNames(esClient, cluster);
        if (CollectionUtils.isEmpty(indexNameList)) {
            return new PageObject<>(new ArrayList<>(), CommonConstant.NUM_ZERO);
        }
        PageObject<MysqlLogDTO> mysqlSlowSqlDTOPageObject = searchFromIndex(esClient, query, auditLogQuery.getCurrent(), auditLogQuery.getSize(), indexNameList);
        mysqlSlowSqlDTOPageObject.getData().forEach(item -> {
            item.setQueryDate(DateUtils.parseUTCSDate(item.getTimestampMysql()));
        });
        return mysqlSlowSqlDTOPageObject;
    }

    /**
     * 校验es组件
     *
     * @param cluster
     * @return
     */
    @Override
    public Boolean checkEsConnection(MiddlewareClusterDTO cluster) {
        try {
            RestHighLevelClient client = this.createEsClient(cluster);
            ClusterHealthRequest healthRequest = new ClusterHealthRequest();
            ClusterHealthStatus status = client.cluster().health(healthRequest, RequestOptions.DEFAULT).getStatus();
            if (StringUtils.equals(status.name(), ClusterHealthStatus.RED.name())) {
                return false;
            }
        } catch (Exception e) {
            log.warn("es校验失败", e);
            return false;
        }
        return true;
    }

    private void transferIndex(String index, MiddlewareClusterDTO cluster) throws Exception {

        if (!isExistIndex(index, cluster)) {
            log.info("索引不存在，index:{}, clusterId:{}", index, cluster.getId());
        }
        log.info("索引模板创建后，对集群{}的索引{}迁移数据", cluster.getName(), index);
        String backupIndexName = "backup-" + index;
        BulkByScrollResponse res = this.reIndex(index, backupIndexName, cluster.getId());
        log.info(res.getStatus().toString());
        deleteIndex(index, cluster);
        this.reIndex(backupIndexName, index, cluster.getId());
    }

    private BulkByScrollResponse reIndex(String sourceIndex, String destIndex, String clusterId) throws IOException {
        ReindexRequest reindexRequest = new ReindexRequest();
        reindexRequest.setSourceIndices(sourceIndex);
        reindexRequest.setDestIndex(destIndex);
        return esClients.get(clusterId).reindex(reindexRequest, RequestOptions.DEFAULT);
    }

    private String getAuthorization(MiddlewareClusterDTO cluster) {
        MiddlewareClusterLoggingInfo elasticSearch = cluster.getLogging().getElasticSearch();
        if (StringUtils.isEmpty(elasticSearch.getUser()) || StringUtils.isEmpty(elasticSearch.getPassword())) {
            return "";
        }
        try {
            return Base64.getEncoder().encodeToString((elasticSearch.getUser() + ":" + elasticSearch.getPassword()).getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            log.error("获取es authorization 失败", e);
            return "";
        }
    }

    /**
     * 根据查询条件设置SearchRequestBuilder
     */
    private BoolQueryBuilder getSearchRequestBuilder(MysqlLogQuery slowLogQuery) {

        String startTime = slowLogQuery.getStartTime();
        String endTime = slowLogQuery.getEndTime();

        BoolQueryBuilder query = QueryBuilders.boolQuery();
        if (StringUtils.isNotBlank(startTime) && StringUtils.isNotBlank(endTime)) {
            if (DateUtils.parseUTCDate(startTime).after(DateUtils.parseUTCDate(endTime))) {
                throw new BusinessException(ErrorMessage.START_DATE_AFTER_END);
            }
            query.must(QueryBuilders.rangeQuery("@timestamp").from(startTime).to(endTime));
        }
        query.must(QueryBuilders.matchQuery("k8s_pod_namespace", slowLogQuery.getNamespace()));
        query.must(QueryBuilders.matchQuery("middleware_name", slowLogQuery.getMiddlewareName()));
        query.must(QueryBuilders.existsQuery("query_time"));
        //添加筛选query_time
        if (StringUtils.isNotEmpty(slowLogQuery.getFromQueryTime())
                && StringUtils.isNotEmpty(slowLogQuery.getToQueryTime())) {
            query.must(QueryBuilders.rangeQuery("query_time").from(slowLogQuery.getFromQueryTime())
                    .to(slowLogQuery.getToQueryTime()));
        }
        if (StringUtils.isNotBlank(slowLogQuery.getSearchWord())) {
            String keyWord = slowLogQuery.getSearchWord().trim().toLowerCase();
            //日志内容关键字查询
            if (EsSearchTypeEnum.MATCH.getCode().equalsIgnoreCase(slowLogQuery.getSearchType())) {
                query = query.must(QueryBuilders.matchQuery("query", keyWord));
            } else if (EsSearchTypeEnum.MATCH_PHRASE.getCode().equalsIgnoreCase(slowLogQuery.getSearchType())) {
                query = query.must(QueryBuilders.matchPhraseQuery("query", keyWord));
            } else if (EsSearchTypeEnum.WILDCARD.getCode().equalsIgnoreCase(slowLogQuery.getSearchType())) {
                keyWord = slowLogQuery.getSearchWord();
                //模糊查询如果参数没有*，添加*进行模糊匹配
                if (!keyWord.contains("*")) {
                    keyWord = "*" + keyWord + "*";
                }
                query = query.must(QueryBuilders.wildcardQuery("query", keyWord));
            } else if (EsSearchTypeEnum.REGEXP.getCode().equalsIgnoreCase(slowLogQuery.getSearchType())) {
                query = query.must(QueryBuilders.regexpQuery("query", keyWord));
            }
        }
        return query;
    }

    /**
     * 根据查询条件设置SearchRequestBuilder
     */
    private BoolQueryBuilder getAuditSearchRequestBuilder(MysqlLogQuery auditLogQuery) {
        BoolQueryBuilder query = QueryBuilders.boolQuery();
        query.must(QueryBuilders.matchQuery("k8s_pod_namespace", auditLogQuery.getNamespace()));
        query.must(QueryBuilders.matchQuery("middleware_name", auditLogQuery.getMiddlewareName()));
        if (StringUtils.isNotBlank(auditLogQuery.getSearchWord())) {
            String keyWord = auditLogQuery.getSearchWord().trim().toLowerCase();
            //日志内容关键字查询
            keyWord = auditLogQuery.getSearchWord();
            //模糊查询如果参数没有*，添加*进行模糊匹配
            if (!keyWord.contains("*")) {
                keyWord = "*" + keyWord + "*";
            }
            query = query.must(QueryBuilders.wildcardQuery("query", keyWord));
        }
        return query;
    }

    private List<String> getExistIndexNames(RestHighLevelClient esClient, MiddlewareClusterDTO cluster, String startTime, String endTime) throws Exception {
        List<String> indexNameList = new ArrayList<>();
        if (StringUtils.isNotBlank(startTime) && StringUtils.isNotBlank(endTime)) {
            Date startDate = DateUtils.parseUTCDate(startTime);
            Date endDate = DateUtils.parseUTCDate(endTime);

            Calendar dayc1 = new GregorianCalendar();
            Calendar dayc2 = new GregorianCalendar();
            dayc1.setTime(startDate); //设置calendar的日期
            dayc2.setTime(endDate);
            //dayc1在dayc2之前就循环
            while (dayc1.compareTo(dayc2) <= 0) {
                String s = (dayc1.get(Calendar.YEAR) + "年" +
                        (dayc1.get(Calendar.MONTH) + 1) + "月" + dayc1.get(Calendar.DATE)) + "日";
                indexNameList.add(
                        CoreConstant.ES_INDEX_MYSQL_SLOW_LOG + CommonConstant.LINE +
                                dayc1.get(Calendar.YEAR) + CommonConstant.DOT +
                                String.format("%02d", (dayc1.get(Calendar.MONTH) + 1)) + CommonConstant.DOT +
                                String.format("%02d", dayc1.get(Calendar.DATE))
                );

                dayc1.add(Calendar.DAY_OF_YEAR, CommonConstant.NUM_ONE); //加1天
            }
        }
        indexNameList = CollectionUtils.isNotEmpty(indexNameList) ? indexNameList : Arrays.asList(generateIndexName());
        // 取得已存在的索引
        String result = resultByGetRestClient(esClient, cluster, "/_cat/indices?format=json");
        List<String> indices = new ArrayList<>();
        if (StringUtils.isNotEmpty(result)) {
            List<Map<String, String>> indexMap = JsonUtil.jsonToPojo(result, ArrayList.class);
            if (CollectionUtils.isNotEmpty(indexMap)) {
                indices = indexMap.stream().map(indexs -> indexs.get("index")).collect(Collectors.toList());
            }
        }
        indexNameList.retainAll(indices);
        return indexNameList;
    }

    private List<String> getExistAuditIndexNames(RestHighLevelClient esClient, MiddlewareClusterDTO cluster) throws Exception {
        // 取得所有索引
        String result = resultByGetRestClient(esClient, cluster, "/_cat/indices/mysqlaudit-*?format=json");
        List<String> indices = new ArrayList<>();
        if (StringUtils.isNotEmpty(result)) {
            List<Map<String, String>> indexMap = JsonUtil.jsonToPojo(result, ArrayList.class);
            if (CollectionUtils.isNotEmpty(indexMap)) {
                indices = indexMap.stream().map(indexs -> indexs.get("index")).collect(Collectors.toList());
            }
        }
        return indices;
    }

    private String generateIndexName() {
        Date now = DateUtils.getCurrentUtcTime();
        String date = DateUtils.DateToString(now, DateStyle.YYYY_MM_DOT);
        String indexName = CoreConstant.ES_INDEX_MYSQL_SLOW_LOG + CommonConstant.LINE + date;
        return indexName;
    }

    @Override
    public String resultByGetRestClient(RestHighLevelClient client, MiddlewareClusterDTO cluster, String endPoint) throws Exception {
        if (cluster == null) {
            return null;
        }
        Request request = new Request("GET", endPoint);
        MiddlewareClusterLoggingInfo elasticSearch = Optional.ofNullable(cluster).map(MiddlewareClusterDTO::getLogging).map(MiddlewareClusterLogging::getElasticSearch).orElse(null);
        if (elasticSearch == null || StringUtils.isEmpty(elasticSearch.getHost())) {
            throw new BusinessException(ErrorMessage.CLUSTER_ES_SERVICE_ERROR);
        }
        String userName = elasticSearch.getUser();
        String password = elasticSearch.getPassword();
        if (StringUtils.isNotEmpty(userName) && StringUtils.isNotEmpty(password)) {
            RequestOptions.Builder builder = RequestOptions.DEFAULT.toBuilder();
            builder.addHeader("Authorization", "Basic " + getAuthorization(cluster)); // (1)
            builder.setHttpAsyncResponseConsumerFactory(           // (2)
                    new HttpAsyncResponseConsumerFactory.HeapBufferedResponseConsumerFactory(30 * 1024 * 1024));
            // 调用build()方法创建对象
            RequestOptions build = builder.build();
            request.setOptions(build);
        }
        RestClient restClient = client.getLowLevelClient();
        Response response = null;
        try {
            response = restClient.performRequest(request);
        } catch (ConnectException e) {
            log.error("日志组件连接失败", e);
            throw new BusinessException(ErrorMessage.ELASTICSEARCH_CONNECT_FAILED);
        }
        if (response == null || Objects.isNull(response.getEntity())) {
            return null;
        }
        return EntityUtils.toString(response.getEntity());
    }


    private PageObject<MysqlLogDTO> searchFromIndex(RestHighLevelClient esClient, BoolQueryBuilder query, Integer current, Integer size, List<String> indexNameList) throws IOException {
        SortBuilder sortBuilder = SortBuilders.fieldSort("@timestamp")
                .order(SortOrder.DESC).unmappedType("integer");
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(query).sort(sortBuilder).from((current - CommonConstant.NUM_ONE) * size).size(size).explain(true);
        SearchRequest request = multiIndexSearch(searchSourceBuilder, indexNameList);
        request.types(CoreConstant.ES_TYPE_MYSQL_SLOW_LOG).source(searchSourceBuilder);
        SearchResponse response;
        response = esClient.search(request, RequestOptions.DEFAULT);
        Iterator<SearchHit> it = response.getHits().iterator();
        List<MysqlLogDTO> searchResults = new ArrayList<>();
        while (it.hasNext()) {
            SearchHit sh = it.next();
            Map<String, Object> doc = sh.getSourceAsMap();
            MysqlLogDTO mysqlSlowSqlDTO = new MysqlLogDTO();
            mysqlSlowSqlDTO.toDTO(doc);
            searchResults.add(mysqlSlowSqlDTO);
        }
        long totalHits = response.getHits().totalHits;
        PageObject<MysqlLogDTO> objectPageObject = new PageObject(searchResults, new Long(totalHits).intValue());
        return objectPageObject;
    }

    private SearchRequest multiIndexSearch(SearchSourceBuilder build, List<String> indexList) {
        SearchRequest request = new SearchRequest();
        List<Object> objectList = new ArrayList<>();
        for (String s : indexList) {
            objectList.add(s);
        }
        String[] strArray = objectList.toArray(new String[objectList.size()]);
        request.indices(strArray);
        request.source(build);
        return request;
    }

    @Override
    public IndexResponse saveAuditRepository(BeanOperationAudit beanRequest, String clusterId) {
        try {
            MiddlewareClusterDTO middlewareClusterDTO = clusterService.findById(clusterId);
            RestHighLevelClient restHighLevelClient = esService.getEsClient(middlewareClusterDTO);

            String indexName = "audit-" + EsIndexUtil.getSuffix();
            IndexRequest request = new IndexRequest(indexName, "_doc");
            String source = JSONObject.toJSONString(beanRequest);
            request.source(source, XContentType.JSON);
            IndexResponse response = restHighLevelClient.index(request, RequestOptions.DEFAULT);
            return response;
        } catch (IOException e) {
            log.error("审计日志插入失败！", e);
        } catch (Exception e){
            log.error("审计日志插入失败！", e);
        }
        return null;
    }

}
