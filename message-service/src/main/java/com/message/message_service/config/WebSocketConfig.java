package com.message.message_service.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import java.io.IOException;
import java.util.Map;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    // Note: STOMP configuration (WebSocketMessageBrokerConfigurer) provides
    // `clientInboundChannel` and `SimpMessagingTemplate`. Do not define them here
    // to avoid bean name conflicts.

    @Bean
    public ChatWebSocketHandler chatWebSocketHandler() {
        return new ChatWebSocketHandler();
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // Remove CORS - Gateway handles all CORS
        registry.addHandler(chatWebSocketHandler(), "/ws-chat");
    }

    public static class ChatWebSocketHandler extends TextWebSocketHandler {
        private final ObjectMapper objectMapper = new ObjectMapper();

        @Override
        protected void handleTextMessage(WebSocketSession session, TextMessage message) throws IOException {
            try {
                String payload = message.getPayload();
                System.out.println("Received: " + payload);

                // Try to parse as JSON
                try {
                    Map<String, Object> jsonData = objectMapper.readValue(payload, new TypeReference<Map<String, Object>>(){});
                    System.out.println("Parsed JSON: " + jsonData);
                    
                    // Send back JSON response
                    Map<String, Object> response = Map.of(
                        "status", "success",
                        "message", "Message received",
                        "data", jsonData
                    );
                    String jsonResponse = objectMapper.writeValueAsString(response);
                    session.sendMessage(new TextMessage(jsonResponse));
                } catch (com.fasterxml.jackson.core.JsonParseException e) {
                    // If not JSON, treat as plain text
                    System.out.println("Not valid JSON, treating as plain text");
                    session.sendMessage(new TextMessage("Echo: " + payload));
                }
            } catch (IOException e) {
                System.err.println("Error sending WebSocket message: " + e.getMessage());
                throw e;
            }
        }

        @Override
        public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
            System.err.println("WebSocket error for session " + session.getId() + ": " + exception.getMessage());
        }

        @Override
        public void afterConnectionClosed(WebSocketSession session, org.springframework.web.socket.CloseStatus closeStatus) throws Exception {
            System.out.println("WebSocket connection closed for session: " + session.getId());
        }
    }
}
