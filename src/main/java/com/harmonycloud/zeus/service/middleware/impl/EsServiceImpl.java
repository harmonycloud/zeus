package com.harmonycloud.zeus.service.middleware.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.harmonycloud.caas.common.constants.CommonConstant;
import com.harmonycloud.caas.common.constants.CoreConstant;
import com.harmonycloud.caas.common.enums.DateType;
import com.harmonycloud.caas.common.enums.DictEnum;
import com.harmonycloud.caas.common.enums.ErrorMessage;
import com.harmonycloud.caas.common.enums.EsTemplateEnum;
import com.harmonycloud.caas.common.exception.BusinessException;
import com.harmonycloud.caas.common.model.middleware.*;
import com.harmonycloud.tool.api.client.ElasticSearchClient;
import com.harmonycloud.tool.date.DateUtils;
import com.harmonycloud.tool.json.JsonUtil;
import com.harmonycloud.tool.page.PageObject;
import com.harmonycloud.zeus.httpservice.HttpServiceClient;
import com.harmonycloud.zeus.service.k8s.ClusterService;
import com.harmonycloud.zeus.service.k8s.MiddlewareClusterService;
import com.harmonycloud.zeus.service.middleware.AbstractMiddlewareService;
import com.harmonycloud.zeus.service.middleware.EsService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.util.EntityUtils;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthRequest;
import org.elasticsearch.action.admin.cluster.settings.ClusterGetSettingsRequest;
import org.elasticsearch.action.admin.cluster.settings.ClusterGetSettingsResponse;
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

    private Map<String, RestHighLevelClient> esClients = new ConcurrentHashMap<>();

    @Autowired
    private ClusterService clusterService;

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

    private String generateIndexName() {
        Date now = DateUtils.getCurrentUtcTime();
        String date = DateUtils.DateToString(now, DateType.YYYY_MM_DOT.getValue());
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
    public List<String> getIndexes(MiddlewareClusterDTO cluster) throws Exception {
        if (cluster == null) {
            return null;
        }
        String clusterId = cluster.getId();
        RestHighLevelClient esClient = esClients.get(clusterId);
        if (esClient == null) {
            esClient = this.getEsClient(clusterService.findById(clusterId));
        }
        // 取得已存在的索引
        String result = resultByGetRestClient(esClient, cluster, "/_cat/indices?format=json");

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
        initEsIndexTemplate(null);
    }

    @Override
    public boolean initEsIndexTemplate(String clusterId) {
        List<MiddlewareClusterDTO> clusters = clusterService.listClusters();
        if (StringUtils.isNotBlank(clusterId)) {
            clusters = clusters.stream().filter(middlewareClusterDTO -> clusterId.equals(middlewareClusterDTO.getId())).collect(Collectors.toList());
        }
        for (MiddlewareClusterDTO cluster : clusters) {
            MiddlewareClusterLogging logging = cluster.getLogging();
            if (logging == null | logging.getElasticSearch() == null) {
                log.info("集群未配置日志组件");
                return false;
            }
            try {
                RestHighLevelClient esClient = getEsClient(cluster);
                int esVersion = getEsVersion(cluster);

                //初始化mysql慢日志模板
                initMysqlSlowLogIndexTemplate(esClient);
                //初始化标准输入日志索引模板
                initStdoutIndexTemplate(esClient, esVersion);
                //初始化文件日志索引模板
                initLogstashIndexTemplate(esClient, esVersion);
                //初始化mysql SQL审计模版
                initAuditSqlTemplate(esClient, esVersion);
                log.info("集群:{}索引模板初始化完成", cluster.getName());
                return true;
            } catch (Exception e) {
                log.error("集群:{}索引模板初始化失败", cluster.getName(), e);
                return false;
            }
        }
        return false;
    }

    /**
     * 初始化mysql慢日志索引模板
     * @author liyinlong
     * @date 2021/7/20 3:56 下午
     * @param esClient
     */
    public void initMysqlSlowLogIndexTemplate(RestHighLevelClient esClient){
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
     * @author liyinlong
     * @date 2021/7/21 9:30 上午
     * @param esClient
     */
    public void initAuditIndexTemplate(RestHighLevelClient esClient){
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
     * @author liyinlong
     * @date 2021/8/11 4:54 下午
     * @param esClient
     */
    public void initStdoutIndexTemplate(RestHighLevelClient esClient, int esVersion){
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
     * @author liyinlong
     * @date 2021/8/11 4:58 下午
     * @param esClient
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
     * @author liyinlong
     * @date 2021/7/21 9:31 上午
     * @param request
     * @param codeJson
     */
    public void setCommonTemplate(PutIndexTemplateRequest request, JSONObject codeJson){
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

    public int getEsVersion(MiddlewareClusterDTO clusterDTO) {
        if (clusterDTO != null && clusterDTO.getLogging() != null && clusterDTO.getLogging().getElasticSearch() != null) {
            try {
                MiddlewareClusterLoggingInfo es = clusterDTO.getLogging().getElasticSearch();
                String host = es.getHost();
                String port = es.getPort();
                String user = es.getUser();
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