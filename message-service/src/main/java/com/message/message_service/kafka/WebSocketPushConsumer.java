package com.message.message_service.kafka;

import com.message.message_service.event.MessageDeliveredEvent;
import com.message.message_service.event.MessageReadEvent;
import com.message.message_service.event.MessageSentEvent;
import com.message.message_service.kafka.MessageEventPublisher;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.user.SimpUser;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.stereotype.Service;

@Service
public class WebSocketPushConsumer {

    private static final Logger log = LoggerFactory.getLogger(WebSocketPushConsumer.class);
    private final SimpMessagingTemplate messagingTemplate;
    private final MessageEventPublisher eventPublisher;
    private final com.message.message_service.service.ChatActivityService chatActivityService;
    private final SimpUserRegistry userRegistry;

    public WebSocketPushConsumer(SimpMessagingTemplate messagingTemplate,
                                 MessageEventPublisher eventPublisher,
                                 com.message.message_service.service.ChatActivityService chatActivityService,
                                 SimpUserRegistry userRegistry) {
        this.messagingTemplate = messagingTemplate;
        this.eventPublisher = eventPublisher;
        this.chatActivityService = chatActivityService;
        this.userRegistry = userRegistry;
    }

    @KafkaListener(topics = "chat.message.sent", groupId = "message-ws")
    public void pushMessage(MessageSentEvent event) {
        try {
            log.info("[KAFKA-CONSUMER] 📨 Received MessageSentEvent: messageId={}, senderId={}, receiverId={}", 
                        event.getMessageId(), event.getSenderId(), event.getReceiverId());
            
            // Check if receiver has active WebSocket session
            SimpUser user = userRegistry.getUser(event.getReceiverId().toString());
            boolean online = user != null && !user.getSessions().isEmpty();
            
            log.info("[WEBSOCKET-CHECK] Receiver {} online status: {}, user object: {}", 
                    event.getReceiverId(), online, user != null ? "present" : "null");
            
            if (user != null) {
                log.info("[WEBSOCKET-CHECK] User has {} sessions", user.getSessions().size());
            }
            
            // Create WebSocket message DTO matching frontend expectations
            java.util.Map<String, Object> wsMessage = new java.util.HashMap<>();
            wsMessage.put("messageId", event.getMessageId());
            wsMessage.put("senderId", event.getSenderId());
            wsMessage.put("receiverId", event.getReceiverId());
            wsMessage.put("messageText", event.getContent());  // Map content to messageText for frontend
            wsMessage.put("timestamp", event.getTimestamp());
            wsMessage.put("isDelivered", false);
            wsMessage.put("isRead", false);
            
            log.info("[WEBSOCKET-PUSH] Attempting to push message to /user/{}/queue/messages", event.getReceiverId());
            
            messagingTemplate.convertAndSendToUser(
                    event.getReceiverId().toString(),
                    "/queue/messages",
                    wsMessage
            );
            log.info("[WEBSOCKET-PUSH] ✅ Message pushed to receiver: {} for message: {}", 
                    event.getReceiverId(), event.getMessageId());

            // Only publish delivery if receiver has an active WebSocket session
            if (online) {
                try {
                    MessageDeliveredEvent delivered = new MessageDeliveredEvent();
                    delivered.setMessageId(event.getMessageId());
                    delivered.setSenderId(event.getSenderId());
                    delivered.setReceiverId(event.getReceiverId());
                    delivered.setDelivered(true);
                    delivered.setDeliveredAt(Instant.now());
                    eventPublisher.publishDelivered(delivered);
                    log.info("[DELIVERY] ✅ Receiver online. Published delivery for message {}", event.getMessageId());

                    // If receiver is actively viewing this chat, also publish read
                    if (chatActivityService.isActiveWith(event.getReceiverId(), event.getSenderId())) {
                        MessageReadEvent read = new MessageReadEvent();
                        read.setMessageId(event.getMessageId());
                        read.setSenderId(event.getSenderId());
                        read.setReceiverId(event.getReceiverId());
                        read.setRead(true);
                        read.setReadAt(Instant.now());
                        eventPublisher.publishRead(read);
                        log.info("[READ] ✅ Receiver in chat with sender. Published read for message {}", event.getMessageId());
                    } else {
                        log.info("[READ] ⏳ Receiver not in chat. Skipping auto-read for message {}", event.getMessageId());
                    }
                } catch (Exception e) {
                    log.error("Failed to publish delivery/read for message {}", event.getMessageId(), e);
                }
            } else {
                log.warn("[DELIVERY] 💤 Receiver {} has no active WebSocket session. Message pushed but delivery not confirmed.", event.getReceiverId());
            }
        } catch (Exception e) {
            log.error("Error pushing message to WebSocket for receiver: {}", event.getReceiverId(), e);
        }
    }

    @KafkaListener(topics = "chat.message.delivered", groupId = "message-ws")
    public void notifySenderDelivered(MessageDeliveredEvent event) {
        log.info("[KAFKA-CONSUMER] 📬 Received MessageDeliveredEvent: messageId={}, senderId={}, receiverId={}", 
                event.getMessageId(), event.getSenderId(), event.getReceiverId());
        
        String senderIdStr = event.getSenderId().toString();
        var senderUser = userRegistry.getUser(senderIdStr);
        log.info("[STATUS-PUSH] Sender lookup for delivery: userId={}, found={}, sessions={}", 
                senderIdStr,
                senderUser != null,
                senderUser != null ? senderUser.getSessions().size() : 0);
        
        try {
            // Create status payload matching frontend expectations
            java.util.Map<String, Object> statusPayload = new java.util.HashMap<>();
            statusPayload.put("messageId", event.getMessageId());
            statusPayload.put("senderId", event.getSenderId());
            statusPayload.put("receiverId", event.getReceiverId());
            statusPayload.put("delivered", true);
            statusPayload.put("deliveredAt", event.getDeliveredAt());
            
            log.info("[STATUS-PUSH] Sending delivery status payload: {}", statusPayload);
            
            messagingTemplate.convertAndSendToUser(
                    senderIdStr,
                    "/queue/status",
                    statusPayload
            );
            log.info("[WEBSOCKET-PUSH] ✅ Delivery notification sent to sender: {} for message: {}", 
                    senderIdStr, event.getMessageId());
        } catch (Exception e) {
            log.error("[STATUS-PUSH] ❌ Error notifying sender about delivery for message: {}", event.getMessageId(), e);
        }
    }

    @KafkaListener(topics = "chat.message.read", groupId = "message-ws")
    public void notifySenderRead(MessageReadEvent event) {
        log.info("[KAFKA-CONSUMER] 👁️ Received MessageReadEvent: messageId={}, senderId={}, receiverId={}", 
                event.getMessageId(), event.getSenderId(), event.getReceiverId());
        
        String senderIdStr = event.getSenderId().toString();
        var senderUser = userRegistry.getUser(senderIdStr);
        log.info("[STATUS-PUSH] Sender lookup for read: userId={}, found={}, sessions={}", 
                senderIdStr,
                senderUser != null,
                senderUser != null ? senderUser.getSessions().size() : 0);
        
        try {
            // Create status payload matching frontend expectations
            java.util.Map<String, Object> statusPayload = new java.util.HashMap<>();
            statusPayload.put("messageId", event.getMessageId());
            statusPayload.put("senderId", event.getSenderId());
            statusPayload.put("receiverId", event.getReceiverId());
            statusPayload.put("read", true);
            statusPayload.put("readAt", event.getReadAt());
            
            log.info("[STATUS-PUSH] Sending read status payload: {}", statusPayload);
            
            messagingTemplate.convertAndSendToUser(
                    senderIdStr,
                    "/queue/status",
                    statusPayload
            );
            log.info("[WEBSOCKET-PUSH] ✅ Read notification sent to sender: {} for message: {}", 
                    senderIdStr, event.getMessageId());
        } catch (Exception e) {
            log.error("[STATUS-PUSH] ❌ Error notifying sender about read status for message: {}", event.getMessageId(), e);
        }
    }
}
