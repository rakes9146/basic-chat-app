package com.chatapp.apigateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Remove CORS headers added by backend services to prevent duplicates.
 * Gateway will add the proper CORS headers.
 */
@Component
public class RemoveBackendCorsHeadersFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return chain.filter(exchange).then(Mono.fromRunnable(() -> {
            HttpHeaders headers = exchange.getResponse().getHeaders();
            
            // Remove CORS headers that backend might have added
            // Gateway's global CORS will add them properly
            headers.remove(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN);
            headers.remove(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS);
            headers.remove(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS);
            headers.remove(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS);
            headers.remove(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS);
            headers.remove(HttpHeaders.ACCESS_CONTROL_MAX_AGE);
            headers.remove("Vary");
        }));
    }

    @Override
    public int getOrder() {
        // Run this filter before CORS headers are added by Gateway
        return Ordered.HIGHEST_PRECEDENCE + 1;
    }
}
