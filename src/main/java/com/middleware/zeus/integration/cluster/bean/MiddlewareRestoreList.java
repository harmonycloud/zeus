package com.middleware.zeus.integration.cluster.bean;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

import static com.middleware.caas.common.constants.middleware.MiddlewareConstant.CR_API_VERSION;

/**
 * @author liyinlong
 * @since 2021/10/22 12:03 上午
 */
@Data
@Accessors(chain = true)
public class MiddlewareRestoreList {

    private String apiVersion = CR_API_VERSION;

    private String kind;

    private ObjectMeta metadata;

    private List<MiddlewareRestoreCR> items;


}
