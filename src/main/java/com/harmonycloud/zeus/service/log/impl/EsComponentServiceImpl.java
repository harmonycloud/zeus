package com.harmonycloud.zeus.service.log.impl;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.middleware.caas.common.constants.CommonConstant;
import com.middleware.caas.common.constants.CoreConstant;
import com.middleware.caas.common.constants.DateStyle;
import com.middleware.caas.common.constants.LogConstant;
import com.middleware.caas.common.enums.ComponentsEnum;
import com.middleware.caas.common.enums.DictEnum;
import com.middleware.caas.common.enums.ErrorMessage;
import com.middleware.caas.common.enums.EsSearchTypeEnum;
import com.middleware.caas.common.exception.BusinessException;
import com.middleware.caas.common.model.ClusterComponentsDto;
import com.middleware.tool.api.client.ElasticSearchClient;
import com.middleware.tool.date.DateUtils;
import com.middleware.tool.json.JsonUtil;
import com.middleware.tool.page.PageObject;
import com.harmonycloud.zeus.bean.BeanOperationAudit;
import com.harmonycloud.zeus.service.k8s.ClusterComponentService;
import com.harmonycloud.zeus.service.log.EsComponentService;
import com.harmonycloud.zeus.service.middleware.EsService;
import com.harmonycloud.zeus.util.EsIndexUtil;
import com.middleware.caas.common.model.middleware.MysqlLogDTO;
import com.middleware.caas.common.model.middleware.MysqlLogQuery;
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
import org.elasticsearch.xcontent.XContentType;
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
    private EsService esService;

    @Autowired
    private ClusterComponentService clusterComponentService;

    @Override
    public RestHighLevelClient getEsClient(String clusterId) throws Exception {
        RestHighLevelClient client = esClients.get(clusterId);
        if (client == null) {
            client = this.createEsClient(clusterId);
            esClients.put(clusterId, client);
        }
        return client;
    }

    @Override
    public RestHighLevelClient resetEsClient(String clusterId) {
        RestHighLevelClient client = this.createEsClient(clusterId);
        esClients.put(clusterId, client);
        return client;
    }

    @Override
    public RestClient getLowLevelClient(String clusterId) {
        RestHighLevelClient client = esClients.get(clusterId);
        if (client == null) {
            client = this.createEsClient(clusterId);
            esClients.put(clusterId, client);
        }
        return client.getLowLevelClient();
    }

    @Override
    public boolean isExistIndex(String index, String clusterId) throws Exception {
        GetIndexRequest indexRequest = new GetIndexRequest().indices(index);
        boolean exists = this.getEsClient(clusterId).indices().exists(indexRequest, RequestOptions.DEFAULT);
        return exists;
    }

    @Override
    public boolean deleteIndex(String indexName, String clusterId) throws Exception {
        if (!isExistIndex(indexName, clusterId)) {
            throw new BusinessException(ErrorMessage.NOT_FOUND, DictEnum.LOG_INDEX.phrase());
        }
        DeleteIndexRequest deleteIndexRequest = new DeleteIndexRequest(indexName);
        deleteIndexRequest.indicesOptions(IndicesOptions.LENIENT_EXPAND_OPEN);
        AcknowledgedResponse response = this.getEsClient(clusterId).indices().delete(deleteIndexRequest, RequestOptions.DEFAULT);
        return response.isAcknowledged();

    }

    private RestHighLevelClient createEsClient(String clusterId) {
        ClusterComponentsDto es = clusterComponentService.get(clusterId, ComponentsEnum.LOGGING.getName());
        if (es == null) {
            throw new BusinessException(ErrorMessage.CLUSTER_ES_SERVICE_ERROR);
        }
        String protocol = es.getProtocol();
        String userName = es.getUsername();
        String password = es.getPassword();
        String esHost = es.getHost();
        Integer port = Integer.valueOf(es.getPort());
        if (StringUtils.isEmpty(userName)) {
            // userName不能等于null
            userName = "";
        }
        try {
            RestHighLevelClient highLevelClient =
                    ElasticSearchClient.getHighLevelClient(protocol, esHost, userName, password, port);
            return highLevelClient;
        } catch (Exception e) {
            log.error("创建ElasticSearch Client 失败,cluster:{}", JSONObject.toJSONString(clusterId), e);
            throw new BusinessException(ErrorMessage.CLUSTER_ES_SERVICE_ERROR);
        }
    }

    @Override
    public PageObject<MysqlLogDTO> getSlowSql(String clusterId, MysqlLogQuery slowLogQuery) throws Exception {
        RestHighLevelClient esClient = esClients.get(clusterId);
        if (esClient == null) {
            esClient = this.getEsClient(clusterId);
        }
        BoolQueryBuilder query = this.getSearchRequestBuilder(slowLogQuery);
        //根据时间范围判断落在哪几个索引
        List<String> indexNameList = getExistIndexNames(esClient, clusterId, slowLogQuery.getStartTime(), slowLogQuery.getEndTime(), LogConstant.MYSQL_SLOW_SQL);
        if (CollectionUtils.isEmpty(indexNameList)) {
            return new PageObject<>(new ArrayList<>(), CommonConstant.NUM_ZERO);
        }
        PageObject<MysqlLogDTO> mysqlSlowSqlDTOPageObject = searchFromIndex(esClient, query, slowLogQuery.getCurrent(), slowLogQuery.getSize(), indexNameList);
        return mysqlSlowSqlDTOPageObject;
    }

    @Override
    public PageObject<MysqlLogDTO> getAuditSql(String clusterId, MysqlLogQuery auditLogQuery) throws Exception {
        RestHighLevelClient esClient = esClients.get(clusterId);
        if (esClient == null) {
            esClient = this.getEsClient(clusterId);
        }
        if (auditLogQuery.getCurrent() == null) {
            auditLogQuery.setCurrent(1);
        }
        if (auditLogQuery.getSize() == null) {
            auditLogQuery.setSize(10);
        }
        BoolQueryBuilder query = this.getAuditSearchRequestBuilder(auditLogQuery);
        // 获取SQL审计所有索引
        List<String> indexNameList = getExistIndexNames(esClient, clusterId, auditLogQuery.getStartTime(),  auditLogQuery.getEndTime(), LogConstant.MYSQL_AUDIT_SQL);
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
     * @param clusterId
     * @return
     */
    @Override
    public Boolean checkEsConnection(String clusterId) {
        try {
            RestHighLevelClient client = this.createEsClient(clusterId);
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

    private void transferIndex(String index, String clusterId) throws Exception {

        if (!isExistIndex(index, clusterId)) {
            log.info("索引不存在，index:{}, clusterId:{}", index, clusterId);
        }
        log.info("索引模板创建后，对集群{}的索引{}迁移数据", clusterId, index);
        String backupIndexName = "backup-" + index;
        BulkByScrollResponse res = this.reIndex(index, backupIndexName, clusterId);
        log.info(res.getStatus().toString());
        deleteIndex(index, clusterId);
        this.reIndex(backupIndexName, index, clusterId);
    }

    private BulkByScrollResponse reIndex(String sourceIndex, String destIndex, String clusterId) throws IOException {
        ReindexRequest reindexRequest = new ReindexRequest();
        reindexRequest.setSourceIndices(sourceIndex);
        reindexRequest.setDestIndex(destIndex);
        return esClients.get(clusterId).reindex(reindexRequest, RequestOptions.DEFAULT);
    }

    private String getAuthorization(String clusterId) {
        ClusterComponentsDto es = clusterComponentService.get(clusterId, ComponentsEnum.LOGGING.getName());
        if (StringUtils.isEmpty(es.getUsername()) || StringUtils.isEmpty(es.getPassword())) {
            return "";
        }
        try {
            return Base64.getEncoder().encodeToString((es.getUsername() + ":" + es.getPassword()).getBytes("UTF-8"));
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
            //日志内容关键字查询
            String keyWord = auditLogQuery.getSearchWord();
            query = query.must(QueryBuilders.matchPhraseQuery("query", keyWord));
        }
        return query;
    }

    private List<String> getExistIndexNames(RestHighLevelClient esClient, String clusterId, String startTime, String endTime,String indexPrefix) throws Exception {
        List<String> indexNameList = new ArrayList<>();
        if (StringUtils.isNotBlank(startTime) && StringUtils.isNotBlank(endTime)) {
            Date startDate = DateUtils.parseUTCSDate(startTime);
            Date endDate = DateUtils.parseUTCSDate(endTime);

            Calendar dayc1 = new GregorianCalendar();
            Calendar dayc2 = new GregorianCalendar();
            dayc1.setTime(startDate); //设置calendar的日期
            dayc2.setTime(endDate);
            //dayc1在dayc2之前就循环
            while (dayc1.compareTo(dayc2) <= 0) {
                String s = (dayc1.get(Calendar.YEAR) + "年" +
                        (dayc1.get(Calendar.MONTH) + 1) + "月" + dayc1.get(Calendar.DATE)) + "日";
                indexNameList.add(
                        indexPrefix + CommonConstant.LINE +
                                dayc1.get(Calendar.YEAR) + CommonConstant.DOT +
                                String.format("%02d", (dayc1.get(Calendar.MONTH) + 1)) + CommonConstant.DOT +
                                String.format("%02d", dayc1.get(Calendar.DATE))
                );

                dayc1.add(Calendar.DAY_OF_YEAR, CommonConstant.NUM_ONE); //加1天
            }
        }
        indexNameList = CollectionUtils.isNotEmpty(indexNameList) ? indexNameList : Arrays.asList(generateIndexName(indexPrefix));
        // 取得已存在的索引
        String result = resultByGetRestClient(esClient, clusterId, "/_cat/indices/" + indexPrefix + "-*?format=json");
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

    private String generateIndexName(String indexPrefix) {
        Date now = DateUtils.getCurrentUtcTime();
        String date = DateUtils.DateToString(now, DateStyle.YYYY_MM_DOT);
        String indexName = indexPrefix + CommonConstant.LINE + date;
        return indexName;
    }

    @Override
    public String resultByGetRestClient(RestHighLevelClient client, String clusterId, String endPoint) throws Exception {
        Request request = new Request("GET", endPoint);
        ClusterComponentsDto es = clusterComponentService.get(clusterId, ComponentsEnum.LOGGING.getName());
        if (es == null) {
            throw new BusinessException(ErrorMessage.CLUSTER_ES_SERVICE_ERROR);
        }
        String userName = es.getUsername();
        String password = es.getPassword();
        if (StringUtils.isNotEmpty(userName) && StringUtils.isNotEmpty(password)) {
            RequestOptions.Builder builder = RequestOptions.DEFAULT.toBuilder();
            builder.addHeader("Authorization", "Basic " + getAuthorization(clusterId)); // (1)
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
        request.types(CoreConstant.ES_TYPE_MYSQL_AUDIT_LOG).source(searchSourceBuilder);
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
        long totalHits = response.getHits().getTotalHits().value;
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
            RestHighLevelClient restHighLevelClient = esService.getEsClient(clusterId);

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
