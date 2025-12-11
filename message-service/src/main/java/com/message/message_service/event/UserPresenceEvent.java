package com.message.message_service.event;

import lombok.Data;

import java.time.Instant;
import java.time.LocalDateTime;

@Data
public class UserPresenceEvent
{

    private Long userId;
    private boolean online;
    private Instant lastSeen;

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

    public Instant getLastSeen() {
        return lastSeen;
    }

    public void setLastSeen(Instant lastSeen) {
        this.lastSeen = lastSeen;
    }
}

