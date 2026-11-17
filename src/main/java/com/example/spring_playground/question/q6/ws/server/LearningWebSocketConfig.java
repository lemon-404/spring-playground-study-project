package com.example.spring_playground.question.q6.ws.server;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/** q6 학습 시에만 활성화되는 기본 WebSocket 서버 설정. */
@Configuration
@Profile("q6-websocket")
@EnableWebSocket
public class LearningWebSocketConfig implements WebSocketConfigurer {

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // ws://localhost:8080/q6/ws 로 연결하면 이 핸들러가 메시지를 받는다.
        registry.addHandler(new LearningWebSocketHandler(), "/q6/ws");
    }
}
