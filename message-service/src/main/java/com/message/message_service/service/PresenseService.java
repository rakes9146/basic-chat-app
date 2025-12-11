package com.message.message_service.service;

import com.message.message_service.event.UserPresenceEvent;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class PresenseService {

    private final StringRedisTemplate redis;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public PresenseService(StringRedisTemplate redis, KafkaTemplate<String, Object> kafkaTemplate) {
        this.redis = redis;
        this.kafkaTemplate = kafkaTemplate;
    }

    public void setOnline(Long userId) {
        redis.opsForValue().set("presence:user:" + userId, "online");
        publishPresence(userId, true);
    }

    public void setOffline(Long userId) {
        redis.opsForValue().set("presence:user:" + userId, "offline");
        publishPresence(userId, false);
    }

    private void publishPresence(Long userId, boolean online) {
        UserPresenceEvent evt = new UserPresenceEvent();
        evt.setUserId(userId);
        evt.setOnline(online);
        evt.setLastSeen(Instant.now());
        kafkaTemplate.send("chat.user.presence", evt);
    }

    public boolean isOnline(Long userId) {
        return "online".equals(redis.opsForValue().get("presence:user:" + userId));
    }

}
