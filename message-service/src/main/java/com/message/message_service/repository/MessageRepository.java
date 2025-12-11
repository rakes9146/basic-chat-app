package com.message.message_service.repository;

import com.message.message_service.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    // Fetch messages in one direction only (not recommended for chat)
    List<Message> findBySenderIdAndReceiverId(Long senderId, Long receiverId);
    
    // Fetch ALL messages between two users (bidirectional conversation)
    @Query("SELECT m FROM Message m WHERE " +
           "(m.senderId = :userId1 AND m.receiverId = :userId2) OR " +
           "(m.senderId = :userId2 AND m.receiverId = :userId1) " +
           "ORDER BY m.createdDate ASC")
    List<Message> findConversationBetweenUsers(@Param("userId1") Long userId1, @Param("userId2") Long userId2);

    @Modifying
    @Transactional
    @Query("UPDATE Message m SET m.isRead = true where m.messageId = :messageId ")
    void markMessageAsRead(@Param("messageId") Long messageId);

    @Modifying
    @Transactional
    @Query("UPDATE Message m SET m.isDelivered = true where m.messageId = :messageId ")
    void markMessageAsDelivered(@Param("messageId") Long messageId);
    
    // Find unread messages from a specific sender to a specific receiver
    List<Message> findBySenderIdAndReceiverIdAndIsReadFalse(Long senderId, Long receiverId);
    
    // Find undelivered messages for a specific receiver
    List<Message> findByReceiverIdAndIsDeliveredFalse(Long receiverId);
}
