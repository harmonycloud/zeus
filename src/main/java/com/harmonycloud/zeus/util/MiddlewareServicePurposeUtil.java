package com.harmonycloud.zeus.util;

import com.harmonycloud.caas.common.model.middleware.IngressDTO;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author liyinlong
 * @since 2022/7/14 2:14 下午
 */
public class MiddlewareServicePurposeUtil {

    public static String convertChinesePurpose(IngressDTO ingressDTO) {
        String middlewareType = ingressDTO.getMiddlewareType();
        String middlewareName = ingressDTO.getMiddlewareName();
        String serviceName = ingressDTO.getName();
        if (middlewareType == null) {
            return "";
        }
        if (serviceName.contains("tcp")) {
            serviceName = ingressDTO.getServiceList().get(0).getServiceName();
        }
        if (serviceName.contains("nodeport")) {
            serviceName = serviceName.split("-")[0];
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
            sbf.append(getPurpose(middlewareName, middlewareType, svcName)).append(",");
        });
        return sbf.substring(0, sbf.length() - 1);
    }

    private static String getPurpose(String middlewareName, String middlewareType, String serviceName) {
        switch (middlewareType) {
            case "mysql":
                return convertMysql(middlewareName, serviceName);
            case "redis":
                return convertRedis(middlewareName, serviceName);
            case "elasticsearch":
                return convertEs(middlewareName, serviceName);
            case "postgresql":
                return convertPgSQL(middlewareName, serviceName);
            case "rocketmq":
                return convertRocketMQ(middlewareName, serviceName);
            case "kafka":
                return convertKafka(middlewareName, serviceName);
            case "zookeeper":
                return convertZookeeper(middlewareName, serviceName);
            default:
                return "/";
        }
    }

    public static String convertChinesePurpose(String middlewareName, String type, String serviceName) {
        IngressDTO ingressDTO = new IngressDTO();
        ingressDTO.setMiddlewareType(type);
        ingressDTO.setName(serviceName);
        ingressDTO.setMiddlewareName(middlewareName);
        return convertChinesePurpose(ingressDTO);
    }

    public static String convertMysql(String middlewareName, String serviceName) {
        if (serviceName.contains("readonly")) {
            return "只读";
        } else if (serviceName.contains("proxy")) {
            return "读写分离";
        } else if (serviceName.equals(middlewareName)) {
            return "读写";
        } else {
            return null;
        }
    }

    public static String convertRedis(String middlewareName, String serviceName) {
        if (serviceName.contains("readonly")) {
            return "只读";
        } else if (serviceName.contains("predixy")) {
            return "读写";
        } else if (serviceName.equals(middlewareName)) {
            return "读写";
        } else {
            return null;
        }
    }

    public static String convertEs(String middlewareName, String serviceName) {
        if (serviceName.contains("master") || serviceName.contains("data") || serviceName.contains("client")) {
            return "读写";
        } else if (serviceName.contains("kibana")) {
            return "管理页面";
        } else {
            return null;
        }
    }

    public static String convertPgSQL(String middlewareName, String serviceName) {
        if (serviceName.contains("repl")) {
            return "只读";
        } else if (serviceName.equals(middlewareName)) {
            return "读写";
        } else {
            return null;
        }
    }

    public static String convertRocketMQ(String middlewareName, String serviceName) {
        if (serviceName.contains("console-svc")) {
            return "管理页面";
        } else if (serviceName.contains("proxy")) {
            return "服务代理";
        } else if (serviceName.contains("namesrv")) {
            return "服务连接";
        } else {
            return null;
        }
    }

    public static String convertKafka(String middlewareName, String serviceName) {
        if (serviceName.contains("manager-svc")) {
            return "管理页面";
        } else if (serviceName.contains("broker")) {
            return "服务连接";
        } else {
            return null;
        }
    }

    public static String convertZookeeper(String middlewareName, String serviceName) {
        if (serviceName.contains("client")) {
            return "服务连接";
        } else {
            return null;
        }
    }

    public static String cutString(String str, String start, String end) {
        if (StringUtils.isBlank(str)) {
            return str;
        }
        String reg = start + "(.*)" + end;
        Pattern pattern = Pattern.compile(reg);
        Matcher matcher = pattern.matcher(str);
        while (matcher.find()) {
            str = matcher.group(1);
        }
        return str;
    }



}
