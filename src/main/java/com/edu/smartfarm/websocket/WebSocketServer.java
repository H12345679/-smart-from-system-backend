package com.edu.smartfarm.websocket;

import jakarta.websocket.*;
import jakarta.websocket.server.ServerEndpoint;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket服务端 — 向前端推送实时传感器数据和报警
 * 连接地址: ws://localhost:8080/api/v1/ws/monitor
 */
@Slf4j
@Component
@ServerEndpoint("/ws/monitor")
public class WebSocketServer {

    private static final ConcurrentHashMap<String, Session> sessions = new ConcurrentHashMap<>();

    @OnOpen
    public void onOpen(Session session) {
        sessions.put(session.getId(), session);
        log.info("WebSocket连接建立, sessionId={}, 当前在线数={}", session.getId(), sessions.size());
    }

    @OnClose
    public void onClose(Session session) {
        sessions.remove(session.getId());
        log.info("WebSocket连接关闭, sessionId={}, 当前在线数={}", session.getId(), sessions.size());
    }

    @OnError
    public void onError(Session session, Throwable error) {
        log.error("WebSocket错误, sessionId={}: {}", session.getId(), error.getMessage());
        sessions.remove(session.getId());
    }

    @OnMessage
    public void onMessage(String message, Session session) {
        // 客户端心跳
        if ("ping".equals(message)) {
            try {
                session.getBasicRemote().sendText("pong");
            } catch (IOException e) {
                log.error("发送pong失败");
            }
        }
    }

    /**
     * 向所有在线客户端推送消息
     */
    public void sendToAll(String message) {
        sessions.forEach((id, session) -> {
            try {
                if (session.isOpen()) {
                    session.getBasicRemote().sendText(message);
                }
            } catch (IOException e) {
                log.error("WebSocket推送失败: {}", e.getMessage());
            }
        });
    }
}
