package com.message.message_service.service;

import com.message.message_service.event.MessageReadEvent;
import com.message.message_service.kafka.MessageEventPublisher;
import com.message.message_service.repository.MessageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class ChatActivityService {

    private static final Logger log = LoggerFactory.getLogger(ChatActivityService.class);
    private final StringRedisTemplate redis;
    private final MessageRepository messageRepository;
    private final MessageEventPublisher eventPublisher;

    public ChatActivityService(StringRedisTemplate redis, 
                               MessageRepository messageRepository,
                               MessageEventPublisher eventPublisher) {
        this.redis = redis;
        this.messageRepository = messageRepository;
        this.eventPublisher = eventPublisher;
    }

    private String key(Long userId) {
        return "chat:active:user:" + userId;
    }

    public void setActive(Long userId, Long peerId) {
        if (userId == null || peerId == null) return;
        try {
            redis.opsForValue().set(key(userId), String.valueOf(peerId));
            log.info("[CHAT-ACTIVITY] 💬 User {} opened chat with peer {}", userId, peerId);
            
            // Mark all unread messages from peerId to userId as read
            markUnreadMessagesAsRead(userId, peerId);
        } catch (Exception e) {
            log.warn("[REDIS] Could not set chat active status (Redis unavailable): {}", e.getMessage());
        }
    }

    private void markUnreadMessagesAsRead(Long userId, Long peerId) {
        try {
            // Find all unread messages from peer to current user
            var unreadMessages = messageRepository.findBySenderIdAndReceiverIdAndIsReadFalse(peerId, userId);
            log.info("[CHAT-ACTIVITY] 📖 Found {} unread messages from {} to {}", 
                    unreadMessages.size(), peerId, userId);
            
            for (var message : unreadMessages) {
                log.info("[CHAT-ACTIVITY] 📘 Marking message {} as read", message.getMessageId());
                
                // Update in DB
                message.setRead(true);
                messageRepository.save(message);
                
                // Publish read event to notify sender (peerId is the sender)
                MessageReadEvent event = new MessageReadEvent();
                event.setMessageId(message.getMessageId());
                event.setSenderId(peerId);  // The peer who sent the message
                event.setReceiverId(userId); // The current user who is reading
                event.setRead(true);
                event.setReadAt(Instant.now());
                
                log.info("[CHAT-ACTIVITY] 🔔 Publishing read event for message {}", message.getMessageId());
                eventPublisher.publishRead(event);
            }
            
            log.info("[CHAT-ACTIVITY] ✅ Marked {} messages as read", unreadMessages.size());
        } catch (Exception e) {
            log.error("[CHAT-ACTIVITY] ❌ Error marking messages as read", e);
        }
    }

    public void clearActive(Long userId) {
        if (userId == null) return;
        try {
            redis.delete(key(userId));
        } catch (Exception e) {
            log.warn("[REDIS] Could not clear chat active status (Redis unavailable): {}", e.getMessage());
        }
    }

    public boolean isActiveWith(Long userId, Long peerId) {
        if (userId == null || peerId == null) return false;
        try {
            String val = redis.opsForValue().get(key(userId));
            return String.valueOf(peerId).equals(val);
        } catch (Exception e) {
            log.warn("[REDIS] Could not check chat active status (Redis unavailable): {}", e.getMessage());
            return false;
        }
    }
}
