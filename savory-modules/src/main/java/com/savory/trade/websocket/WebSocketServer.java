package com.savory.trade.websocket;

import jakarta.websocket.*;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket 服务
 * 用于实时推送新订单通知、催单提醒给商家端
 *
 * 路径: ws://host:8080/ws/{userId}
 */
@Component
@ServerEndpoint("/ws/{userId}")
@Slf4j
public class WebSocketServer {

    //存储在线连接: userId → Session
    private static final Map<Long, Session> sessionMap = new ConcurrentHashMap<>();

    @OnOpen
    public void onOpen(Session session, @PathParam("userId") Long userId) {
        sessionMap.put(userId, session);
        log.info("WebSocket连接建立: userId={}", userId);
    }

    @OnClose
    public void onClose(@PathParam("userId") Long userId) {
        sessionMap.remove(userId);
        log.info("WebSocket连接断开: userId={}", userId);
    }

    @OnError
    public void onError(Session session, Throwable error) {
        log.error("WebSocket错误: {}", error.getMessage());
    }

    @OnMessage
    public void onMessage(String message, Session session) {
        log.info("收到WebSocket消息: {}", message);
    }

    /**
     * 服务端推送消息到指定用户
     *
     * @param userId  目标用户ID
     * @param message 消息内容（JSON格式）
     */
    public static void sendMessage(Long userId, String message) {
        Session session = sessionMap.get(userId);
        if (session != null && session.isOpen()) {
            try {
                session.getBasicRemote().sendText(message);
            } catch (IOException e) {
                log.error("WebSocket推送失败: userId={}", userId, e);
                sessionMap.remove(userId);
            }
        }
    }

    /**
     * 广播消息给所有在线用户
     */
    public static void broadcast(String message) {
        sessionMap.forEach((userId, session) -> {
            if (session.isOpen()) {
                try {
                    session.getBasicRemote().sendText(message);
                } catch (IOException e) {
                    log.error("WebSocket广播失败: userId={}", userId, e);
                }
            }
        });
    }
}
