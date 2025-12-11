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
            log.debug("[CHANNEL-INTERCEPTOR] Processing command: {}", accessor.getCommand());
            
            if (StompCommand.CONNECT.equals(accessor.getCommand())) {
                log.info("[CHANNEL-INTERCEPTOR] 🔌 CONNECT frame received");
                log.info("[CHANNEL-INTERCEPTOR] All native headers: {}", accessor.toNativeHeaderMap());
                
                String userId = accessor.getFirstNativeHeader("userId");
                
                if (userId != null && !userId.isEmpty()) {
                    Principal principal = () -> userId;
                    accessor.setUser(principal);
                    log.info("[CHANNEL-INTERCEPTOR] ✅ Set principal for user: {}", userId);
                    log.info("[CHANNEL-INTERCEPTOR] Principal name: {}", accessor.getUser().getName());
                } else {
                    log.error("[CHANNEL-INTERCEPTOR] ❌ No userId in CONNECT headers!");
                }
            } else if (accessor.getUser() != null) {
                log.debug("[CHANNEL-INTERCEPTOR] Command {} has user: {}", accessor.getCommand(), accessor.getUser().getName());
            }
        }
        
        return message;
    }
}
