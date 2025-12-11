package com.message.message_service.kafka;


import com.message.message_service.event.UserPresenceEvent;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PresenceConsumer {

    private final Map<Long, Boolean> onlineUsers = new ConcurrentHashMap<>();

    @KafkaListener(topics = "chat.user.presence", groupId = "chat-group")
    public void trackPresence(UserPresenceEvent event) {
        onlineUsers.put(event.getUserId(), event.isOnline());
    }

    public boolean isUserOnline(Long userId) {
        return onlineUsers.getOrDefault(userId, false);
    }
}