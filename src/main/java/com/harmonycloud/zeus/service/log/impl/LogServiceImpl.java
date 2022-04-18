package com.harmonycloud.zeus.service.log.impl;


import com.alibaba.fastjson.JSONObject;
import com.harmonycloud.caas.common.base.BaseResult;
import com.harmonycloud.caas.common.enums.*;
import com.harmonycloud.caas.common.exception.BusinessException;
import com.harmonycloud.caas.common.exception.CaasRuntimeException;
import com.harmonycloud.caas.common.model.middleware.LogQuery;
import com.harmonycloud.caas.common.model.middleware.LogQueryDto;
import com.harmonycloud.caas.common.model.middleware.MiddlewareClusterDTO;
import com.harmonycloud.zeus.bean.BeanLogMsg;
import com.harmonycloud.zeus.service.k8s.ClusterService;
import com.harmonycloud.zeus.service.log.LogService;
import com.harmonycloud.zeus.service.middleware.EsService;
import com.harmonycloud.zeus.util.AssertUtil;
import com.harmonycloud.zeus.util.DateUtil;
import com.harmonycloud.tool.date.DateUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.ConnectionClosedException;
import org.elasticsearch.ElasticsearchStatusException;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchScrollRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.*;

import static com.harmonycloud.caas.common.constants.CommonConstant.*;

/**
 * @description 日志service实现类
 * @author: liyinlong
 * @date 2021/7/8 3:52 下午
 */
@Slf4j
@Service
@Scope("prototype")
public class LogServiceImpl implements LogService {

    private static Logger logger = LoggerFactory.getLogger(LogServiceImpl.class);

    /**
     * 每次只能查询100个POD的文件
     */
    private static final int MAX_POD_FETCH_COUNT = 100;
    private static final int MAX_EXPORT_LENGTH = 10000;

    @Value("${shell:#{null}}")
    private String shellStarter;
    @Value("${es.scroll.time:600000}")
    private String scrollTime;

    @Autowired
    private ClusterService clusterService;
    @Autowired
    private EsService esService;

    @Override
    public void exportLog(LogQuery logQuery, HttpServletResponse response) throws Exception {
        AssertUtil.notBlank(logQuery.getClusterId(), DictEnum.CLUSTER_ID);
        OutputStream outputStream = null;
        MiddlewareClusterDTO cluster = clusterService.findById(logQuery.getClusterId());
        RestHighLevelClient client = esService.getEsClient(cluster);
        if (StringUtils.isBlank(logQuery.getMiddlewareName())
                && StringUtils.isBlank(logQuery.getPod())
                && StringUtils.isBlank(logQuery.getContainer())) {
            throw new CaasRuntimeException(String.valueOf(ErrorCodeMessage.NS_POD_CONTAINER_NOT_BLANK));
        }
        logQuery.setPageSize(MAX_EXPORT_LENGTH);
        SearchRequest request = this.getSearchRequestBuilder(logQuery);
        SearchSourceBuilder builder = request.source();
        builder.size(logQuery.getPageSize());
        SearchResponse scrollResp = null;
        try {
            scrollResp = client.search(request, RequestOptions.DEFAULT);
        } catch (ElasticsearchStatusException e) {
            if (isExceedMaxResult(e)) {
                //设置最大查询结果条数
                logger.info("设置日志条数最大搜索条数,index：{}, clusterId:{}",
                        JSONObject.toJSONString(logQuery.getIndexes()), cluster.getId());
                esService.updateIndexMaxResultWindow(client, logQuery.getIndexes(), MAX_EXPORT_LENGTH);
                scrollResp = client.search(request, RequestOptions.DEFAULT);
            } else {
                logger.error("导出日志失败，", e);
                throw new CaasRuntimeException(String.valueOf(ErrorCodeMessage.QUERY_FAIL));
            }
        }
        Long totalHit = scrollResp.getHits().getTotalHits();

        Date now = DateUtils.getCurrentUtcTime();
        String datetime = DateUtils.DateToString(now, DateType.YYMMDDHHMMSS.getValue());

        response.setContentType("multipart/form-data");
        response.setHeader("Content-Disposition", "attachment;fileName=" + logQuery.getMiddlewareName() + "_" + datetime + ".log");
        outputStream = response.getOutputStream();
        try {
            if (totalHit > MAX_EXPORT_LENGTH) {
                outputStream.write(("Find total " + totalHit + " line messages, only export " + MAX_EXPORT_LENGTH + " lines.\n").getBytes());
            }
            for (SearchHit it : scrollResp.getHits().getHits()) {
                outputStream.write((it.getSourceAsMap().get("message").toString() + "\n").getBytes());
            }
        } catch (Exception e) {
            logger.error("导出日志失败", e);
        } finally {
            if (outputStream != null) {
                outputStream.close();
            }
        }

    }

    @Override
    public BaseResult getLogContents(LogQuery logQuery) throws Exception {
        if (StringUtils.isBlank(logQuery.getNamespace())) {
            throw new CaasRuntimeException(String.valueOf(ErrorCodeMessage.PARAMETER_VALUE_NOT_PROVIDE));
        }

        AssertUtil.notBlank(logQuery.getClusterId(), DictEnum.CLUSTER_ID);
        logger.info("查询日志，query:{}", JSONObject.toJSONString(logQuery));
        List<Map<String, Object>> data = new ArrayList<>();
        SearchResponse scrollResp = null;
        String scrollId = logQuery.getScrollId();

        MiddlewareClusterDTO middlewareClusterDTO = clusterService.findById(logQuery.getClusterId());
        RestHighLevelClient client = esService.getEsClient(middlewareClusterDTO);

        if (StringUtils.isBlank(scrollId)) {
            if (StringUtils.isBlank(logQuery.getNamespace())) {
                return BaseResult.error(ErrorMessage.NAMESPACE_NOT_BLANK);
            }
            if (StringUtils.isBlank(logQuery.getMiddlewareName()) && StringUtils.isBlank(logQuery.getPod())
                    && StringUtils.isBlank(logQuery.getContainer())) {
                return BaseResult.error(ErrorMessage.NS_POD_CONTAINER_NOT_BLANK);
            }
            SearchRequest request = this.getSearchRequestBuilder(logQuery);
            SearchSourceBuilder source = request.source();
            source.size(logQuery.getPageSize());
            scrollResp = client.search(request, RequestOptions.DEFAULT);
            scrollId = scrollResp.getScrollId();
        } else {
            SearchScrollRequest request = new SearchScrollRequest(scrollId);
            request.scroll(new TimeValue(Long.parseLong(scrollTime)));
            scrollResp = client.scroll(request, RequestOptions.DEFAULT);
        }
        List<BeanLogMsg> logList = new ArrayList<>();
        for (SearchHit it : scrollResp.getHits().getHits()) {
            String onelogs = it.getSourceAsMap().get("message").toString();
            String offset = it.getSourceAsMap().get("offset").toString();
            String time = it.getSourceAsMap().get("@timestamp").toString();
            logList.add(new BeanLogMsg(Long.parseLong(offset), time, onelogs));
        }
        Map<String, Object> dataWithScrollId = new HashMap<>(2);
        dataWithScrollId.put("log", logList);
        dataWithScrollId.put("scrollId", scrollId);
        dataWithScrollId.put("totalHit", scrollResp.getHits().getTotalHits());
        return BaseResult.ok(dataWithScrollId);
    }

    @Override
    public BaseResult listfileName(LogQuery logQuery) throws Exception {
        AssertUtil.notBlank(logQuery.getClusterId(), DictEnum.CLUSTER_ID);
        TreeSet<String> logFileNames = new TreeSet<String>();
        if (logQuery.getIndexes() == null || logQuery.getIndexes().length == 0) {
            return BaseResult.ok(logFileNames);
        }

        MiddlewareClusterDTO middlewareClusterDTO = clusterService.findById(logQuery.getClusterId());
        RestHighLevelClient client = esService.getEsClient(middlewareClusterDTO);

        if (StringUtils.isBlank(logQuery.getContainer()) || StringUtils.isBlank(logQuery.getPod())) {
            logFileNames.addAll(this.listLogFileNames(logQuery, client, true));
        } else {
            logFileNames.addAll(this.listLogFileNames(logQuery, client, false));
        }

        List<Map<String, String>> fileMapList = new ArrayList<>();
        logFileNames.forEach(item -> {
            if (logQuery.isPodLog()) {
                Map<String, String> singleFileMap = new HashMap<>(8);
                if (item.indexOf(SLASH) == -1) {
                    singleFileMap.put("name", logQuery.getPod());
                    singleFileMap.put("logPath", item);
                    fileMapList.add(singleFileMap);
                } else {
                    String[] files = item.split(SLASH);
                    singleFileMap.put("name", files[0]);
                    singleFileMap.put("logPath", item);
                    fileMapList.add(singleFileMap);
                }
            } else {
                Map<String, String> singleFileMap = new HashMap<>(8);
                singleFileMap.put("name", item);
                singleFileMap.put("logPath", item);
                fileMapList.add(singleFileMap);
            }
        });

        return BaseResult.ok(fileMapList);
    }

    /**
     * 查询某个容器的日志文件名称列表，返回文件名称包含pod名称为前缀
     *
     * @param client
     * @return
     */
    private TreeSet<String> listLogFileNames(LogQuery logQuery, RestHighLevelClient client, boolean withPodName) throws IOException {
        TreeSet<String> logFileNames = new TreeSet<String>();
        SearchResponse scrollResp = null;
        BoolQueryBuilder queryBuilder = this.getQueryBuilder(logQuery);
        SearchSourceBuilder builder = new SearchSourceBuilder();
        SearchRequest request = new SearchRequest();

        if (StringUtils.isNotBlank(logQuery.getPod())) {
            queryBuilder.must(QueryBuilders.matchPhraseQuery("k8s_pod", logQuery.getPod()));
            builder.query(queryBuilder).aggregation(AggregationBuilders.terms("source").field("source"));

            request.indices(esService.getLogIndexPrefix(logQuery.isPodLog()) + "*");
            if (logQuery.getIndexes().length > 0) {
                request.indices(logQuery.getIndexes());
            }
            request.source(builder);
            scrollResp = client.search(request, RequestOptions.DEFAULT);
            if (scrollResp.getAggregations() == null) {
                return logFileNames;
            }
            Terms agg1 = scrollResp.getAggregations().get("source");
            for (Terms.Bucket bucket : agg1.getBuckets()) {
                String name = bucket.getKey().toString();
                if (name.contains("/")) {
                    name = name.substring(name.lastIndexOf("/") + 1);
                }
                logFileNames.add(name);
            }
            return logFileNames;
        }
        builder.query(queryBuilder).sort("offset", SortOrder.DESC)
                .aggregation(AggregationBuilders.terms("k8s_pod").field("k8s_pod").size(MAX_POD_FETCH_COUNT)
                        .subAggregation(AggregationBuilders.terms("source").field("source").size(MAX_POD_FETCH_COUNT)));
        request.indices(esService.getLogIndexPrefix(logQuery.isPodLog()) + "*");
        if (logQuery.getIndexes().length > 0) {
            request.indices(logQuery.getIndexes());
        }
        request.source(builder);
        scrollResp = client.search(request, RequestOptions.DEFAULT);

        if (scrollResp.getAggregations() == null) {
            return logFileNames;
        }
        Terms podTerms = scrollResp.getAggregations().get("k8s_pod");
        Iterator<SearchHit> it = scrollResp.getHits().iterator();

        while (it.hasNext()) {
            SearchHit sh = it.next();
        }
        for (Terms.Bucket bucket : podTerms.getBuckets()) {
            String bucketPodName = bucket.getKey().toString();
            if (!bucketPodName.startsWith(logQuery.getMiddlewareName())) {
                continue;
            }
            Terms logDirTerms = bucket.getAggregations().get("source");

            for (Terms.Bucket dirBucket : logDirTerms.getBuckets()) {
                String logDir = dirBucket.getKey().toString();
                if (logDir.contains("/")) {
                    logDir = logDir.substring(logDir.lastIndexOf("/") + 1);
                }
                if (withPodName) {
                    logFileNames.add(bucketPodName + "/" + logDir);
                } else {
                    logFileNames.add(logDir);
                }
            }
        }

        return logFileNames;
    }


    /**
     * 根据查询条件设置SearchRequestBuilder
     *
     * @param logQuery
     * @return
     */
    private SearchRequest getSearchRequestBuilder(LogQuery logQuery) {
        //日志时间范围查询设置
        BoolQueryBuilder queryBuilder = getQueryBuilder(logQuery);

        if (StringUtils.isNotBlank(logQuery.getPod())) {
            queryBuilder.must(QueryBuilders.matchPhraseQuery("k8s_pod", logQuery.getPod()));
        }
        SearchSourceBuilder builder = new SearchSourceBuilder();

        builder.sort("@timestamp", SortOrder.ASC).sort("offset", SortOrder.ASC);
        builder.query(queryBuilder);
        SearchRequest request = new SearchRequest();

        String repositories = esService.getLogIndexPrefix(logQuery.isPodLog()) + "*";
        request.indices(repositories);
        if (logQuery.getIndexes().length > 0) {
            request.indices(logQuery.getIndexes());
        }
        request.source(builder);
        request.scroll(new TimeValue(Long.parseLong(scrollTime)));

        return request;
    }

    /**
     * 根据查询条件设置SearchRequestBuilder
     *
     * @param client
     * @param logQuery
     * @return
     */
    private SearchSourceBuilder getSearchRequestBuilderByline(RestHighLevelClient client, LogQuery logQuery, String upOrdown, String timestamp) {
        //日志时间范围查询设置
        BoolQueryBuilder queryBuilder = getQueryBuilderByLine(logQuery, upOrdown, timestamp);

        if (StringUtils.isNoneBlank(logQuery.getLogDir())) {
            queryBuilder.must(QueryBuilders.wildcardQuery("source", "*" + logQuery.getLogDir()));
        }
        if (StringUtils.isNotBlank(logQuery.getPod())) {
            queryBuilder.must(QueryBuilders.matchPhraseQuery("k8s_pod", logQuery.getPod()));
        }
        SearchSourceBuilder builder = new SearchSourceBuilder();
        builder.query(queryBuilder);
        SearchRequest request = new SearchRequest();

        request.indices(esService.getLogIndexPrefix(logQuery.isPodLog()) + "*").scroll(new TimeValue(Long.parseLong(scrollTime))).source(builder);

        if (StringUtils.equals(UP, upOrdown)) {
            builder.sort("@timestamp", SortOrder.DESC)
                    .sort("offset", SortOrder.DESC);
        } else {
            builder.sort("@timestamp", SortOrder.ASC)
                    .sort("offset", SortOrder.ASC);
        }
        return builder;
    }


    /**
     * 参数校验，并将接口日志查询对象转换为内部服务查询对象
     *
     * @param logQueryDto 对外接口日志查询对象
     * @return 内部服务日志查询对象
     */
    @Override
    public LogQuery transLogQuery(LogQueryDto logQueryDto) throws Exception {
        Assert.hasText(logQueryDto.getNamespace(), "分区不能为空");
        AssertUtil.notBlank(logQueryDto.getClusterId(), DictEnum.CLUSTER_ID);

        if (StringUtils.isNotBlank(logQueryDto.getScrollId())) {
            LogQuery logQuery = new LogQuery();
            logQuery.setScrollId(logQueryDto.getScrollId());
            logQuery.setNamespace(logQueryDto.getNamespace());
            logQuery.setMiddlewareName(logQueryDto.getMiddlewareName());
            logQuery.setClusterId(logQueryDto.getClusterId());
            logQuery.setPodLog(logQueryDto.isPodLog());
            return logQuery;
        }
        Assert.notNull(logQueryDto, "查询参数不能为空");
        Assert.hasText(logQueryDto.getMiddlewareName(), "服务名不能为空");
        String fromDate = "";
        String toDate = "";
        String[] indexes;
        // ES中日志时间戳为0时区
        String style = DateType.YYYY_MM_DD_T_HH_MM_SS_Z.getValue();
        if (StringUtils.isNotBlank(logQueryDto.getLogTimeStart())
                && StringUtils.isNotBlank(logQueryDto.getLogTimeEnd())) {
            fromDate = logQueryDto.getLogTimeStart();
            toDate = logQueryDto.getLogTimeEnd();
        } else {
            if (logQueryDto.getRecentTimeNum() == null || logQueryDto.getRecentTimeNum() == 0) {
                logQueryDto.setRecentTimeNum(DEFAULT_LOG_QUERY_TIME);
                logQueryDto.setRecentTimeUnit(TIME_UNIT_MINUTES);
            }
            SimpleDateFormat format = new SimpleDateFormat(style);
            Date current = new Date();
            Date from = DateUtil.addTime(current, logQueryDto.getRecentTimeUnit(),
                    -logQueryDto.getRecentTimeNum());
            fromDate = format.format(from);
            toDate = format.format(current);
        }
        logger.info("Query log time, fromDate:{},toDate:{}", fromDate, toDate);
        LogQuery logQuery = new LogQuery();
        BeanUtils.copyProperties(logQueryDto, logQuery);
        logQuery.setPodLog(logQueryDto.isPodLog());
        logQuery.setLogDateStart(fromDate);
        logQuery.setLogDateEnd(toDate);
        //获取查询时间段对应的索引列表
        Date startDate = DateUtil.StringToDate(fromDate, style);
        Date endDate = DateUtil.StringToDate(toDate, style);
        if (!endDate.after(startDate)) {
            throw new CaasRuntimeException(String.valueOf(ErrorCodeMessage.DATE_FROM_AFTER_TO));
        }
        indexes = this.getIndexes(startDate, endDate, logQueryDto.getClusterId(), logQueryDto.isPodLog());
        logQuery.setIndexes(indexes);
        if (logQueryDto.getPageSize() == null) {
            logQuery.setPageSize(DEFAULT_PAGE_SIZE_200);
        } else if (logQueryDto.getPageSize() > MAX_PAGE_SIZE_1000) {
            logQuery.setPageSize(MAX_PAGE_SIZE_1000);
        }
        if (StringUtils.isBlank(logQuery.getSearchType())) {
            logQuery.setSearchType(EsSearchTypeEnum.MATCH_PHRASE.getCode());
        } else if (EsSearchTypeEnum.getByCode(logQuery.getSearchType()) == null) {
            throw new CaasRuntimeException(String.valueOf(ErrorCodeMessage.LOG_SEARCH_TYPE_NOT_SUPPORT));
        }
        return logQuery;
    }

    /**
     * 根据查询的时间区间 返回该时间段内es的索引列表
     *
     * @param from
     * @param to
     * @return
     */
    private String[] getIndexes(Date from, Date to, String clusterId, boolean isPodLog) throws Exception {
        Set<String> indexes = new HashSet<>();
        Date indexDate = from;
        MiddlewareClusterDTO middlewareClusterDTO = clusterService.findById(clusterId);
        List<String> existIndexes = null;
        try {
            existIndexes = esService.getIndexes(middlewareClusterDTO);
        } catch (ConnectionClosedException e) {
            log.error("连接elasticsearch出错了", e);
            throw new BusinessException(ErrorMessage.ELASTICSEARCH_CONNECT_FAILED);
        }
        while (indexDate.before(to)) {
            String index = esService.getLogIndexPrefix(isPodLog) + DateUtil.DateToString(indexDate, DateType.YYYYMMDD_DOT);
            String snapshotIndex = index + ES_INDEX_SNAPSHOT_RESTORE;
            if (existIndexes.contains(index)) {
                indexes.add(index);
            }
            //同时查询快照恢复的索引
            if (existIndexes.contains(snapshotIndex)) {
                indexes.add(snapshotIndex);
            }
            indexDate = DateUtil.addDay(indexDate, 1);
        }
        //添加最后一天的索引
        String lastIndex = esService.getLogIndexPrefix(isPodLog) + DateUtil.DateToString(to, DateType.YYYYMMDD_DOT);
        String lastSnapshotIndex = lastIndex + ES_INDEX_SNAPSHOT_RESTORE;
        if (existIndexes.contains(lastIndex) && !indexes.contains(lastIndex)) {
            indexes.add(lastIndex);
        }
        if (existIndexes.contains(lastSnapshotIndex) && !indexes.contains(lastSnapshotIndex)) {
            indexes.add(lastSnapshotIndex);
        }
        // 兼容日志收集filebeat升级前后创建索引时间问题,
        Date next = DateUtil.addDay(to, 1);
        String nextIndex = esService.getLogIndexPrefix(isPodLog) + DateUtil.DateToString(next, DateType.YYYYMMDD_DOT);
        String nextSnapshotIndex = nextIndex + ES_INDEX_SNAPSHOT_RESTORE;
        if (existIndexes.contains(nextIndex) && !indexes.contains(nextIndex)) {
            indexes.add(nextIndex);
        }
        if (existIndexes.contains(nextSnapshotIndex) && !indexes.contains(nextSnapshotIndex)) {
            indexes.add(nextSnapshotIndex);
        }
        return indexes.toArray(new String[0]);
    }

    private BoolQueryBuilder getQueryBuilder(LogQuery logQuery) {
        //日志时间范围查询设置
        QueryBuilder timeFilter = QueryBuilders.rangeQuery("@timestamp").from(logQuery.getLogDateStart())
                .to(logQuery.getLogDateEnd());
        BoolQueryBuilder queryBuilder = QueryBuilders.boolQuery().filter(timeFilter)
                .filter(QueryBuilders.matchPhraseQuery("k8s_pod_namespace", logQuery.getNamespace()));
        queryBuilder.filter(QueryBuilders.matchPhraseQuery("middleware_name", logQuery.getMiddlewareName()));
        if (StringUtils.isNotBlank(logQuery.getContainer())) {
            queryBuilder.filter(QueryBuilders.matchPhraseQuery("k8s_container_name", logQuery.getContainer()));
        }
        if (StringUtils.isNotBlank(logQuery.getLogPath())) {
            if (logQuery.getLogPath().contains(SLASH)) {
                String logpath = logQuery.getLogPath().substring(logQuery.getLogPath().lastIndexOf(SLASH) + 1);
                queryBuilder.must(QueryBuilders.wildcardQuery("source", "*" + logpath + "*"));
            } else {
                queryBuilder.must(QueryBuilders.wildcardQuery("source", "*" + logQuery.getLogPath() + "*"));
            }

        }
        if (StringUtils.isNotBlank(logQuery.getSearchWord())) {
            String keyWord = logQuery.getSearchWord().trim().toLowerCase();
            //日志内容关键字查询
            if (EsSearchTypeEnum.MATCH.getCode().equalsIgnoreCase(logQuery.getSearchType())) {
                queryBuilder = queryBuilder.must(QueryBuilders.matchQuery("message", keyWord));
            } else if (EsSearchTypeEnum.MATCH_PHRASE.getCode().equalsIgnoreCase(logQuery.getSearchType())) {
                queryBuilder = queryBuilder.must(QueryBuilders.matchPhraseQuery("message", keyWord));
            } else if (EsSearchTypeEnum.WILDCARD.getCode().equalsIgnoreCase(logQuery.getSearchType())) {
                keyWord = logQuery.getSearchWord();
                //模糊查询如果参数没有*，添加*进行模糊匹配
                if (!keyWord.contains(STAR)) {
                    keyWord = STAR + keyWord + STAR;
                }
                queryBuilder = queryBuilder.must(QueryBuilders.wildcardQuery("message", keyWord));
            } else if (EsSearchTypeEnum.REGEXP.getCode().equalsIgnoreCase(logQuery.getSearchType())) {
                queryBuilder = queryBuilder.must(QueryBuilders.regexpQuery("message", keyWord));
            }
        }
        return queryBuilder;
    }

    private BoolQueryBuilder getQueryBuilderByLine(LogQuery logQuery, String upOrdown, String timestamp) {
        //日志行数范围查询设置
        QueryBuilder timeFilter = null;
        QueryBuilder offsetFilter = null;
        if (StringUtils.equals(UP, upOrdown)) {
            timeFilter = QueryBuilders.rangeQuery("@timestamp").to(timestamp);
            offsetFilter = QueryBuilders.rangeQuery("offset").to(logQuery.getOffset());
        } else {
            timeFilter = QueryBuilders.rangeQuery("@timestamp").from(timestamp);
            offsetFilter = QueryBuilders.rangeQuery("offset").from(logQuery.getOffset());
        }
        BoolQueryBuilder queryBuilder = QueryBuilders.boolQuery().filter(timeFilter).filter(offsetFilter)
                .filter(QueryBuilders.matchPhraseQuery("k8s_pod_namespace", logQuery.getNamespace()));
        queryBuilder.filter(QueryBuilders.matchPhraseQuery("middleware_name", logQuery.getMiddlewareName()));
        if (StringUtils.isNotBlank(logQuery.getContainer())) {
            queryBuilder.filter(QueryBuilders.matchPhraseQuery("k8s_container_name", logQuery.getContainer()));
        }
        if (StringUtils.isNotBlank(logQuery.getSearchWord())) {
            String keyWord = logQuery.getSearchWord().trim().toLowerCase();
            //日志内容关键字查询
            if (EsSearchTypeEnum.MATCH.getCode().equalsIgnoreCase(logQuery.getSearchType())) {
                queryBuilder = queryBuilder.must(QueryBuilders.matchQuery("message", keyWord));
            } else if (EsSearchTypeEnum.MATCH_PHRASE.getCode().equalsIgnoreCase(logQuery.getSearchType())) {
                queryBuilder = queryBuilder.must(QueryBuilders.matchPhraseQuery("message", keyWord));
            } else if (EsSearchTypeEnum.WILDCARD.getCode().equalsIgnoreCase(logQuery.getSearchType())) {
                //模糊查询如果参数没有*，添加*进行模糊匹配
                if (!keyWord.contains(STAR)) {
                    keyWord = STAR + keyWord + STAR;
                }
                queryBuilder = queryBuilder.must(QueryBuilders.wildcardQuery("message", keyWord));
            } else if (EsSearchTypeEnum.REGEXP.getCode().equalsIgnoreCase(logQuery.getSearchType())) {
                queryBuilder = queryBuilder.must(QueryBuilders.regexpQuery("message", keyWord));
            }
        }
        return queryBuilder;
    }

    private boolean isExceedMaxResult(ElasticsearchStatusException e) {
        if (e.getSuppressed() != null && e.getSuppressed().length > 0) {
            Throwable cause = e.getSuppressed()[0].getCause();
            if (cause != null && cause.getMessage() != null
                    && cause.getMessage().contains("max_result_window")) {
                return true;
            }
        }

        return false;
    }

}