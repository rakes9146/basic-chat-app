package com.message.message_service.kafka;

import com.message.message_service.event.MessageDeliveredEvent;
import com.message.message_service.event.MessageReadEvent;
import com.message.message_service.repository.MessageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;

@Service
public class MessageStatusConsumer {

    private static final Logger log = LoggerFactory.getLogger(MessageStatusConsumer.class);
    private final MessageRepository repo;

    public MessageStatusConsumer(MessageRepository repo) {
        this.repo = repo;
    }

    @KafkaListener(topics = "chat.message.delivered", groupId = "message-db")
    public void handleDelivered(MessageDeliveredEvent event) {
        try {
                        log.info("[KAFKA-CONSUMER] 📬 Received delivery event for messageId: {}", event.getMessageId());
            repo.findById(event.getMessageId()).ifPresentOrElse(
                    m -> {
                        m.setDelivered(true);
                        m.setDeliveredAt(Timestamp.from(event.getDeliveredAt()).toLocalDateTime());
                        repo.save(m);
                        log.info("[DATABASE] ✅ Message {} marked as delivered in DB", event.getMessageId());
                    },
                    () -> log.warn("[DATABASE] ⚠️ Message not found for delivery update: {}", event.getMessageId())
            );
        } catch (Exception e) {
            log.error("Error updating delivery status for message: {}", event.getMessageId(), e);
        }
    }

    @KafkaListener(topics = "chat.message.read", groupId = "message-db")
    public void handleRead(MessageReadEvent event) {
                    log.info("[KAFKA-CONSUMER] 👁️ Received read event for messageId: {}", event.getMessageId());
        try {
            repo.findById(event.getMessageId()).ifPresentOrElse(
                    m -> {
                        m.setRead(true);
                        m.setReadAt(Timestamp.from(event.getReadAt()).toLocalDateTime());
                        repo.save(m);
                        log.info("[DATABASE] ✅ Message {} marked as read in DB", event.getMessageId());
                    },
                    () -> log.warn("[DATABASE] ⚠️ Message not found for read update: {}", event.getMessageId())
            );
        } catch (Exception e) {
            log.error("Error updating read status for message: {}", event.getMessageId(), e);
        }
    }
}
