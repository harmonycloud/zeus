package com.middleware.zeus.service.log;

import com.middleware.zeus.common.base.BaseResult;
import com.middleware.zeus.common.model.middleware.LogQuery;
import com.middleware.zeus.common.model.middleware.LogQueryDto;
import com.middleware.zeus.common.model.middleware.MiddlewareLogQuery;
import com.middleware.zeus.common.model.middleware.MysqlLogDTO;
import com.middleware.zeus.util.page.PageObject;

import javax.servlet.http.HttpServletResponse;
import java.util.List;

/**
 * @author dengyulong
 * @date 2021/05/19
 * 日志Service接口
 */
public interface LogService {

    /**
     * 标准输出
     */
    String LOG_TYPE_STDOUT = "stdoutlog";

    /**
     * 文件日志
     */
    String LOG_TYPE_LOGFILE = "filelog";

    /**
     * 上一次日志
     */
    String LOG_TYPE_PREVIOUS_LOG = "previouslog";

    /**
     * 导出日志为txt文件
     * @author liyinlong
     * @date 2021/7/8 3:20 下午
     * @param logQuery 查询条件
     * @param response http响应
     * @throws Exception
     */
    void exportLog(LogQuery logQuery, HttpServletResponse response) throws Exception;

    /**
     * 从es查询日志
     * @author liyinlong
     * @date 2021/7/8 3:21 下午
     * @param logQuery
     * @return
     * @throws Exception
     */
    BaseResult getLogContents(LogQuery logQuery)throws Exception;

    /**
     * 从es查询文件列表
     * @author liyinlong
     * @date 2021/7/8 3:27 下午
     * @param logQuery
     * @return
     * @throws Exception
     */
    BaseResult listfileName(LogQuery logQuery) throws Exception;

    /**
     * 转换查询条件对象
     * @author liyinlong
     * @date 2021/7/8 4:08 下午
     * @param logQueryDto 查询条件对象
     * @return 查询条件
     * @throws Exception 查询条件转换失败异常
     */
    LogQuery transLogQuery(LogQueryDto logQueryDto) throws Exception;

    /**
     * 清理历史日志
     *
     * @throws Exception 查询条件转换失败异常
     */
    void cleanHistoryLog() throws Exception;

    /**
     * 清理历史日志
     *
     * @throws Exception 查询条件转换失败异常
     */
    PageObject<MysqlLogDTO> andit(MiddlewareLogQuery middlewareLogQuery) throws Exception;

    /**
     * 查询索引列信息
     * @param clusterId 集群id
     * @param namespace 命名空间
     * @param index 索引名称
     */
    List<String> getIndexColumnInfo(String clusterId, String namespace, String index);

}
