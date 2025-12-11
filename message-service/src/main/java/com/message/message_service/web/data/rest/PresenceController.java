package com.message.message_service.web.data.rest;

import com.message.message_service.event.UserPresenceEvent;
import com.message.message_service.service.PresenseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

@Controller
public class PresenceController {

    private static final Logger log = LoggerFactory.getLogger(PresenceController.class);
    private final PresenseService presenseService;

    public PresenceController(PresenseService presenseService) {
        this.presenseService = presenseService;
    }

    @MessageMapping("/presence")
    public void handlePresence(UserPresenceEvent event) {
        try {
            if (event == null || event.getUserId() == null) return;
            if (event.isOnline()) {
                presenseService.setOnline(event.getUserId());
                log.info("Set user {} online via STOMP presence", event.getUserId());
            } else {
                presenseService.setOffline(event.getUserId());
                log.info("Set user {} offline via STOMP presence", event.getUserId());
            }
        } catch (Exception e) {
            log.error("Error handling presence event: {}", event, e);
        }
    }
}
