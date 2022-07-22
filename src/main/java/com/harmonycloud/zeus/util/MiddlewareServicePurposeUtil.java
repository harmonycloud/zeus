package com.harmonycloud.zeus.util;

import com.harmonycloud.caas.common.model.middleware.IngressDTO;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * @author liyinlong
 * @since 2022/7/14 2:14 下午
 */
public class MiddlewareServicePurposeUtil {

    public static String convertChinesePurpose(IngressDTO ingressDTO) {
        String middlewareType = ingressDTO.getMiddlewareType();
        String serviceName = ingressDTO.getName();
        if (middlewareType == null) {
            return "";
        }
        List<String> serviceNameList = new ArrayList<>();
        if (!CollectionUtils.isEmpty(ingressDTO.getRules())) {
            ingressDTO.getRules().forEach(ingressRuleDTO -> {
                ingressRuleDTO.getIngressHttpPaths().forEach(ingressHttpPath -> {
                    serviceNameList.add(ingressHttpPath.getServiceName());
                });
            });
        } else {
            serviceNameList.add(serviceName);
        }
        StringBuffer sbf = new StringBuffer();
        serviceNameList.forEach(svcName -> {
            sbf.append(getPurpose(middlewareType, svcName)).append(",");
        });
        return sbf.substring(0, sbf.length() - 1);
    }

    private static String getPurpose(String middlewareType, String serviceName) {
        switch (middlewareType) {
            case "mysql":
                return convertMysql(serviceName);
            case "redis":
                return convertRedis(serviceName);
            case "elasticsearch":
                return convertEs(serviceName);
            case "postgresql":
                return convertPgSQL(serviceName);
            case "rocketmq":
                return convertRocketMQ(serviceName);
            case "kafka":
                return convertKafka(serviceName);
            case "zookeeper":
                return convertZookeeper(serviceName);
            default:
                return "/";
        }
    }

    public static String convertChinesePurpose(String type, String serviceName) {
        IngressDTO ingressDTO = new IngressDTO();
        ingressDTO.setMiddlewareType(type);
        ingressDTO.setName(serviceName);
        return convertChinesePurpose(ingressDTO);
    }

    public static String convertMysql(String serviceName) {
        if (serviceName.contains("readonly")) {
            return "只读";
        } else if (serviceName.contains("proxy")) {
            return "读写分离";
        } else {
            return "读写";
        }
    }

    public static String convertRedis(String serviceName) {
        if (serviceName.contains("readonly")) {
            return "只读";
        } else if (serviceName.contains("predixy")) {
            return "读写";
        } else {
            return "读写";
        }
    }

    public static String convertEs(String serviceName) {
        if (serviceName.contains("master") || serviceName.contains("data") || serviceName.contains("client")) {
            return "读写";
        } else if (serviceName.contains("kibana")) {
            return "管理页面";
        } else {
            return "/";
        }
    }

    public static String convertPgSQL(String serviceName) {
        if (serviceName.contains("repl")) {
            return "只读";
        } else {
            return "读写";
        }
    }

    public static String convertRocketMQ(String serviceName) {
        if (serviceName.contains("console-svc")) {
            return "管理页面";
        } else if (serviceName.contains("proxy")) {
            return "服务代理";
        } else {
            return "服务连接";
        }
    }

    public static String convertKafka(String serviceName) {
        if (serviceName.contains("manager-svc")) {
            return "管理页面";
        } else {
            return "服务连接";
        }
    }

    public static String convertZookeeper(String serviceName) {
        if (serviceName.contains("client")) {
            return "服务连接";
        } else {
            return "/";
        }
    }

}
