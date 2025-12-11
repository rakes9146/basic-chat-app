package com.message.message_service.web.data;

public class ChatActivityDto {
    private Long userId;
    private Long peerId;
    private boolean active;

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Long getPeerId() { return peerId; }
    public void setPeerId(Long peerId) { this.peerId = peerId; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
