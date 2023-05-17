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
        }
        return servicePortDTOList;
    }

    private static List<ServicePortDTO> filterEs(String mode, List<ServicePortDTO> servicePortDTOList) {
        String keyword;
        switch (mode) {
            case "complex":
            case "complex-cold":
            case "cold-complex":
                keyword = "client";
                break;
            case "regular":
                keyword = "data";
                break;
            default:
                keyword = "master";
        }
        return servicePortDTOList.stream().filter(servicePortDTO -> servicePortDTO.getServiceName().endsWith(keyword)).collect(Collectors.toList());
    }

}
