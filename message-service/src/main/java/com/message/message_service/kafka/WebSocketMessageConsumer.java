package com.message.message_service.kafka;

import com.message.message_service.event.MessageSentEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

/**
 * DEPRECATED: Use WebSocketPushConsumer instead (has proper delivery/read event publishing)
 */
//@Service
public class WebSocketMessageConsumer {

    private static final Logger log = LoggerFactory.getLogger(WebSocketMessageConsumer.class);
    private final SimpMessagingTemplate messagingTemplate;

    public WebSocketMessageConsumer(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @KafkaListener(topics = "chat.message.sent", groupId = "chat-group")
    public void pushToReceiver(MessageSentEvent event) {
        try {
            messagingTemplate.convertAndSendToUser(
                    event.getReceiverId().toString(),
                    "/queue/messages",
                    event
            );
            log.info("Message from sender {} pushed to receiver {}", event.getSenderId(), event.getReceiverId());
        } catch (Exception e) {
            log.error("Error pushing message to receiver: {}", event.getReceiverId(), e);
        }
    }
}
