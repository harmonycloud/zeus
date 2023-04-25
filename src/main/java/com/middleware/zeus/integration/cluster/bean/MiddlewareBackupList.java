package com.middleware.zeus.integration.cluster.bean;

import io.fabric8.kubernetes.api.model.DefaultKubernetesResourceList;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

import static com.middleware.caas.common.constants.middleware.MiddlewareConstant.CR_API_VERSION;

/**
 * 中间件备份记录list
 * @author  liyinlong
 * @since 2021/9/13 4:33 下午
 */
@Accessors(chain = true)
public class MiddlewareBackupList extends DefaultKubernetesResourceList<MiddlewareBackupCR> {

}