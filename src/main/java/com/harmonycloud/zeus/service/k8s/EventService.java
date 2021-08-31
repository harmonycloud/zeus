package com.harmonycloud.zeus.service.k8s;

import com.harmonycloud.caas.common.model.EventDetail;

import java.util.List;

/**
 * @author xutianhong
 * @Date 2021/4/1 4:02 下午
 */
public interface EventService {

    /**
     * 获取集群events
     *
     * @param clusterId 集群id
     * @param namespace 命名空间
     * @return
     */
    List<EventDetail> getEvents(String clusterId, String namespace);

    /**
     * 获取集群events
     *
     * @param clusterId      集群id
     * @param namespace      命名空间
     * @param middlewareName 中间件名称
     * @param middlewareType 中间件类型
     * @param eventType      时间类型
     * @param kind           资源类型
     * @return
     */
    List<EventDetail> getEvents(String clusterId, String namespace, String middlewareName, String middlewareType,
                                String eventType, String kind);

}
