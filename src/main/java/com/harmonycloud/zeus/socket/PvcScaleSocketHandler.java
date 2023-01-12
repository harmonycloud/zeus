package com.harmonycloud.zeus.socket;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.harmonycloud.caas.common.model.EventDetail;
import com.harmonycloud.zeus.service.middleware.MiddlewarePvcService;

import lombok.extern.slf4j.Slf4j;

/**
 * @author xutianhong
 * @Date 2023/1/12 10:30 上午
 */
@Slf4j
public class PvcScaleSocketHandler extends TextWebSocketHandler {

    private final MiddlewarePvcService middlewarePvcService;
    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);

    @Autowired
    public PvcScaleSocketHandler(MiddlewarePvcService middlewarePvcService){
        this.middlewarePvcService = middlewarePvcService;
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

                try {
                    log.info("发送信息");
                    sendMessage(text, session);
                } catch (IOException e) {
                    log.error("list pvc evevt 信息发送失败");
                }
            }, 0, 2000, TimeUnit.MILLISECONDS);
        }
    }

    public void sendMessage(List<String> text, WebSocketSession session) throws IOException {
        Map<String, Object> map = new HashMap<>();
        map.put("type", "TERMINAL_PRINT");
        map.put("text", text);

        String message = new ObjectMapper().writeValueAsString(map);
        session.sendMessage(new TextMessage(message));
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
        log.info("线程关闭");
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        super.afterConnectionClosed(session, status);
        // stop
        executor.shutdown();
        log.info("线程关闭");
    }

}
