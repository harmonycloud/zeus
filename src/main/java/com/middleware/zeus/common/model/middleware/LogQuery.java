package com.middleware.zeus.common.model.middleware;


import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author zhangkui
 * @date 2017/3/31
 * 日志查询参数对象
 */
@Accessors(chain = true)
@Data
public class LogQuery {

    private String logDateStart;
    private String logDateEnd;
    private String namespace;
    private String appName;
    private String middlewareName;
    private String appType;
    private String container;
    private String logDir;
    private String pod;
    private String clusterId;
    /**
     * Elasticsearch分页id
     */
    private String scrollId;
    /**
     * 日志级别 ，I-info,E-error,W-warn
     */
    private String severity;
    /**
     * 日志内容查询关键字
     */
    private String searchWord;

    private Integer pageSize;

    private String searchType;

    private String[] indexes;

    private int offset;

    private boolean podLog;

    private String logPath;

    private String middlewareType;

}
