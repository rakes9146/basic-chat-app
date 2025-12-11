package com.message.message_service.service;

import com.message.message_service.entity.Message;
import com.message.message_service.event.MessageSentEvent;
import com.message.message_service.kafka.MessageEventPublisher;
import com.message.message_service.repository.MessageRepository;
import com.message.message_service.web.data.MessageDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class MessageServiceImpl implements MessageService {

    private static final Logger log = LoggerFactory.getLogger(MessageServiceImpl.class);

    private final MessageRepository messageRepository;
    private final MessageEventPublisher eventPublisher;

    public MessageServiceImpl(MessageRepository messageRepository, MessageEventPublisher eventPublisher) {
        this.messageRepository = messageRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public List<MessageDto> getMessageBySenderAndReceiverId(Long senderId, Long receiverId) {
        try {
            List<MessageDto> messageDtoList = new ArrayList<>();
            // Use bidirectional query to get complete conversation
            List<Message> dbMessageList = messageRepository.findConversationBetweenUsers(senderId, receiverId);
            
            if (!dbMessageList.isEmpty()) {
                messageDtoList = dbMessageList.stream().map(msg -> {
                    MessageDto messageDto = new MessageDto();
                    messageDto.setMessageId(msg.getMessageId());
                    messageDto.setMessageText(msg.getMessageText());
                    messageDto.setSenderId(msg.getSenderId());
                    messageDto.setReceiverId(msg.getReceiverId());
                    messageDto.setDelivered(msg.isDelivered()); // Explicitly map boolean fields
                    messageDto.setRead(msg.isRead()); // Explicitly map boolean fields
                    messageDto.setCreatedDate(msg.getCreatedDate()); // Map timestamp from database
                    return messageDto;
                }).collect(Collectors.toUnmodifiableList());
                
                log.info("Retrieved {} messages with status - example: isDelivered={}, isRead={}", 
                        messageDtoList.size(), 
                        messageDtoList.isEmpty() ? "N/A" : messageDtoList.get(0).isDelivered(),
                        messageDtoList.isEmpty() ? "N/A" : messageDtoList.get(0).isRead());
            }
            
            log.info("Retrieved {} messages in conversation between users {} and {}", messageDtoList.size(), senderId, receiverId);
            return messageDtoList;
        } catch (Exception e) {
            log.error("Error retrieving messages for users: {} and {}", senderId, receiverId, e);
            throw new RuntimeException("Error retrieving messages", e);
        }
    }

    @Override
    public MessageDto saveNewMessage(MessageDto messageDto) {
        try {
            if (messageDto == null) {
                log.warn("Attempted to save null message");
                return null;
            }
            
            log.info("[SERVICE] Saving message - SenderId: {}, ReceiverId: {}, Text: {}", 
                    messageDto.getSenderId(), messageDto.getReceiverId(), messageDto.getMessageText());
            
            Message messageEntity = new Message();
            BeanUtils.copyProperties(messageDto, messageEntity);
            
            log.info("[SERVICE] After BeanUtils copy - SenderId: {}, ReceiverId: {}", 
                    messageEntity.getSenderId(), messageEntity.getReceiverId());
            
            messageEntity = messageRepository.save(messageEntity);
            
            log.info("[SERVICE] After DB save - ID: {}, SenderId: {}, ReceiverId: {}", 
                    messageEntity.getMessageId(), messageEntity.getSenderId(), messageEntity.getReceiverId());
            
            messageDto.setMessageId(messageEntity.getMessageId());
            
            // ✅ Publish Kafka event so receiver gets the message via WebSocket
            MessageSentEvent event = new MessageSentEvent();
            event.setMessageId(messageEntity.getMessageId());
            event.setSenderId(messageEntity.getSenderId());
            event.setReceiverId(messageEntity.getReceiverId());
            event.setContent(messageEntity.getMessageText());
            event.setTimestamp(Instant.now());
            
            eventPublisher.publishMessageSent(event);
            log.info("[SERVICE] ✅ Published MessageSentEvent to Kafka for message ID: {}", messageEntity.getMessageId());
            
            log.info("[SERVICE] Message saved successfully with ID: {}", messageEntity.getMessageId());
            return messageDto;
        } catch (Exception e) {
            log.error("Error saving message from sender: {}", messageDto.getSenderId(), e);
            throw new RuntimeException("Error saving message", e);
        }
    }

    @Override
    public void updateMessageDeliveryStatus(Long messageId) {
        try {
            messageRepository.markMessageAsDelivered(messageId);
            log.info("Message {} marked as delivered", messageId);
        } catch (Exception e) {
            log.error("Error updating delivery status for message: {}", messageId, e);
            throw new RuntimeException("Error updating delivery status", e);
        }
    }

    @Override
    public void updateMessageReadStatus(Long messageId) {
        try {
            messageRepository.markMessageAsRead(messageId);
            log.info("Message {} marked as read", messageId);
        } catch (Exception e) {
            log.error("Error updating read status for message: {}", messageId, e);
            throw new RuntimeException("Error updating read status", e);
        }
    }
}
