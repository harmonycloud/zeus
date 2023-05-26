package com.middleware.zeus.socket;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.middleware.zeus.common.enums.middleware.ResourceUnitEnum;
import com.middleware.zeus.service.k8s.MaintenanceService;
import com.middleware.zeus.service.k8s.PvcService;
import com.middleware.zeus.service.middleware.MiddlewarePvcService;
import com.middleware.zeus.util.numeric.ResourceCalculationUtil;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaim;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaimCondition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.middleware.zeus.common.model.EventDetail;

import lombok.extern.slf4j.Slf4j;

import static com.middleware.zeus.common.constants.NameConstant.*;

/**
 * @author xutianhong
 * @Date 2023/1/12 10:30 上午
 */
@Slf4j
public class PvcScaleSocketHandler extends TextWebSocketHandler {

    private final MiddlewarePvcService middlewarePvcService;
    private final PvcService pvcService;
    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);

    @Autowired
    public PvcScaleSocketHandler(MiddlewarePvcService middlewarePvcService, PvcService pvcService){
        this.middlewarePvcService = middlewarePvcService;
        this.pvcService = pvcService;
    }


    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        Map<String, String> messageMap = getMessageMap(message);
        log.info("处理ws文本消息：{}", messageMap);
        String action = messageMap.get("action");
        if ("list pvc event".equals(action)) {
            String clusterId = (String)session.getAttributes().get("clusterId");
            String namespace = (String)session.getAttributes().get("namespace");
            String middlewareName = (String)session.getAttributes().get("middlewareName");
            String pvcName = (String)session.getAttributes().get("pvcName");

            executor.scheduleWithFixedDelay(() -> {
                List<EventDetail> eventDetails =
                    middlewarePvcService.getEvent(clusterId, namespace, middlewareName, pvcName);
                List<String> text = eventDetails.stream().map(EventDetail::getMessage).collect(Collectors.toList());



                List<String> statusMessage = new ArrayList<>();
                PersistentVolumeClaim pvc = pvcService.get(clusterId, namespace, pvcName);
                if (pvc.getStatus() == null || pvc.getStatus().getCapacity() == null || pvc.getStatus().getCapacity().containsKey(STORAGE)){
                    statusMessage.add("pvc status error");
                }else {
                    Double request = ResourceCalculationUtil.getResourceValue(pvc.getSpec().getResources().getRequests().get(STORAGE).getAmount(), MEMORY, ResourceUnitEnum.GI.getUnit());
                    Double used = ResourceCalculationUtil.getResourceValue(pvc.getStatus().getCapacity().get(STORAGE).getAmount(), MEMORY, ResourceUnitEnum.GI.getUnit());;
                    if (request.equals(used)){
                        statusMessage.add("scale succeed");
                    }
                }
                // 发送event
                sendMessage(text, session);
                if(!CollectionUtils.isEmpty(statusMessage)){
                    // 发送状态
                    sendMessage(statusMessage, session);
                    executor.shutdown();
                }
            }, 0, 2000, TimeUnit.MILLISECONDS);
        }
    }

    public void sendMessage(List<String> text, WebSocketSession session) {
        try {
            Map<String, Object> map = new HashMap<>();
            map.put("type", "TERMINAL_PRINT");
            map.put("text", text);

            String message = new ObjectMapper().writeValueAsString(map);
            session.sendMessage(new TextMessage(message));
        } catch (Exception e){
            log.error("发送信息失败,信息内容:{}", text);
        }
    }

    private Map<String, String> getMessageMap(TextMessage message) {
        try {
            Map<String, String> map =
                    new ObjectMapper().readValue(message.getPayload(), new TypeReference<Map<String, String>>() {});

            return map;
        } catch (IOException e) {
            log.warn("getMessageMap失败", e);
        }
        return new HashMap<>();
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        super.handleTransportError(session, exception);
        // stop
        executor.shutdown();
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        super.afterConnectionClosed(session, status);
        // stop
        executor.shutdown();
    }

}
