package com.middleware.zeus.common.enums.middleware;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author dengyulong
 * @date 2021/04/01
 */
public enum StorageClassProvisionerEnum {

    /**
     * 存储服务以及是否支持中间件
     */
    HOST_PATH("HostPath", "kubernetes.io/no-provisioner"),
    NFS("NFS", "nfs-client-provisioner"),
    CSI_LVM("CSI-LVM", "localplugin.csi.alibabacloud.com"),
    LOCAL_PATH("LocalPath", "rancher.io/local-path"),
    JUICE_FS("juicefs", "csi.juicefs.com"),
    HITACHI("hitachi", "hspc.csi.hitachi.com"),
    ;
    
    private static final Map<String, StorageClassProvisionerEnum> PROVISIONER_MAP = new HashMap<>();
    private static final Map<String, StorageClassProvisionerEnum> TYPE_MAP = new HashMap<>();
    
    static {
        for (StorageClassProvisionerEnum provisionerEnum : StorageClassProvisionerEnum.values()) {
            PROVISIONER_MAP.put(provisionerEnum.getProvisioner(), provisionerEnum);
            TYPE_MAP.put(provisionerEnum.getType(), provisionerEnum);
        }
    }

    private final String type;
    private final String provisioner;

    
    public static StorageClassProvisionerEnum findByType(String type) {
        if (type == null) {
            return null;
        }
        return TYPE_MAP.get(type);
    }

    public static StorageClassProvisionerEnum findByProvisioner(String provisioner) {
        if (provisioner == null) {
            return null;
        }
        return provisioner.startsWith(NFS.provisioner) ? NFS : PROVISIONER_MAP.get(provisioner);
    }

    public static List<String> getDefaultSupportType() {
        List<String> list = new ArrayList<>();
        list.add(NFS.type);
        list.add(CSI_LVM.type);
        list.add(LOCAL_PATH.type);
        return list;
    }

    StorageClassProvisionerEnum(String type, String provisioner) {
        this.type = type;
        this.provisioner = provisioner;
    }

    public String getType() {
        return type;
    }

    public String getProvisioner() {
        return provisioner;
    }

}
