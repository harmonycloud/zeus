package com.middleware.zeus.common.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * @author liyinlong
 * @since 2021/11/3 1:53 下午
 */
@Data
@Accessors(chain = true)
@NoArgsConstructor
public class StorageClassDTO {

    private String storage;

    private String storageClassName;

    private Boolean isLvmStorage;

    private String provisioner;

    public StorageClassDTO(String storage, String storageClassName, Boolean isLvmStorage) {
        this.storage = storage;
        this.storageClassName = storageClassName;
        this.isLvmStorage = isLvmStorage;
    }
}
