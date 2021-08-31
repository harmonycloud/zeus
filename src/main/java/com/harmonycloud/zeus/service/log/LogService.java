package com.harmonycloud.zeus.service.log;

import com.harmonycloud.caas.common.base.BaseResult;
import com.harmonycloud.caas.common.model.middleware.LogQuery;
import com.harmonycloud.caas.common.model.middleware.LogQueryDto;

import javax.servlet.http.HttpServletResponse;

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
     * 日志文件
     */
    String LOG_TYPE_LOGFILE = "filelog";


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

}
