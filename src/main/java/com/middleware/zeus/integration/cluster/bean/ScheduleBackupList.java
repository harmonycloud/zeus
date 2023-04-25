package com.middleware.zeus.integration.cluster.bean;

import io.fabric8.kubernetes.api.model.DefaultKubernetesResourceList;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * @author xutianhong
 * @Date 2021/4/2 5:58 下午
 */
@Accessors(chain = true)
public class ScheduleBackupList extends DefaultKubernetesResourceList<MysqlScheduleBackupCR> {

}
