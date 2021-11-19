package com.harmonycloud.zeus.integration.cluster.bean;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * @author xutianhong
 * @Date 2021/4/2 5:58 下午
 */
@Accessors(chain = true)
@Data
public class ScheduleBackupList {

    private String apiVersion;

    private String kind;

    private ObjectMeta metadata;

    private List<MysqlScheduleBackupCRD> items;

}
