package com.harmonycloud.zeus.service.middleware.impl;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.middleware.caas.common.constants.CommonConstant;
import com.middleware.caas.common.constants.CoreConstant;
import com.middleware.caas.common.enums.*;
import com.middleware.caas.common.exception.BusinessException;
import com.middleware.caas.common.model.ClusterComponentsDto;
import com.middleware.tool.api.client.ElasticSearchClient;
import com.middleware.tool.date.DateUtils;
import com.middleware.tool.json.JsonUtil;
import com.middleware.tool.page.PageObject;
import com.harmonycloud.zeus.httpservice.HttpServiceClient;
import com.harmonycloud.zeus.service.k8s.ClusterComponentService;
import com.harmonycloud.zeus.service.k8s.ClusterService;
import com.harmonycloud.zeus.service.middleware.AbstractMiddlewareService;
import com.harmonycloud.zeus.service.middleware.EsService;
import com.middleware.caas.common.model.middleware.MiddlewareClusterDTO;
import com.middleware.caas.common.model.middleware.MysqlLogDTO;
import com.middleware.caas.common.model.middleware.MysqlLogQuery;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.util.EntityUtils;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthRequest;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.admin.indices.get.GetIndexRequest;
import org.elasticsearch.action.admin.indices.settings.put.UpdateSettingsRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.support.IndicesOptions;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.client.*;
import org.elasticsearch.client.indices.PutIndexTemplateRequest;
import org.elasticsearch.cluster.health.ClusterHealthStatus;
import org.elasticsearch.common.settings.Settings;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * @author dengyulong
 * @date 2021/03/23
 */
@Service
@Slf4j
public class EsServiceImpl extends AbstractMiddlewareService implements EsService {

    private Map<String, List<String>> indexMap = new ConcurrentHashMap<>();

    @Value("${elasticsearch.index.prefix:middlewarelogstash-}")
    private String esIndexPrefix;
    @Value("${elasticsearch.cluster-name:kubernetes-logging}")
    private String esClusterName;
    @Value("${elasticsearch.pod.index.prefix:middlewarestdout-}")
    private String esPodIndexPrefix;
    @Autowired
    private HttpServiceClient httpServiceClient;
    @Autowired
    private ClusterComponentService clusterComponentService;

    private Map<String, RestHighLevelClient> esClients = new ConcurrentHashMap<>();

    @Autowired
    private ClusterService clusterService;

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
        List<String> indexNameList = getExistIndexNames(esClient, clusterId, slowLogQuery.getStartTime(), slowLogQuery.getEndTime());
        if (CollectionUtils.isEmpty(indexNameList)) {
            return new PageObject<>(new ArrayList<>(), CommonConstant.NUM_ZERO);
        }
        PageObject<MysqlLogDTO> mysqlSlowSqlDTOPageObject = searchFromIndex(esClient, query, slowLogQuery.getCurrent(), slowLogQuery.getSize(), indexNameList);
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
        ClusterComponentsDto elasticSearch = clusterComponentService.get(clusterId, ComponentsEnum.LOGGING.getName());
        if (StringUtils.isEmpty(elasticSearch.getUsername()) || StringUtils.isEmpty(elasticSearch.getPassword())) {
            return "";
        }
        try {
            return Base64.getEncoder().encodeToString((elasticSearch.getUsername() + ":" + elasticSearch.getPassword()).getBytes("UTF-8"));
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
        return query;
    }

    private List<String> getExistIndexNames(RestHighLevelClient esClient, String clusterId, String startTime, String endTime) throws Exception {
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
        String result = resultByGetRestClient(esClient, clusterId, "/_cat/indices?format=json");
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

    private String generateIndexName() {
        Date now = DateUtils.getCurrentUtcTime();
        String date = DateUtils.DateToString(now, DateType.YYYY_MM_DOT.getValue());
        String indexName = CoreConstant.ES_INDEX_MYSQL_SLOW_LOG + CommonConstant.LINE + date;
        return indexName;
    }

    @Override
    public String resultByGetRestClient(RestHighLevelClient client, String clusterId, String endPoint) throws Exception {
        Request request = new Request("GET", endPoint);
        ClusterComponentsDto es = clusterComponentService.get(clusterId, ComponentsEnum.LOGGING.getName());
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
        Response response = restClient.performRequest(request);
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
    public String getLogIndexPrefix(boolean isPodLog) {
        if (isPodLog) {
            return esPodIndexPrefix;
        }
        return esIndexPrefix;
    }

    /**
     * 更新索引的最大搜索结果记录数量
     *
     * @param client
     * @param index
     */
    @Override
    public void updateIndexMaxResultWindow(RestHighLevelClient client, String[] index, int maxResultWindow) throws IOException {
        UpdateSettingsRequest request = new UpdateSettingsRequest();
        request.indices(index);

        Settings settings = Settings.builder()
                .put("index.max_result_window", maxResultWindow).build();
        request.settings(settings);
        client.indices().putSettings(request, RequestOptions.DEFAULT);
    }

    @Override
    public List<String> getIndexes(String clusterId) throws Exception {
        RestHighLevelClient esClient = esClients.get(clusterId);
        if (esClient == null) {
            esClient = this.getEsClient(clusterId);
        }
        // 取得已存在的索引
        String result = resultByGetRestClient(esClient, clusterId, "/_cat/indices?format=json");

        List<String> indices = new ArrayList<>();
        if (StringUtils.isNotEmpty(result)) {
            List<Map<String, String>> indexMap = JsonUtil.jsonToPojo(result, ArrayList.class);
            if (CollectionUtils.isNotEmpty(indexMap)) {
                indices = indexMap.stream().map(indexs -> indexs.get("index")).collect(Collectors.toList());
            }
        }

        if (!CollectionUtils.isEmpty(indices)) {
            indexMap.put(clusterId, indices);
        }
        return indexMap.get(clusterId);
    }

    /**
     * 创建es的模板索引
     */
    @Override
    public void initEsIndexTemplate() {
        List<MiddlewareClusterDTO> clusterDTOS = clusterService.listClusters();
        clusterDTOS.forEach(clusterDTO -> {
            initEsIndexTemplate(clusterDTO.getId());
        });
    }

    @Override
    public boolean initEsIndexTemplate(String clusterId) {
        ClusterComponentsDto es = clusterComponentService.get(clusterId, ComponentsEnum.LOGGING.getName());
        if (es == null) {
            log.info("集群未配置日志组件");
            return false;
        }
        try {
            RestHighLevelClient esClient = getEsClient(clusterId);
            int esVersion = getEsVersion(clusterId);

            //初始化mysql慢日志模板
            initMysqlSlowLogIndexTemplate(esClient);
            //初始化标准输入日志索引模板
            initStdoutIndexTemplate(esClient, esVersion);
            //初始化文件日志索引模板
            initLogstashIndexTemplate(esClient, esVersion);
            //初始化mysql SQL审计模版
            initAuditSqlTemplate(esClient, esVersion);
            log.info("集群:{}索引模板初始化完成", clusterId);
            return true;
        } catch (Exception e) {
            log.error("集群:{}索引模板初始化失败", clusterId, e);
            return false;
        }
    }

    /**
     * 初始化mysql慢日志索引模板
     *
     * @param esClient
     * @author liyinlong
     * @date 2021/7/20 3:56 下午
     */
    public void initMysqlSlowLogIndexTemplate(RestHighLevelClient esClient) {
        try {
            PutIndexTemplateRequest request = new PutIndexTemplateRequest(EsTemplateEnum.MYSQL_SLOW_LOG.getName());
            JSONObject codeJson = JSONObject.parseObject(EsTemplateEnum.MYSQL_SLOW_LOG.getCode());
            setCommonTemplate(request, codeJson);
            esClient.indices().putTemplate(request, RequestOptions.DEFAULT);
            log.info("mysql慢日志索引模板初始化成功");
        } catch (Exception e) {
            log.error("mysql慢日志索引模板初始化失败", e);
        }
    }

    /**
     * 初始化操作审计索引模板
     *
     * @param esClient
     * @author liyinlong
     * @date 2021/7/21 9:30 上午
     */
    public void initAuditIndexTemplate(RestHighLevelClient esClient) {
        try {
            PutIndexTemplateRequest request = new PutIndexTemplateRequest(EsTemplateEnum.AUDIT.getName());
            JSONObject codeJson = JSONObject.parseObject(EsTemplateEnum.AUDIT.getCode());
            setCommonTemplate(request, codeJson);
            JSONObject mappings = codeJson.getJSONObject("mappings").getJSONObject("user_op_audit");
            request.mapping(mappings.toString(), XContentType.JSON);
            esClient.indices().putTemplate(request, RequestOptions.DEFAULT);
            log.info("操作审计索引模板初始化成功");
        } catch (IOException e) {
            log.error("操作审计索引模板初始化失败", e);
        }
    }

    /**
     * 初始化标准输出日志索引模板
     *
     * @param esClient
     * @author liyinlong
     * @date 2021/8/11 4:54 下午
     */
    public void initStdoutIndexTemplate(RestHighLevelClient esClient, int esVersion) {
        try {
            PutIndexTemplateRequest request = new PutIndexTemplateRequest(EsTemplateEnum.STDOUT.getName());
            JSONObject codeJson = JSONObject.parseObject(EsTemplateEnum.STDOUT.getCode());
            setCommonTemplate(request, codeJson);
            JSONObject mappings = getMappings(codeJson, esVersion);
            request.mapping(mappings.toString(), XContentType.JSON);
            esClient.indices().putTemplate(request, RequestOptions.DEFAULT);
            log.info("标准输出日志索引模板初始化成功");
        } catch (Exception e) {
            log.error("标准输出日志索引模板初始化失败", e);
        }
    }

    /**
     * 初始化文件日志索引模板
     *
     * @param esClient
     * @author liyinlong
     * @date 2021/8/11 4:58 下午
     */
    public void initLogstashIndexTemplate(RestHighLevelClient esClient, int esVersion) {
        try {
            PutIndexTemplateRequest request = new PutIndexTemplateRequest(EsTemplateEnum.LOG_STASH.getName());
            JSONObject codeJson = JSONObject.parseObject(EsTemplateEnum.LOG_STASH.getCode());
            setCommonTemplate(request, codeJson);
            JSONObject mappings = getMappings(codeJson, esVersion);
            request.mapping(mappings.toString(), XContentType.JSON);
            esClient.indices().putTemplate(request, RequestOptions.DEFAULT);
            log.info("文件日志索引模板logstash初始化成功");
        } catch (Exception e) {
            log.error("文件日志索引模板logstash初始化失败", e);
        }
    }

    /**
     * 初始化mysql sql审计索引模板
     *
     * @param esClient
     */
    public void initAuditSqlTemplate(RestHighLevelClient esClient, int esVersion) {
        try {
            PutIndexTemplateRequest request = new PutIndexTemplateRequest(EsTemplateEnum.MYSQL_AUDIT_SQL.getName());
            JSONObject codeJson = JSONObject.parseObject(EsTemplateEnum.MYSQL_AUDIT_SQL.getCode());
            setCommonTemplate(request, codeJson);
            JSONObject mappings = getMappings(codeJson, esVersion);
            request.mapping(mappings.toString(), XContentType.JSON);
            esClient.indices().putTemplate(request, RequestOptions.DEFAULT);
            log.info("SQL审计索引模板mysqlaudit初始化成功");
        } catch (Exception e) {
            log.error("SQL审计索引模板mysqlaudit初始化失败", e);
            e.printStackTrace();
        }
    }

    /**
     * 设置索引模板通用属性
     *
     * @param request
     * @param codeJson
     * @author liyinlong
     * @date 2021/7/21 9:31 上午
     */
    public void setCommonTemplate(PutIndexTemplateRequest request, JSONObject codeJson) {
        request.settings(codeJson.getJSONObject("settings").toString(), XContentType.JSON);
        ArrayList<String> list = new ArrayList<>();
        for (Object pattern : codeJson.getJSONArray("index_patterns")) {
            list.add(pattern.toString());
        }
        if (codeJson.getJSONObject("mappings") != null) {
            request.mapping(codeJson.getJSONObject("mappings").toJSONString(), XContentType.JSON);
        }
        request.patterns(list);
        request.order(codeJson.getIntValue("order"));
    }

    /**
     * 获取索引mappings
     *
     * @param codeJson
     * @param version
     * @return
     */
    public JSONObject getMappings(JSONObject codeJson, int version) {
        if (version == 7) {
            return codeJson.getJSONObject("mappings").getJSONObject("doc");
        } else {
            return codeJson.getJSONObject("mappings");
        }
    }

    public int getEsVersion(String clusterId) {
        ClusterComponentsDto es = clusterComponentService.get(clusterId, ComponentsEnum.LOGGING.getName());
        if (es != null) {
            try {
                String host = es.getHost();
                String port = es.getPort();
                String user = es.getUsername();
                String protocol = es.getProtocol();
                String password = es.getPassword();
                String auth = user + ":" + password;
                String authorization = "Basic " + Base64.getEncoder().encodeToString(auth.getBytes());
                JSONObject jsonObject = httpServiceClient.clusterInfo(protocol, host, port, authorization).getJSONObject("version");
                String number = jsonObject.getString("number").substring(0, 1);
                return Integer.parseInt(number);
            } catch (NumberFormatException e) {
                log.error("查询es版本出错了");
                return 6;
            }
        }
        return 6;
    }

}