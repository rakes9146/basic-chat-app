package com.chatapp.apigateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

/**
 * Removes duplicate CORS headers from backend responses.
 * Gateway's global CORS will add the correct headers.
 */
@Component
public class DedupeCorsFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return chain.filter(exchange).then(Mono.fromRunnable(() -> {
            HttpHeaders headers = exchange.getResponse().getHeaders();
            
            // Remove duplicate CORS headers by keeping only the first value
            deduplicate(headers, HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN);
            deduplicate(headers, HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS);
            deduplicate(headers, "Vary");
        }));
    }
    
    private void deduplicate(HttpHeaders headers, String headerName) {
        List<String> values = headers.get(headerName);
        if (values != null && values.size() > 1) {
            // Keep only first value
            String firstValue = values.get(0);
            headers.remove(headerName);
            headers.add(headerName, firstValue);
        }
    }

    @Override
    public int getOrder() {
        // Run AFTER the response comes back but BEFORE it's sent to client
        return Ordered.LOWEST_PRECEDENCE;
    }
}
