package com.message.message_service.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;

import java.security.Principal;

/**
 * Channel interceptor to set user principal from WebSocket session attributes
 */
public class UserChannelInterceptor implements ChannelInterceptor {
    
    private static final Logger log = LoggerFactory.getLogger(UserChannelInterceptor.class);

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        
        if (accessor != null) {
            log.info("[CHANNEL-INTERCEPTOR] 🔄 Processing command: {}", accessor.getCommand());
            
            if (StompCommand.CONNECT.equals(accessor.getCommand())) {
                log.info("[CHANNEL-INTERCEPTOR] 🔌 CONNECT frame received");
                log.info("[CHANNEL-INTERCEPTOR] All native headers: {}", accessor.toNativeHeaderMap());
                log.info("[CHANNEL-INTERCEPTOR] Session attributes: {}", accessor.getSessionAttributes());
                
                // Try to get userId from native headers first
                String userId = accessor.getFirstNativeHeader("userId");
                log.info("[CHANNEL-INTERCEPTOR] UserId from native header: {}", userId);
                
                // If not in native headers, try session attributes (from handshake)
                if ((userId == null || userId.isEmpty()) && accessor.getSessionAttributes() != null) {
                    Object userIdFromSession = accessor.getSessionAttributes().get("userId");
                    if (userIdFromSession != null) {
                        userId = userIdFromSession.toString();
                        log.info("[CHANNEL-INTERCEPTOR] UserId from session attributes: {}", userId);
                    }
                }
                
                final String finalUserId = userId;
                
                if (finalUserId != null && !finalUserId.isEmpty()) {
                    Principal principal = () -> finalUserId;
                    accessor.setUser(principal);
                    log.info("[CHANNEL-INTERCEPTOR] ✅ Set principal for user: {}", finalUserId);
                    
                    if (accessor.getUser() != null) {
                        log.info("[CHANNEL-INTERCEPTOR] ✅ Principal confirmed - name: {}", accessor.getUser().getName());
                    } else {
                        log.error("[CHANNEL-INTERCEPTOR] ❌ Principal is null after setting!");
                    }
                } else {
                    log.error("[CHANNEL-INTERCEPTOR] ❌ No userId found in headers or session!");
                }
            } else if (accessor.getUser() != null) {
                log.debug("[CHANNEL-INTERCEPTOR] Command {} has user: {}", accessor.getCommand(), accessor.getUser().getName());
            } else {
                log.warn("[CHANNEL-INTERCEPTOR] ⚠️ Command {} has NO user principal!", accessor.getCommand());
            }
        }
        
        return message;
    }
}
