package com.message.message_service.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;

/**
 * Listener for WebSocket connection events
 * Logs when users connect, disconnect, and subscribe to topics
 */
@Component
public class WebSocketEventListener {

    private static final Logger log = LoggerFactory.getLogger(WebSocketEventListener.class);
    private final SimpUserRegistry userRegistry;

    public WebSocketEventListener(SimpUserRegistry userRegistry) {
        this.userRegistry = userRegistry;
    }

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        String userId = headerAccessor.getUser() != null ? headerAccessor.getUser().getName() : "anonymous";
        
        log.info("=".repeat(80));
        log.info("[WEBSOCKET-EVENT] 🟢 NEW CONNECTION");
        log.info("=".repeat(80));
        log.info("[CONNECTION] Session ID: {}", sessionId);
        log.info("[CONNECTION] User ID: {}", userId);
        log.info("[CONNECTION] Total users in registry: {}", userRegistry.getUserCount());
        
        // List all connected users
        userRegistry.getUsers().forEach(user -> {
            log.info("[CONNECTION] 👤 Registered user: {} with {} session(s)", 
                user.getName(), user.getSessions().size());
        });
        
        log.info("=".repeat(80));
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        String userId = headerAccessor.getUser() != null ? headerAccessor.getUser().getName() : "anonymous";
        
        log.info("=".repeat(80));
        log.info("[WEBSOCKET-EVENT] 🔴 DISCONNECTION");
        log.info("=".repeat(80));
        log.info("[DISCONNECT] Session ID: {}", sessionId);
        log.info("[DISCONNECT] User ID: {}", userId);
        log.info("[DISCONNECT] Remaining users in registry: {}", userRegistry.getUserCount());
        
        // List remaining connected users
        userRegistry.getUsers().forEach(user -> {
            log.info("[DISCONNECT] 👤 Still registered: {} with {} session(s)", 
                user.getName(), user.getSessions().size());
        });
        
        log.info("=".repeat(80));
    }

    @EventListener
    public void handleSubscribeEvent(SessionSubscribeEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        String userId = headerAccessor.getUser() != null ? headerAccessor.getUser().getName() : "anonymous";
        String destination = headerAccessor.getDestination();
        
        log.info("[WEBSOCKET-EVENT] 📡 SUBSCRIPTION");
        log.info("[SUBSCRIBE] User: {} | Session: {} | Destination: {}", userId, sessionId, destination);
        log.info("[SUBSCRIBE] Total registered users: {}", userRegistry.getUserCount());
    }
}
