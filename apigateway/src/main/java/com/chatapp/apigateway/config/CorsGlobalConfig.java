package com.chatapp.apigateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

@Configuration
public class CorsGlobalConfig {

    @Bean
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration config = new CorsConfiguration();
        config.addAllowedOrigin("http://localhost:4200");
        config.addAllowedMethod("GET");
        config.addAllowedMethod("POST");
        config.addAllowedMethod("PUT");
        config.addAllowedMethod("PATCH");
        config.addAllowedMethod("DELETE");
        config.addAllowedMethod("OPTIONS");
        config.addAllowedHeader("*");
        config.setAllowCredentials(true);
        config.addExposedHeader("Authorization");
        config.addExposedHeader("Cache-Control");
        config.addExposedHeader("Content-Type");
        // Expose WebSocket/SockJS related headers for /ws/info and upgrade
        config.addExposedHeader("Upgrade");
        config.addExposedHeader("Connection");
        config.addExposedHeader("Sec-WebSocket-Accept");
        config.addExposedHeader("Sec-WebSocket-Version");
        config.addExposedHeader("Sec-WebSocket-Protocol");
        config.addExposedHeader("Sec-WebSocket-Extensions");

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return new CorsWebFilter(source);
    }
}
