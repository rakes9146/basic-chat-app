package com.message.message_service.web.data.rest;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.message.message_service.service.MessageService;
import com.message.message_service.web.data.MessageDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MessageController.class)
class MessageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MessageService messageService;

    private MessageDto sampleMessage;

    @BeforeEach
    void setup() {
        sampleMessage = new MessageDto();
        sampleMessage.setMessageId(1L);
        sampleMessage.setMessageText("Hello!");
        sampleMessage.setSenderId(100L);
        sampleMessage.setReceiverId(200L);
        sampleMessage.setDelivered(false);
        sampleMessage.setRead(false);
    }

    @Test
    void testSaveMessage() throws Exception {
        Mockito.when(messageService.saveNewMessage(Mockito.any(MessageDto.class)))
                .thenReturn(sampleMessage);

        mockMvc.perform(post("/message")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(sampleMessage)))
                .andExpect(status().isCreated())
                .andExpect(content().string("Message Created"));

        Mockito.verify(messageService, Mockito.times(1)).saveNewMessage(Mockito.any(MessageDto.class));
    }

    @Test
    void testGetMessages() throws Exception {
        Mockito.when(messageService.getMessageBySenderAndReceiverId(100L, 200L))
                .thenReturn(List.of(sampleMessage));

        mockMvc.perform(get("/message")
                        .param("senderId", "100")
                        .param("receiverId", "200"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].messageId").value(1L))
                .andExpect(jsonPath("$[0].messageText").value("Hello!"))
                .andExpect(jsonPath("$[0].senderId").value(100L))
                .andExpect(jsonPath("$[0].receiverId").value(200L));
    }

    @Test
    void testUpdateDeliveryStatus() throws Exception {
        mockMvc.perform(put("/message/1/delivery"))
                .andExpect(status().isOk())
                .andExpect(content().string("Message Updated"));

        Mockito.verify(messageService).updateMessageDeliveryStatus(1L);
    }

    @Test
    void testUpdateReadStatus() throws Exception {
        mockMvc.perform(put("/message/1/read"))
                .andExpect(status().isOk())
                .andExpect(content().string("Message Read"));

        Mockito.verify(messageService).updateMessageReadStatus(1L);
    }
}
