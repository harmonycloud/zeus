package com.harmonycloud.zeus.util;

import com.harmonycloud.caas.common.enums.middleware.MiddlewareTypeEnum;
import com.harmonycloud.caas.common.model.MiddlewareServiceNameIndex;
import com.harmonycloud.caas.common.model.middleware.Middleware;

/**
 * 中间件服务名称转换工具类
 * @author liyinlong
 * @since 2021/9/8 4:09 下午
 */
public class ServiceNameConvertUtil {

    /**
     * 转换mysql服务名称
     * @param isReadonlyService
     * @param middlewareName
     * @return
     */
    public static MiddlewareServiceNameIndex convertMysql(String middlewareName, Boolean isReadonlyService) {
        String nodePortServiceName;
        String middlewareServiceNameSuffix;
        if (isReadonlyService) {
            nodePortServiceName = String.format("%s-readonly-nodeport", middlewareName);
            middlewareServiceNameSuffix = "readonly";
        } else {
            nodePortServiceName = String.format("%s-nodeport", middlewareName);
            middlewareServiceNameSuffix = middlewareName;
        }
        return new MiddlewareServiceNameIndex(nodePortServiceName, middlewareServiceNameSuffix);
    }

    /**
     * 转换es服务名称
     * @param middlewareName
     * @return
     */
    public static MiddlewareServiceNameIndex convertEs(String middlewareName){
        return new MiddlewareServiceNameIndex(middlewareName + "-kibana-nodeport", "-kibana");
    }

    /**
     * 转换rocket-mq服务名称
     * @param middlewareName
     * @return
     */
    public static MiddlewareServiceNameIndex convertMq(String middlewareName) {
        return new MiddlewareServiceNameIndex(middlewareName + "-console-svc-nodeport", "console-svc");
    }

    /**
     * 转换kakfa服务名称
     * @return
     */
    public static MiddlewareServiceNameIndex convertKafka(String middlewareName){
        return new MiddlewareServiceNameIndex(middlewareName + "-manager-svc-nodeport", "kafka-manager-svc");
    }

    /**
     * 根据中间件类型返回服务名称
     *
     * @param middleware
     * @return
     */
    public static MiddlewareServiceNameIndex convert(Middleware middleware) {
        String middlewareType = middleware.getType();
        String middlewareName = middleware.getName();
        if (middlewareType.equals(MiddlewareTypeEnum.ELASTIC_SEARCH.getType())) {
            return convertEs(middlewareName);
        } else if (middlewareType.equals(MiddlewareTypeEnum.KAFKA.getType())) {
            return convertKafka(middlewareName);
        } else if (middlewareType.equals(MiddlewareTypeEnum.ROCKET_MQ.getType())) {
            return convertMq(middlewareName);
        } else {
            return null;
        }
    }

    /**
     * 获取web管理控制台服务端口
     * @param type 中间件类型
     * @return
     */
    public static String getManagePlatformServicePort(String type) {
        if (type.equals(MiddlewareTypeEnum.ELASTIC_SEARCH.getType())) {
            return "5200";
        } else if (type.equals(MiddlewareTypeEnum.KAFKA.getType())) {
            return "9000";
        } else if (type.equals(MiddlewareTypeEnum.ROCKET_MQ.getType())) {
            return "8080";
        } else {
            return null;
        }
    }
}
