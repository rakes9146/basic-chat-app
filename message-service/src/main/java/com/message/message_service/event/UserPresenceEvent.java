package com.message.message_service.event;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserPresenceEvent {
    private Long userId;
    private boolean online;
    private LocalDateTime timestamp;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public boolean isOnline() {
        return online;
    }

    public void setOnline(boolean online) {
        this.online = online;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}

