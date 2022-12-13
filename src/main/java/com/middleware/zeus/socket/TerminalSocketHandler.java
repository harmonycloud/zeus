package com.middleware.zeus.socket;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.middleware.zeus.service.log.LogService;
import com.middleware.zeus.socket.term.TerminalService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.middleware.caas.common.constants.NameConstant;
import com.middleware.caas.common.model.middleware.LogQueryDto;
import com.harmonycloud.caas.filters.user.CurrentUser;
import com.harmonycloud.caas.filters.user.CurrentUserRepository;

import lombok.extern.slf4j.Slf4j;

/**
 * @author dengyulong
 * @date 2021/05/19
 */
@Slf4j
public class TerminalSocketHandler extends TextWebSocketHandler {

    private final TerminalService terminalService;

    @Autowired
    public TerminalSocketHandler(TerminalService terminalService) {
        this.terminalService = terminalService;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        CurrentUser user = CurrentUserRepository.getUser();
        // 名称给进去，在刷新页面情况下中断连接时，CurrentUserRepository.getUser()会取不到用户
        session.getAttributes().put(NameConstant.USER, user);
        terminalService.setWebSocketSession(session);
        super.afterConnectionEstablished(session);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        Map<String, String> messageMap = getMessageMap(message);
        log.info("处理ws文本消息：{}", messageMap);
        if (messageMap.containsKey("type")) {
            String type = messageMap.get("type");
            Object terminalType = session.getAttributes().get("terminalType");
            String clusterId = (String)session.getAttributes().get("clusterId");
            switch (type) {
                case "TERMINAL_INIT":
                    log.info("终端初始化");
                    terminalService.onTerminalInit();
                    break;
                case "TERMINAL_READY":
                    log.info("终端已准备");
                    if (terminalType != null) {
                        log.info("terminal type:{}", terminalType.toString());
                        String pod = (String) session.getAttributes().get("pod");
                        String namespace = (String) session.getAttributes().get("namespace");
                        String container = (String) session.getAttributes().get("container");
                        LogQueryDto logQueryDto = new LogQueryDto();
                        logQueryDto.setPod(pod);
                        logQueryDto.setNamespace(namespace);
                        logQueryDto.setContainer(container);
                        logQueryDto.setClusterId(clusterId);
                        if (terminalType.toString().equalsIgnoreCase("stdoutlog")) {
                            log.info("处理标准输出日志");
                            logQueryDto.setLogSource(LogService.LOG_TYPE_STDOUT);
                            terminalService.onLogTerminalReady(logQueryDto);
                        } else if (terminalType.toString().equals("filelog")) {
                            log.info("处理文件日志");
                            String logDir = (String) session.getAttributes().get("logDir");
                            logDir = logDir.substring(0, logDir.lastIndexOf("/"));
                            String logFile = (String) session.getAttributes().get("logFile");
                            logQueryDto.setLogDir(logDir);
                            logQueryDto.setLogFile(logFile);
                            logQueryDto.setLogSource(LogService.LOG_TYPE_LOGFILE);
                            terminalService.onLogTerminalReady(logQueryDto);
                        } else if (terminalType.toString().equals("console")) {
                            log.info("处理终端控制台命令");
                            String scriptType = session.getAttributes().get("scriptType").toString();
                            if (StringUtils.isBlank(scriptType)) {
                                scriptType = "sh";
                            }
                            log.info(String.format("进入控制台，容器名称:%s,pod名称:%s,namespace名称:%s,shell类型:%s", container, pod,
                                    namespace, scriptType));
                            String middlewareName = null;
                            String middlewareType = null;
                            if (session.getAttributes().containsKey("middlewareName") && session.getAttributes().containsKey("middlewareType")){
                                middlewareName = session.getAttributes().get("middlewareName").toString();
                                middlewareType = session.getAttributes().get("middlewareType").toString();

                            }
                            terminalService.onTerminalReady(container, pod, namespace, clusterId, scriptType, middlewareName, middlewareType);
                        }else if(terminalType.toString().equals("previousLog")){
                            log.info("处理上一次日志");
                            logQueryDto.setLogSource(LogService.LOG_TYPE_PREVIOUS_LOG);
                            terminalService.onLogTerminalReady(logQueryDto);
                        }
                    } else {
                        log.info("terminal type is null, enter pod terminal");
                        String scriptType = session.getAttributes().get("scriptType").toString();
                        String container = session.getAttributes().get("container").toString();
                        String pod = session.getAttributes().get("pod").toString();
                        String namespace = session.getAttributes().get("namespace").toString();
                        log.info(String.format("进入控制台，容器名称:%s,pod名称:%s,namespace名称:%s,shell类型:%s", container, pod,
                                namespace, scriptType));
                        terminalService.onTerminalReady(container, pod, namespace, clusterId, scriptType, null, null);
                    }
                    break;
                case "TERMINAL_COMMAND":
                    terminalService.onCommand(messageMap.get("command"));
                    break;
                case "TERMINAL_RESIZE":
                    terminalService.onTerminalResize(messageMap.get("columns"), messageMap.get("rows"));
                    break;
                default:
                    throw new RuntimeException("Unrecodnized action");
            }
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
        terminalService.onTerminalClosed(session);
        terminalService.deleteConsoleProcess(session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        super.afterConnectionClosed(session, status);
        terminalService.onTerminalClosed(session);
        terminalService.deleteConsoleProcess(session);
    }

    @Override
    public boolean supportsPartialMessages() {
        return super.supportsPartialMessages();
    }
}
