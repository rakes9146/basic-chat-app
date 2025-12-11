package com.message.message_service.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "message")
@Data
public class Message {

      @Id
      @GeneratedValue(strategy = GenerationType.AUTO)
      @Column(name ="message_id")
      private Long messageId;

      @Lob
      @Column(name="message_text")
      private String messageText;

      @Column(name = "sender_id")
      private Long senderId;

      @Column(name = "receiver_id")
      private Long receiverId;

     @Column(name = "is_delivered")
     private boolean isDelivered;

     @Column(name = "is_read")
     private boolean isRead;

     @Column(name="delivered_at")
     private LocalDateTime deliveredAt;

    @Column(name="read_at")
    private LocalDateTime readAt;
    
    @Column(name="created_date")
    @CreationTimestamp
    private LocalDateTime createdDate;


    public Long getMessageId() {
        return messageId;
    }

    public void setMessageId(Long messageId) {
        this.messageId = messageId;
    }

    public String getMessageText() {
        return messageText;
    }

    public void setMessageText(String messageText) {
        this.messageText = messageText;
    }

    public Long getSenderId() {
        return senderId;
    }

    public void setSenderId(Long senderId) {
        this.senderId = senderId;
    }

    public Long getReceiverId() {
        return receiverId;
    }

    public void setReceiverId(Long receiverId) {
        this.receiverId = receiverId;
    }

    public boolean isDelivered() {
        return isDelivered;
    }

    public void setDelivered(boolean delivered) {
        isDelivered = delivered;
    }

    public boolean isRead() {
        return isRead;
    }

    public void setRead(boolean read) {
        isRead = read;
    }

    public LocalDateTime getDeliveredAt() {
        return deliveredAt;
    }

    public void setDeliveredAt(LocalDateTime deliveredAt) {
        this.deliveredAt = deliveredAt;
    }

    public LocalDateTime getReadAt() {
        return readAt;
    }

    public void setReadAt(LocalDateTime readAt) {
        this.readAt = readAt;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(LocalDateTime createdDate) {
        this.createdDate = createdDate;
    }
}
