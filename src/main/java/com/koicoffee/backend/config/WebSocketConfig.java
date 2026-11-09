package com.koicoffee.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private static final String[] ALLOWED_ORIGIN_PATTERNS = {
            "http://localhost:[*]",
            "http://127.0.0.1:[*]",
            "http://localhost:5173",
            "http://localhost:3000",
            "http://localhost:8080",
            "https://koicoffeepos.vercel.app",
            "https://*.vercel.app",
            "https://koicoffee.id.vn",
            "https://www.koicoffee.id.vn",
            "https://*koicoffee*"
    };

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // SockJS endpoint for fallback and compatibility
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns(ALLOWED_ORIGIN_PATTERNS)
                .withSockJS();

        // Direct WebSocket endpoint without SockJS
        registry.addEndpoint("/ws-native")
                .setAllowedOriginPatterns(ALLOWED_ORIGIN_PATTERNS);
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Broker destinations: /topic for broadcasts, /queue for point-to-point
        registry.enableSimpleBroker("/topic", "/queue");
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void configureWebSocketTransport(WebSocketTransportRegistration registration) {
        // Buffer & message limits to prevent slow-client memory leaks
        registration.setMessageSizeLimit(128 * 1024); // 128KB
        registration.setSendBufferSizeLimit(512 * 1024); // 512KB
        registration.setSendTimeLimit(20 * 1000); // 20s
    }
}
