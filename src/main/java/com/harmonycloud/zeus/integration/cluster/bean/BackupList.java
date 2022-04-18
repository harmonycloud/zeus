package com.harmonycloud.zeus.integration.cluster.bean;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * @author xutianhong
 * @Date 2021/4/6 4:50 下午
 */
@Accessors(chain = true)
@Data
public class BackupList {
    private String apiVersion;

    private String kind;

    private ObjectMeta metadata;

    private List<BackupCR> items;
}
