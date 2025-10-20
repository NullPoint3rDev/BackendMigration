//package org.alloy.config;
//
//import org.springframework.context.annotation.Configuration;
//import org.springframework.messaging.simp.config.MessageBrokerRegistry;
//import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
//import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
//import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
//
//@Configuration
//@EnableWebSocketMessageBroker
//public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
//
//    @Override
//    public void configureMessageBroker(MessageBrokerRegistry config) {
//        config.enableSimpleBroker("/topic");
//        config.setApplicationDestinationPrefixes("/app");
//    }
//
//    @Override
//    public void registerStompEndpoints(StompEndpointRegistry registry) {
//        System.out.println("[WEBSOCKET-CONFIG] 🔧 Регистрация WebSocket endpoint: /ws");
//        registry.addEndpoint("/ws")
//                .setAllowedOriginPatterns("*")  // Используем patterns вместо origins
//                .withSockJS();
//        System.out.println("[WEBSOCKET-CONFIG] ✅ WebSocket endpoint зарегистрирован");
//    }
//}