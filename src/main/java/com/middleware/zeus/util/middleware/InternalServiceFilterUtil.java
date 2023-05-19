package com.middleware.zeus.util.middleware;

import com.middleware.zeus.common.enums.middleware.MiddlewareTypeEnum;
import com.middleware.zeus.common.model.middleware.ServicePortDTO;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author liyinlong
 * @since 2023/5/16 5:36 下午
 */
public class InternalServiceFilterUtil {

    public static List<ServicePortDTO> filterUnused(String type, String mode, List<ServicePortDTO> servicePortDTOList) {
        if (MiddlewareTypeEnum.ELASTIC_SEARCH.getType().equals(type)) {
            return filterEs(mode, servicePortDTOList);
        } else if (MiddlewareTypeEnum.REDIS.getType().equals(type)) {
            return filterRedis(mode, servicePortDTOList);
        } else if (MiddlewareTypeEnum.POSTGRESQL.getType().equals(type)) {
            return filterPG(mode, servicePortDTOList);
        }
        return servicePortDTOList;
    }

    private static List<ServicePortDTO> filterEs(String mode, List<ServicePortDTO> servicePortDTOList) {
        String keyword;
        switch (mode) {
            case "complex":
            case "cold-complex":
                keyword = "client";
                break;
            case "complex-cold":
            case "regular":
                keyword = "data";
                break;
            default:
                keyword = "master";
        }
        return servicePortDTOList.stream().filter(servicePortDTO -> servicePortDTO.getServiceName().endsWith(keyword)
                || servicePortDTO.getServiceName().endsWith("kibana")).collect(Collectors.toList());
    }

    private static List<ServicePortDTO> filterRedis(String mode, List<ServicePortDTO> servicePortDTOList) {
        boolean proxy = false;
        for (ServicePortDTO servicePortDTO : servicePortDTOList) {
            if (servicePortDTO.getServiceName().endsWith("predixy")) {
                proxy = true;
                break;
            }
        }
        if (proxy) {
            return servicePortDTOList.stream().filter(servicePortDTO -> servicePortDTO.getServiceName().endsWith("predixy")
            ).collect(Collectors.toList());
        }
        return servicePortDTOList;
    }

    private static List<ServicePortDTO> filterPG(String mode, List<ServicePortDTO> servicePortDTOList) {
        if ("1m-0s".equals(mode)) {
            return servicePortDTOList.stream().filter(servicePortDTO ->
                    !servicePortDTO.getServiceName().endsWith("patroni") &&
                            !servicePortDTO.getServiceName().endsWith("repl") &&
                            !servicePortDTO.getServiceName().endsWith("svc-metrics")
            ).collect(Collectors.toList());
        }
        return servicePortDTOList;
    }

}
