package com.middleware.zeus.service.k8s.impl;

import com.middleware.zeus.common.model.EventDetail;
import com.middleware.zeus.common.model.ObjectReference;
import com.middleware.zeus.integration.cluster.EventWrapper;
import com.middleware.zeus.integration.cluster.bean.MiddlewareCR;
import com.middleware.zeus.service.k8s.EventService;
import com.middleware.zeus.service.k8s.MiddlewareCRService;
import com.middleware.zeus.util.date.DateUtils;
import io.fabric8.kubernetes.api.model.events.v1.Event;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author xutianhong
 * @Date 2021/4/1 4:02 下午
 */
@Service
@Slf4j
public class EventServiceImpl implements EventService {

    @Autowired
    private EventWrapper eventWrapper;
    @Autowired
    private MiddlewareCRService middlewareCRService;

    @Override
    public List<EventDetail> getEvents(String clusterId, String namespace) {
        List<Event> eventList = eventWrapper.list(clusterId, namespace);
        return eventList.stream().map(this::convertEventDetail).collect(Collectors.toList());
    }

    @Override
    public List<EventDetail> getEvents(String clusterId, String namespace, String middlewareName, String middlewareType, 
        String eventType, String kind) {
        MiddlewareCR mw = middlewareCRService.getCR(clusterId, namespace, middlewareType, middlewareName);
        if (mw == null) {
            return new ArrayList<>(0);
        }
        if (ObjectUtils.isEmpty(mw.getStatus().getInclude())) {
            return new ArrayList<>(0);
        }
        Set<String> nameSet = new HashSet<>();
        nameSet.add(middlewareName);
        mw.getStatus().getInclude()
            .forEach((k, v) -> v.forEach(middlewareInfo -> nameSet.add(middlewareInfo.getName())));
        List<Event> events = eventWrapper.list(clusterId, namespace);
        // 类型转换，并且按照lastTimestamp降序
        return events.stream().filter(e -> {
            if (StringUtils.isNotBlank(eventType) && !eventType.equals(e.getType())) {
                return false;
            }

            if (StringUtils.isNotBlank(kind) && !kind.equals(e.getRegarding().getKind())) {
                return false;
            }
            // 过滤中间件信息
            return nameSet.contains(e.getRegarding().getName());
        }).map(this::convertEventDetail).sorted((e1, e2) -> {
            if (e1.getLastTimestamp().equals(e2.getLastTimestamp())) {
                return 0;
            }
            return e1.getLastTimestamp().after(e2.getLastTimestamp()) ? -1 : 1;
        }).collect(Collectors.toList());
    }

    @Override
    public List<EventDetail> getEvents(String clusterId, Map<String, String> fields) {
        List<Event> eventList = eventWrapper.listByFields(clusterId, fields);
        return eventList.stream().map(this::convertEventDetail).collect(Collectors.toList());
    }

    private EventDetail convertEventDetail(Event event) {
        EventDetail eventDetail = new EventDetail();
        BeanUtils.copyProperties(event, eventDetail, "eventTime", "firstTimestamp", "lastTimestamp");
        // 转换时间
        convertTime(event, eventDetail);

        ObjectReference objectReference = new ObjectReference();
        BeanUtils.copyProperties(event.getRegarding(), objectReference);
        eventDetail.setInvolvedObject(objectReference);
        return eventDetail;
    }

    private void convertTime(Event event, EventDetail eventDetail) {
        if (event.getEventTime() != null && StringUtils.isNotBlank(event.getEventTime().getTime())) {
            // 处理eventTime中包含微秒的情况，比如2020-04-17T03:14:18.992948Z
            if (event.getEventTime().getTime().contains(".")) {
                event.getEventTime().setTime(
                    event.getEventTime().getTime().substring(0, event.getEventTime().getTime().indexOf(".")) + "Z");
            }
            eventDetail.setEventTime(DateUtils.parseUTCDate(event.getEventTime().getTime()));
        }
        if (StringUtils.isNotBlank(event.getDeprecatedFirstTimestamp())) {
            eventDetail.setFirstTimestamp(DateUtils.parseUTCDate(event.getDeprecatedFirstTimestamp()));
        } else {
            eventDetail.setFirstTimestamp(eventDetail.getEventTime());
        }
        if (StringUtils.isNotBlank(event.getDeprecatedLastTimestamp())) {
            eventDetail.setLastTimestamp(DateUtils.parseUTCDate(event.getDeprecatedLastTimestamp()));
        } else {
            eventDetail.setLastTimestamp(eventDetail.getEventTime());
        }
    }

}
