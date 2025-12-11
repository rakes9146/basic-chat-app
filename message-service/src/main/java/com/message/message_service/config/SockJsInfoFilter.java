package com.message.message_service.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Filter to handle SockJS /info endpoint requests properly
 * Prevents content-type negotiation issues with SockJS info requests
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SockJsInfoFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(SockJsInfoFilter.class);

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        
        String requestURI = httpRequest.getRequestURI();
        
        // Check if this is a SockJS info request
        if (requestURI != null && requestURI.contains("/ws/info")) {
            log.debug("[SOCKJS-FILTER] Processing SockJS info request: {}", requestURI);
            
            // Wrap response to prevent content-type issues
            ContentTypeResponseWrapper wrappedResponse = new ContentTypeResponseWrapper(httpResponse);
            
            try {
                chain.doFilter(request, wrappedResponse);
            } catch (Exception e) {
                log.error("[SOCKJS-FILTER] Error processing SockJS info request", e);
                
                // Return a simple error response for SockJS
                httpResponse.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                httpResponse.setContentType("application/json");
                httpResponse.getWriter().write("{\"error\":\"Internal server error\"}");
            }
        } else {
            // Normal request processing
            chain.doFilter(request, response);
        }
    }

    /**
     * Response wrapper to ensure proper content type for SockJS responses
     */
    private static class ContentTypeResponseWrapper extends jakarta.servlet.http.HttpServletResponseWrapper {
        
        public ContentTypeResponseWrapper(HttpServletResponse response) {
            super(response);
        }

        @Override
        public void setContentType(String type) {
            // Force application/json for error responses, otherwise allow the default
            if (type != null && type.contains("javascript") && getStatus() >= 400) {
                super.setContentType("application/json;charset=UTF-8");
            } else {
                super.setContentType(type);
            }
        }
    }
}
