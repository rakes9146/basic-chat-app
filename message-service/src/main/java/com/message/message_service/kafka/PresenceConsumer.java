package com.message.message_service.kafka;

import com.message.message_service.entity.Message;
import com.message.message_service.event.MessageDeliveredEvent;
import com.message.message_service.event.MessageSentEvent;
import com.message.message_service.event.UserPresenceEvent;
import com.message.message_service.repository.MessageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PresenceConsumer {

    private static final Logger log = LoggerFactory.getLogger(PresenceConsumer.class);
    private final Map<Long, Boolean> onlineUsers = new ConcurrentHashMap<>();
    private final SimpMessagingTemplate messagingTemplate;
    private final MessageRepository messageRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    
    public PresenceConsumer(KafkaTemplate<String, Object> kafkaTemplate, 
                           SimpMessagingTemplate messagingTemplate,
                           MessageRepository messageRepository) {
        this.messagingTemplate = messagingTemplate;
        this.messageRepository = messageRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    @KafkaListener(topics = "chat.user.presence", groupId = "chat-group")
    public void trackPresence(UserPresenceEvent event) {
        try {
            onlineUsers.put(event.getUserId(), event.isOnline());
            log.info("[PRESENCE] 📡 User {} status: {}", event.getUserId(), event.isOnline() ? "ONLINE" : "OFFLINE");
            
            // If user comes online, mark all undelivered messages as delivered
            if (event.isOnline()) {
                markUndeliveredMessagesAsDelivered(event.getUserId());
            }
            
            // Broadcast presence update to all connected clients via WebSocket
            Map<String, Object> presenceNotification = new HashMap<>();
            presenceNotification.put("userId", event.getUserId());
            presenceNotification.put("online", event.isOnline());
            presenceNotification.put("lastSeen", event.getLastSeen());
            
            messagingTemplate.convertAndSend("/topic/presence", presenceNotification);
            log.info("[PRESENCE] ✅ Broadcasted to /topic/presence: userId={}, online={}", 
                    event.getUserId(), event.isOnline());
        } catch (Exception e) {
            log.error("Error tracking presence for user: {}", event.getUserId(), e);
        }
    }
    
    /**
     * Mark all undelivered messages as delivered when user comes online
     */
    private void markUndeliveredMessagesAsDelivered(Long userId) {
        try {
            List<Message> undeliveredMessages = messageRepository.findByReceiverIdAndIsDeliveredFalse(userId);
            
            if (undeliveredMessages.isEmpty()) {
                log.info("[PRESENCE] 📭 No undelivered messages for user {}", userId);
                return;
            }
            
            log.info("[PRESENCE] 📬 Found {} undelivered messages for user {}", undeliveredMessages.size(), userId);
            
            for (Message message : undeliveredMessages) {
                // Update message in database
                message.setDelivered(true);
                message.setDeliveredAt(LocalDateTime.now());
                messageRepository.save(message);
                
                log.info("[PRESENCE] ✅ Marked message {} as delivered", message.getMessageId());
                
                // Publish delivery event to Kafka for sender notification
                MessageDeliveredEvent deliveryEvent = new MessageDeliveredEvent();
                deliveryEvent.setMessageId(message.getMessageId());
                deliveryEvent.setSenderId(message.getSenderId());
                deliveryEvent.setReceiverId(message.getReceiverId());
                deliveryEvent.setDelivered(true);
                deliveryEvent.setDeliveredAt(Instant.now());
                
                kafkaTemplate.send("chat.message.delivered", deliveryEvent);
                log.info("[PRESENCE] 📤 Published delivery event for message {}", message.getMessageId());
            }
            
            log.info("[PRESENCE] ✅ Completed marking {} messages as delivered for user {}", 
                    undeliveredMessages.size(), userId);
            
        } catch (Exception e) {
            log.error("[PRESENCE] ❌ Error marking undelivered messages as delivered for user {}", userId, e);
        }
    }

    public boolean isUserOnline(Long userId) {
        return onlineUsers.getOrDefault(userId, false);
    }

    public void maybeTriggerDelivery(MessageSentEvent event) {
        try {
            boolean online = isUserOnline(event.getReceiverId());
            if (online) {
                log.info("[PRESENCE] ✅ Receiver online for message: {} (delivery handled by WebSocketPushConsumer)", event.getMessageId());
            } else {
                log.info("[PRESENCE] 💤 Receiver offline. Skipping delivery event for message: {}", event.getMessageId());
            }
        } catch (Exception e) {
            log.error("Error triggering delivery for message: {}", event.getMessageId(), e);
        }
    }

    @KafkaListener(topics = "chat.message.sent", groupId = "presence-group")
    public void onMessageSent(MessageSentEvent event) {
        try {
            log.debug("PresenceConsumer received MessageSentEvent for message {}", event.getMessageId());
            maybeTriggerDelivery(event);
        } catch (Exception e) {
            log.error("Error handling MessageSentEvent for presence check: {}", event.getMessageId(), e);
        }
    }
}