package com.middleware.zeus.integration.cluster.bean;

import io.fabric8.kubernetes.api.model.DefaultKubernetesResourceList;
import lombok.experimental.Accessors;

/**
 * 中间件备份记录list
 * @author  liyinlong
 * @since 2021/9/13 4:33 下午
 */
@Accessors(chain = true)
public class MiddlewareBackupList extends DefaultKubernetesResourceList<MiddlewareBackup> {

}