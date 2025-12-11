package com.message.message_service.web.data.rest;

import com.message.message_service.service.MessageService;
import com.message.message_service.web.data.MessageDto;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/message")
public class MessageController {

    private static final Logger log = LoggerFactory.getLogger(MessageController.class);
    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    @PostMapping(produces = "application/json", consumes = "application/json")
    public ResponseEntity<?> saveMessage(@Valid @RequestBody MessageDto messageDto) {
        try {
            if (messageDto == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Message body cannot be empty"));
            }
            
            log.info("[CONTROLLER] Received message - SenderId: {}, ReceiverId: {}, Text: {}", 
                    messageDto.getSenderId(), messageDto.getReceiverId(), messageDto.getMessageText());
            
            MessageDto savedMessage = messageService.saveNewMessage(messageDto);
            log.info("[CONTROLLER] Message created successfully - ID: {}, SenderId: {}, ReceiverId: {}", 
                    savedMessage.getMessageId(), savedMessage.getSenderId(), savedMessage.getReceiverId());
            return new ResponseEntity<>(Map.of(
                    "status", "success",
                    "message", "Message created successfully",
                    "messageId", savedMessage.getMessageId()
            ), HttpStatus.CREATED);
        } catch (Exception e) {
            log.error("Error saving message", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to save message: " + e.getMessage()));
        }
    }

    @GetMapping(produces = "application/json")
    public ResponseEntity<?> getMessages(
            @RequestParam("senderId") Long senderId,
            @RequestParam("receiverId") Long receiverId) {
        try {
            if (senderId == null || receiverId == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "senderId and receiverId are required"));
            }
            
            List<MessageDto> messages = messageService.getMessageBySenderAndReceiverId(senderId, receiverId);
            log.info("Retrieved {} messages", messages.size());
            return ResponseEntity.ok(messages);
        } catch (Exception e) {
            log.error("Error retrieving messages", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to retrieve messages: " + e.getMessage()));
        }
    }

    @PutMapping(value = "/{messageId}/delivery", produces = "application/json")
    public ResponseEntity<?> updateDeliveryStatus(@PathVariable("messageId") Long messageId) {
        try {
            if (messageId == null || messageId <= 0) {
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid message ID"));
            }
            
            messageService.updateMessageDeliveryStatus(messageId);
            log.info("Delivery status updated for message: {}", messageId);
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Message marked as delivered"
            ));
        } catch (Exception e) {
            log.error("Error updating delivery status", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to update delivery status: " + e.getMessage()));
        }
    }

    @PutMapping(value = "/{messageId}/read", produces = "application/json")
    public ResponseEntity<?> updateReadStatus(@PathVariable("messageId") Long messageId) {
        try {
            if (messageId == null || messageId <= 0) {
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid message ID"));
            }
            
            messageService.updateMessageReadStatus(messageId);
            log.info("Read status updated for message: {}", messageId);
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Message marked as read"
            ));
        } catch (Exception e) {
            log.error("Error updating read status", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to update read status: " + e.getMessage()));
        }
    }
}
