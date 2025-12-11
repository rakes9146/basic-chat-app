package com.message.message_service.web.data.rest;

import com.message.message_service.event.UserPresenceEvent;
import com.message.message_service.service.PresenseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.stereotype.Controller;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Controller
public class PresenceController {

    private static final Logger log = LoggerFactory.getLogger(PresenceController.class);
    private final PresenseService presenseService;
    private final SimpUserRegistry userRegistry;
    private final SimpMessagingTemplate messagingTemplate;

    public PresenceController(PresenseService presenseService,
                             SimpUserRegistry userRegistry,
                             SimpMessagingTemplate messagingTemplate) {
        this.presenseService = presenseService;
        this.userRegistry = userRegistry;
        this.messagingTemplate = messagingTemplate;
    }

    @MessageMapping("/presence")
    public void handlePresence(UserPresenceEvent event) {
        try {
            if (event == null || event.getUserId() == null) return;
            if (event.isOnline()) {
                presenseService.setOnline(event.getUserId());
                log.info("[PRESENCE] ✅ Set user {} online via STOMP presence", event.getUserId());
                
                // When a user comes online, send them the list of currently online users
                sendOnlineUsersToNewUser(event.getUserId());
            } else {
                presenseService.setOffline(event.getUserId());
                log.info("[PRESENCE] ⚪ Set user {} offline via STOMP presence", event.getUserId());
            }
        } catch (Exception e) {
            log.error("Error handling presence event: {}", event, e);
        }
    }
    
    private void sendOnlineUsersToNewUser(Long userId) {
        try {
            // Get all currently connected users from SimpUserRegistry
            userRegistry.getUsers().forEach(user -> {
                try {
                    Long onlineUserId = Long.parseLong(user.getName());
                    if (!onlineUserId.equals(userId)) {
                        // Send presence notification to the new user about each online user
                        Map<String, Object> presence = new HashMap<>();
                        presence.put("userId", onlineUserId);
                        presence.put("online", true);
                        presence.put("lastSeen", Instant.now());
                        
                        messagingTemplate.convertAndSendToUser(
                            userId.toString(),
                            "/queue/presence-init",
                            presence
                        );
                        log.info("[PRESENCE] 📤 Sent user {} status to new user {}", onlineUserId, userId);
                    }
                } catch (NumberFormatException e) {
                    log.warn("[PRESENCE] Invalid user ID format: {}", user.getName());
                }
            });
        } catch (Exception e) {
            log.error("[PRESENCE] Error sending online users list", e);
        }
    }
}
