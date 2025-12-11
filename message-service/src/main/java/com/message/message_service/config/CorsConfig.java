package com.message.message_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;

@Configuration
public class CorsConfig {

    @Bean
    public CorsFilter corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();
        
        // Allow credentials
        config.setAllowCredentials(true);
        
        // Allow all origins (for development)
        config.addAllowedOriginPattern("*");
        
        // Allow all headers
        config.addAllowedHeader("*");
        
        // Allow all methods
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        
        // Allow WebSocket upgrade headers
        config.addExposedHeader("Upgrade");
        config.addExposedHeader("Connection");
        config.addExposedHeader("Sec-WebSocket-Accept");
        config.addExposedHeader("Sec-WebSocket-Version");
        config.addExposedHeader("Sec-WebSocket-Protocol");
        config.addExposedHeader("Sec-WebSocket-Extensions");
        
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
}
