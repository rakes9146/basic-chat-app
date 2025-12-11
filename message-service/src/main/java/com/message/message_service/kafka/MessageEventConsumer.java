package com.message.message_service.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.message.message_service.event.MessageDeliveredEvent;
import com.message.message_service.event.MessageReadEvent;
import com.message.message_service.event.MessageSentEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * DEPRECATED: This consumer is disabled. Use WebSocketPushConsumer instead.
 * Keeping for reference but all @KafkaListener annotations are commented out.
 */
//@Service
public class MessageEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(MessageEventConsumer.class);
    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    public MessageEventConsumer(SimpMessagingTemplate messagingTemplate, ObjectMapper objectMapper) {
        this.messagingTemplate = messagingTemplate;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "chat.message.sent", groupId = "chat-group")
    public void handleMessageSent(String messageJson) {
        try {
            log.info("[KAFKA] Raw message received from topic 'chat.message.sent': {}", messageJson);
            
            MessageSentEvent event = objectMapper.readValue(messageJson, MessageSentEvent.class);
            log.info("[KAFKA] Parsed MessageSentEvent: messageId={}, senderId={}, receiverId={}, content={}", 
                     event.getMessageId(), event.getSenderId(), event.getReceiverId(), event.getContent());

            Map<String, Object> wsMessage = convertToWebSocketMessage(event);
            log.info("[WEBSOCKET] Converted to WebSocket message: {}", wsMessage);

            // Send to receiver via user-specific queue
            String destination = "/user/" + event.getReceiverId() + "/queue/messages";
            log.info("[WEBSOCKET] Sending to destination: {}", destination);
            
            messagingTemplate.convertAndSendToUser(
                String.valueOf(event.getReceiverId()),
                "/queue/messages",
                wsMessage
            );

            log.info("[SUCCESS] Message forwarded to receiver userId={}", event.getReceiverId());
        } catch (Exception e) {
            log.error("[ERROR] Failed to process MessageSentEvent: {}", messageJson, e);
        }
    }

    @KafkaListener(topics = "chat.message.delivered", groupId = "chat-group")
    public void handleMessageDelivered(String messageJson) {
        try {
            log.info("[KAFKA] Raw delivery status received: {}", messageJson);
            
            MessageDeliveredEvent event = objectMapper.readValue(messageJson, MessageDeliveredEvent.class);
            log.info("[KAFKA] Parsed MessageDeliveredEvent: messageId={}, senderId={}", 
                     event.getMessageId(), event.getSenderId());

            // Notify sender that message was delivered
            Map<String, Object> status = new HashMap<>();
            status.put("messageId", event.getMessageId());
            status.put("senderId", event.getSenderId());
            status.put("receiverId", event.getReceiverId());
            status.put("delivered", true);
            status.put("deliveredAt", event.getDeliveredAt());

            log.info("[WEBSOCKET] Sending delivery status to sender userId={}: {}", event.getSenderId(), status);
            
            messagingTemplate.convertAndSendToUser(
                String.valueOf(event.getSenderId()),
                "/queue/status",
                status
            );

            log.info("[SUCCESS] Delivery status sent to sender userId={}", event.getSenderId());
        } catch (Exception e) {
            log.error("[ERROR] Failed to process MessageDeliveredEvent: {}", messageJson, e);
        }
    }

    @KafkaListener(topics = "chat.message.read", groupId = "chat-group")
    public void handleMessageRead(String messageJson) {
        try {
            MessageReadEvent event = objectMapper.readValue(messageJson, MessageReadEvent.class);
            log.info("Received MessageReadEvent from Kafka: messageId={}, senderId={}", 
                     event.getMessageId(), event.getSenderId());

            // Notify sender that message was read
            Map<String, Object> status = new HashMap<>();
            status.put("messageId", event.getMessageId());
            status.put("senderId", event.getSenderId());
            status.put("receiverId", event.getReceiverId());
            status.put("read", true);
            status.put("readAt", event.getReadAt());

            messagingTemplate.convertAndSendToUser(
                String.valueOf(event.getSenderId()),
                "/queue/status",
                status
            );

            log.info("Sent read status to sender {}", event.getSenderId());
        } catch (Exception e) {
            log.error("Error processing MessageReadEvent: {}", messageJson, e);
        }
    }

    private Map<String, Object> convertToWebSocketMessage(MessageSentEvent event) {
        Map<String, Object> message = new HashMap<>();
        message.put("messageId", event.getMessageId());
        message.put("messageText", event.getContent());
        message.put("senderId", event.getSenderId());
        message.put("receiverId", event.getReceiverId());
        message.put("timestamp", event.getTimestamp());
        message.put("isDelivered", false);
        message.put("isRead", false);
        return message;
    }
}

