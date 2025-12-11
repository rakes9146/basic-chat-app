package com.message.message_service.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class ChatActivityService {

    private final StringRedisTemplate redis;

    public ChatActivityService(StringRedisTemplate redis) {
        this.redis = redis;
    }

    private String key(Long userId) {
        return "chat:active:user:" + userId;
    }

    public void setActive(Long userId, Long peerId) {
        if (userId == null || peerId == null) return;
        redis.opsForValue().set(key(userId), String.valueOf(peerId));
    }

    public void clearActive(Long userId) {
        if (userId == null) return;
        redis.delete(key(userId));
    }

    public boolean isActiveWith(Long userId, Long peerId) {
        if (userId == null || peerId == null) return false;
        String val = redis.opsForValue().get(key(userId));
        return String.valueOf(peerId).equals(val);
    }
}
