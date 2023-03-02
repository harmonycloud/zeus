package com.middleware.zeus.util;

/**
 * @author xutianhong
 * @Date 2023/1/16 1:53 下午
 */
public class PrometheusQueryUtil {

    public static String queryHitachiFree(String storageClass, String serialId, String poolId){
        return String.format("sum(spc_sc_pool_free_capacity{storageclass=\"%s\",serial_id=\"%s\",pool_id=\"%s\"}) / 1024", storageClass, serialId, poolId);
    }

    public static String queryHitachiPodTotal(String serialId, String poolId, String namespace, String pvc) {
        return String.format(
            "sum(spc_volume_total_capacity{namespace=\"%s\",persistentvolumeclaim=~\"%s\"}) by (persistentvolumeclaim) /1024/1024/1024 ",
            namespace, pvc);
    }

    public static String queryHitachiPodUsed(String serialId, String poolId, String namespace, String pvc) {
        return String.format(
            "sum(spc_volume_used_capacity{namespace=\"%s\",persistentvolumeclaim=~\"%s\"}) by (persistentvolumeclaim) /1024/1024/1024",
            namespace, pvc);
    }

}
