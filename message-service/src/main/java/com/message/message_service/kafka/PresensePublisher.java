package com.message.message_service.kafka;

import com.message.message_service.event.UserPresenceEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class PresensePublisher {

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    public void publishPresence(UserPresenceEvent event) {
        kafkaTemplate.send("chat.user.presence", event);
    }




}
