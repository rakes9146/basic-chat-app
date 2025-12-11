package com.message.message_service.web.data.rest;

import com.message.message_service.event.MessageDeliveredEvent;
import com.message.message_service.event.MessageReadEvent;
import com.message.message_service.kafka.MessageEventPublisher;
import com.message.message_service.repository.MessageRepository;
import com.message.message_service.service.MessageService;
import com.message.message_service.web.data.MessageDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.time.Instant;

@Controller
public class WebSocketMessageController {

    private static final Logger log = LoggerFactory.getLogger(WebSocketMessageController.class);
    private final MessageService messageService;
    private final MessageEventPublisher eventPublisher;
    private final MessageRepository messageRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final org.springframework.messaging.simp.user.SimpUserRegistry userRegistry;

    public WebSocketMessageController(MessageService messageService,
                                      MessageEventPublisher eventPublisher,
                                      MessageRepository messageRepository,
                                      SimpMessagingTemplate messagingTemplate,
                                      org.springframework.messaging.simp.user.SimpUserRegistry userRegistry) {
        this.messageService = messageService;
        this.eventPublisher = eventPublisher;
        this.messageRepository = messageRepository;
        this.messagingTemplate = messagingTemplate;
        this.userRegistry = userRegistry;
    }

    // Sender sends a message via WebSocket - save and publish
    @MessageMapping("/send")
    public void send(@Payload MessageDto dto) {
        log.info("[WEBSOCKET] 📨 /send received: senderId={}, receiverId={}, text='{}'",
                dto.getSenderId(), dto.getReceiverId(), dto.getMessageText());

        // Save message to database first
        MessageDto savedMessage = messageService.saveNewMessage(dto);
        
        if (savedMessage == null || savedMessage.getMessageId() == null) {
            log.error("[WEBSOCKET] ❌ Failed to save message - senderId={}, receiverId={}", 
                    dto.getSenderId(), dto.getReceiverId());
            return;
        }

        log.info("[WEBSOCKET] ✅ Message saved with ID: {}", savedMessage.getMessageId());
        
        // Create message payload
        java.util.Map<String, Object> wsMessage = new java.util.HashMap<>();
        wsMessage.put("messageId", savedMessage.getMessageId());
        wsMessage.put("senderId", savedMessage.getSenderId());
        wsMessage.put("receiverId", savedMessage.getReceiverId());
        wsMessage.put("messageText", savedMessage.getMessageText());
        wsMessage.put("timestamp", Instant.now());
        wsMessage.put("isDelivered", false);
        wsMessage.put("isRead", false);
        
        log.info("[WEBSOCKET-PUSH] 📤 Message payload created: {}", wsMessage);
        
        // Log all registered users
        log.info("[REGISTRY] Total users in SimpUserRegistry: {}", userRegistry.getUserCount());
        userRegistry.getUsers().forEach(user -> 
            log.info("[REGISTRY] Registered user: name={}, sessions={}", 
                    user.getName(), user.getSessions().size())
        );
        
        // Push message to RECEIVER immediately via WebSocket
        String receiverIdStr = savedMessage.getReceiverId().toString();
        var receiverUser = userRegistry.getUser(receiverIdStr);
        log.info("[WEBSOCKET-PUSH] 📬 Receiver lookup: userId={}, found={}, sessions={}", 
                receiverIdStr, 
                receiverUser != null,
                receiverUser != null ? receiverUser.getSessions().size() : 0);
        
        try {
            messagingTemplate.convertAndSendToUser(
                receiverIdStr,
                "/queue/messages",
                wsMessage
            );
            log.info("[WEBSOCKET-PUSH] ✅ Pushed to receiver: userId={}, messageId={}", 
                    receiverIdStr, savedMessage.getMessageId());
        } catch (Exception e) {
            log.error("[WEBSOCKET-PUSH] ❌ Failed to push to receiver: userId={}", receiverIdStr, e);
        }
        
        // Push message to SENDER as confirmation (with messageId)
        String senderIdStr = savedMessage.getSenderId().toString();
        var senderUser = userRegistry.getUser(senderIdStr);
        log.info("[WEBSOCKET-PUSH] 📬 Sender lookup: userId={}, found={}, sessions={}", 
                senderIdStr,
                senderUser != null,
                senderUser != null ? senderUser.getSessions().size() : 0);
        
        try {
            messagingTemplate.convertAndSendToUser(
                senderIdStr,
                "/queue/messages",
                wsMessage
            );
            log.info("[WEBSOCKET-PUSH] ✅ Pushed to sender: userId={}, messageId={}", 
                    senderIdStr, savedMessage.getMessageId());
        } catch (Exception e) {
            log.error("[WEBSOCKET-PUSH] ❌ Failed to push to sender: userId={}", senderIdStr, e);
        }
        
        log.info("[WEBSOCKET] 🎉 Message processing complete for messageId: {}", savedMessage.getMessageId());
    }

    // Receiver marks a message as delivered
    @MessageMapping("/deliver")
    public void markDelivered(@Payload MessageDto dto) {
        log.info("[WEBSOCKET] /deliver received: messageId={}, receiverId={}",
                dto.getMessageId(), dto.getReceiverId());

        if (dto.getMessageId() == null) {
            log.error("[WEBSOCKET] Rejected /deliver - messageId is null");
            return;
        }

        // Update message as delivered in DB
        messageService.updateMessageDeliveryStatus(dto.getMessageId());

        // Fetch the message to get senderId
        messageRepository.findById(dto.getMessageId()).ifPresent(msg -> {
            // Publish delivery event to notify sender
            MessageDeliveredEvent event = new MessageDeliveredEvent();
            event.setMessageId(dto.getMessageId());
            event.setSenderId(msg.getSenderId());  // Notify the sender
            event.setReceiverId(dto.getReceiverId());
            event.setDelivered(true);
            event.setDeliveredAt(Instant.now());

            log.info("[KAFKA] Publishing MessageDeliveredEvent: {}", event);
            eventPublisher.publishDelivered(event);
            log.info("[SUCCESS] Published MessageDeliveredEvent to chat.message.delivered");
        });
    }

    // Receiver marks a message as read
    @MessageMapping("/read")
    public void markRead(@Payload MessageDto dto) {
        log.info("[WEBSOCKET] /read received: messageId={}, receiverId={}",
                dto.getMessageId(), dto.getReceiverId());

        if (dto.getMessageId() == null) {
            log.error("[WEBSOCKET] Rejected /read - messageId is null");
            return;
        }

        // Update message as read in DB
        messageService.updateMessageReadStatus(dto.getMessageId());

        // Fetch the message to get senderId
        messageRepository.findById(dto.getMessageId()).ifPresent(msg -> {
            // Publish read event to notify sender
            MessageReadEvent event = new MessageReadEvent();
            event.setMessageId(dto.getMessageId());
            event.setSenderId(msg.getSenderId());  // Notify the sender
            event.setReceiverId(dto.getReceiverId());
            event.setRead(true);
            event.setReadAt(Instant.now());

            log.info("[KAFKA] Publishing MessageReadEvent: {}", event);
            eventPublisher.publishRead(event);
            log.info("[SUCCESS] Published MessageReadEvent to chat.message.read");
        });
    }
}
