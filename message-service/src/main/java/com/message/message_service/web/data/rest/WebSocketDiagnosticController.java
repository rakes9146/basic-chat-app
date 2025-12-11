package com.message.message_service.web.data.rest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.user.SimpUser;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Diagnostic controller to check WebSocket connection status
 */
@RestController
@RequestMapping("/api/websocket")
public class WebSocketDiagnosticController {

    private static final Logger log = LoggerFactory.getLogger(WebSocketDiagnosticController.class);
    private final SimpUserRegistry userRegistry;

    public WebSocketDiagnosticController(SimpUserRegistry userRegistry) {
        this.userRegistry = userRegistry;
    }

    /**
     * Get all active WebSocket users
     * Endpoint: GET /api/websocket/active-users
     */
    @GetMapping("/active-users")
    public Map<String, Object> getActiveUsers() {
        log.info("[DIAGNOSTIC] 🔍 Checking active WebSocket users");
        
        Set<SimpUser> users = userRegistry.getUsers();
        int totalUsers = users.size();
        
        log.info("[DIAGNOSTIC] Found {} active WebSocket users", totalUsers);
        
        List<Map<String, Object>> userDetails = users.stream()
            .map(user -> {
                Map<String, Object> details = new HashMap<>();
                details.put("userId", user.getName());
                details.put("sessionCount", user.getSessions().size());
                details.put("sessionIds", user.getSessions().stream()
                    .map(session -> session.getId())
                    .collect(Collectors.toList()));
                
                log.info("[DIAGNOSTIC] 👤 User: {} | Sessions: {} | IDs: {}", 
                    user.getName(), 
                    user.getSessions().size(),
                    details.get("sessionIds"));
                
                return details;
            })
            .collect(Collectors.toList());
        
        Map<String, Object> response = new HashMap<>();
        response.put("totalUsers", totalUsers);
        response.put("users", userDetails);
        response.put("timestamp", System.currentTimeMillis());
        
        log.info("[DIAGNOSTIC] ✅ Response prepared with {} users", totalUsers);
        
        return response;
    }

    /**
     * Check if a specific user is connected
     * Endpoint: GET /api/websocket/check-user/{userId}
     */
    @GetMapping("/check-user/{userId}")
    public Map<String, Object> checkUser(String userId) {
        log.info("[DIAGNOSTIC] 🔍 Checking if user {} is connected", userId);
        
        SimpUser user = userRegistry.getUser(userId);
        boolean isConnected = user != null;
        int sessionCount = isConnected ? user.getSessions().size() : 0;
        
        Map<String, Object> response = new HashMap<>();
        response.put("userId", userId);
        response.put("connected", isConnected);
        response.put("sessionCount", sessionCount);
        
        if (isConnected) {
            response.put("sessionIds", user.getSessions().stream()
                .map(session -> session.getId())
                .collect(Collectors.toList()));
            log.info("[DIAGNOSTIC] ✅ User {} is CONNECTED with {} sessions", userId, sessionCount);
        } else {
            log.info("[DIAGNOSTIC] ❌ User {} is NOT CONNECTED", userId);
        }
        
        return response;
    }

    /**
     * Get summary statistics
     * Endpoint: GET /api/websocket/stats
     */
    @GetMapping("/stats")
    public Map<String, Object> getStats() {
        log.info("[DIAGNOSTIC] 📊 Getting WebSocket statistics");
        
        Set<SimpUser> users = userRegistry.getUsers();
        int totalUsers = users.size();
        int totalSessions = users.stream()
            .mapToInt(user -> user.getSessions().size())
            .sum();
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalConnectedUsers", totalUsers);
        stats.put("totalActiveSessions", totalSessions);
        stats.put("averageSessionsPerUser", totalUsers > 0 ? (double) totalSessions / totalUsers : 0);
        stats.put("userIds", users.stream()
            .map(SimpUser::getName)
            .collect(Collectors.toList()));
        stats.put("timestamp", System.currentTimeMillis());
        
        log.info("[DIAGNOSTIC] 📊 Stats: {} users, {} sessions", totalUsers, totalSessions);
        
        return stats;
    }
}
