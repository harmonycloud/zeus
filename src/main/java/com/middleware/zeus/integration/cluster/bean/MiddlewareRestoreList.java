package com.middleware.zeus.integration.cluster.bean;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

import static com.middleware.caas.common.constants.middleware.MiddlewareConstant.HARMONY_CLOUD_CN_V1;

/**
 * @author liyinlong
 * @since 2021/10/22 12:03 上午
 */
@Data
@Accessors(chain = true)
public class MiddlewareRestoreList {

    private String apiVersion = HARMONY_CLOUD_CN_V1;

    private String kind;

    private ObjectMeta metadata;

    private List<MiddlewareRestoreCR> items;


}
