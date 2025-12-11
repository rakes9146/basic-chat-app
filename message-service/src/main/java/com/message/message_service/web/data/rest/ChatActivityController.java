package com.message.message_service.web.data.rest;

import com.message.message_service.service.ChatActivityService;
import com.message.message_service.web.data.ChatActivityDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

@Controller
public class ChatActivityController {

    private static final Logger log = LoggerFactory.getLogger(ChatActivityController.class);
    private final ChatActivityService chatActivityService;

    public ChatActivityController(ChatActivityService chatActivityService) {
        this.chatActivityService = chatActivityService;
    }

    @MessageMapping("/chat/active")
    public void handle(@Payload ChatActivityDto dto) {
        try {
            if (dto == null || dto.getUserId() == null) return;
            if (dto.isActive()) {
                chatActivityService.setActive(dto.getUserId(), dto.getPeerId());
                log.info("[CHAT-ACTIVITY] User {} active with peer {}", dto.getUserId(), dto.getPeerId());
            } else {
                chatActivityService.clearActive(dto.getUserId());
                log.info("[CHAT-ACTIVITY] User {} cleared active chat", dto.getUserId());
            }
        } catch (Exception e) {
            log.error("Error handling chat activity: {}", dto, e);
        }
    }
}
