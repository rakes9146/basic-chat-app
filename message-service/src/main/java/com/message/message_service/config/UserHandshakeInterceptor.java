package com.message.message_service.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

/**
 * Handshake interceptor to extract userId from query parameters
 * and set it as the principal for WebSocket user destinations
 */
public class UserHandshakeInterceptor implements HandshakeInterceptor {

    private static final Logger log = LoggerFactory.getLogger(UserHandshakeInterceptor.class);

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                    WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
        
        log.info("[HANDSHAKE-INTERCEPTOR] 🤝 WebSocket handshake initiated");
        
        if (request instanceof ServletServerHttpRequest) {
            ServletServerHttpRequest servletRequest = (ServletServerHttpRequest) request;
            String userId = servletRequest.getServletRequest().getParameter("userId");
            
            if (userId != null && !userId.isEmpty()) {
                attributes.put("userId", userId);
                log.info("[HANDSHAKE-INTERCEPTOR] ✅ UserId extracted from query params: {}", userId);
            } else {
                log.warn("[HANDSHAKE-INTERCEPTOR] ⚠️ No userId in query parameters");
            }
            
            log.debug("[HANDSHAKE-INTERCEPTOR] Request URI: {}", servletRequest.getURI());
        } else {
            log.warn("[HANDSHAKE-INTERCEPTOR] ⚠️ Request is not a ServletServerHttpRequest");
        }
        
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                WebSocketHandler wsHandler, Exception exception) {
        if (exception != null) {
            log.error("[HANDSHAKE-INTERCEPTOR] ❌ Handshake failed with exception", exception);
        } else {
            log.info("[HANDSHAKE-INTERCEPTOR] ✅ Handshake completed successfully");
        }
    }
}
