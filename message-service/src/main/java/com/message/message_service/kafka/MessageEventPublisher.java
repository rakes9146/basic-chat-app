package com.message.message_service.kafka;

import com.message.message_service.event.MessageDeliveredEvent;
import com.message.message_service.event.MessageReadEvent;
import com.message.message_service.event.MessageSentEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import java.util.concurrent.CompletableFuture;
import org.springframework.kafka.support.SendResult;

@Service
public class MessageEventPublisher {
    private static final Logger log = LoggerFactory.getLogger(MessageEventPublisher.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public MessageEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    private void sendWithLogging(String topic, Object event) {
        try {
            CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(topic, event);
            future.whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("Failed to publish event to topic='{}'. event={}", topic, event, ex);
                } else {
                    if (result != null && result.getRecordMetadata() != null) {
                        log.info("Published event to topic='{}' partition={} offset={} event={}",
                                result.getRecordMetadata().topic(),
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset(),
                                event);
                    } else {
                        log.info("Published event to topic='{}'. event={}", topic, event);
                    }
                }
            });
        } catch (Exception e) {
            log.error("Exception while sending event to Kafka topic='{}'. event={}", topic, event, e);
        }
    }

    public void publishMessageSent(MessageSentEvent event) {
        sendWithLogging("chat.message.sent", event);
    }

    public void publishDelivered(MessageDeliveredEvent event) {

        sendWithLogging("chat.message.delivered", event);
    }

    public void publishRead(MessageReadEvent event) {
        sendWithLogging("chat.message.read", event);
    }

}
