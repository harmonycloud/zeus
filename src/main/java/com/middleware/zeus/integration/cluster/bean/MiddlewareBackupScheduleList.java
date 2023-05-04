package com.middleware.zeus.integration.cluster.bean;

import io.fabric8.kubernetes.api.model.DefaultKubernetesResourceList;
import lombok.experimental.Accessors;

/**
 * @author liyinlong
 * @since 2021/10/21 11:43 下午
 */
@Accessors(chain = true)
public class MiddlewareBackupScheduleList extends DefaultKubernetesResourceList<MiddlewareBackupSchedule> {

}
