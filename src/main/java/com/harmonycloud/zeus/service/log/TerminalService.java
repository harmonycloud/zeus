package com.harmonycloud.zeus.service.log;

import org.springframework.web.socket.WebSocketSession;

/**
 * @author dengyulong
 * @date 2021/05/19
 */
public interface TerminalService {

    /**
     * 删除控制台进程
     *
     * @param session websocket session
     */
    void deleteConsoleProcess(WebSocketSession session);
}
