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
public class LogQueryDto {

    private String namespace;
    private String container;
    private String appName;
    private String appType;
    private String middlewareName;
    /**
     * 查询近XXX时间对应的单位，有分，小时，天（m,h,d）. 与recentTimeNum一起使用
     * 例：查询近30分钟内的log（recentTimeUnit=m,recentTimeNum=30）
     *     查询近2天内的log（recentTimeUnit=d,recentTimeNum=2）
     */
    private String recentTimeUnit;
    /**
     * 查询近XXX时间对应的数字，与recentTimeUnit一起使用
     */
    private Integer recentTimeNum;
    /**
     * 绝对时间区间查询方式 日志开始时间
     * 宿主机时区format: yyyy-MM-dd hh:mm:ss / 零时区format: yyyy-MM-dd'T'HH:mm:ss'Z'
     */
    private String logTimeStart;
    /**
     * 绝对时间区间查询方式 日志结束时间
     */
    private String logTimeEnd;
    private String logDir;
    private String logFile;
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

    private String logSource;

    private String pod;

    private String clusterId;

    private String searchType;

    /**
     * 是否采集pod标准输出日志
     */
    private boolean podLog;

    private int offset;

    private String logPath;

    private String middlewareType;

}
