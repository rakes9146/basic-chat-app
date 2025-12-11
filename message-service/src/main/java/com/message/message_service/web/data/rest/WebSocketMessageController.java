package com.message.message_service.web.data.rest;

import com.message.message_service.service.MessageService;
import com.message.message_service.web.data.MessageDto;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

import java.util.List;

@Controller
public class WebSocketMessageController {

    private final MessageService messageService;

    public WebSocketMessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    @MessageMapping("/send")
    @SendTo("/topic/messages")
    public MessageDto saveMessage(MessageDto messageDto) {
        return messageService.saveNewMessage(messageDto);
    }

    @MessageMapping("/fetch")
    @SendTo("/topic/fetch")
    public List<MessageDto> getMessages(MessageDto queryDto) {
        return messageService.getMessageBySenderAndReceiverId(
                queryDto.getSenderId(), queryDto.getReceiverId()
        );
    }

    @MessageMapping("/delivery")
    public void updateDeliveryStatus(Long messageId) {
        messageService.updateMessageDeliveryStatus(messageId);
    }

    @MessageMapping("/read")
    public void updateReadStatus(Long messageId) {
        messageService.updateMessageReadStatus(messageId);
    }
}
