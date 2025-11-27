package kr.ac.ewha.catchMind.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import kr.ac.ewha.catchMind.handler.GameSocketHandler;

@Configuration
@EnableWebSocket	// 웹소켓 기능 on
public class WebSocketConfig implements WebSocketConfigurer {

    private final GameSocketHandler gameSocketHandler;

    // 생성자 주입
    public WebSocketConfig(GameSocketHandler gameSocketHandler) {
        this.gameSocketHandler = gameSocketHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // 클라이언트가 /ws/game 주소로 접속 > gameSocketHandler가 처리
        registry.addHandler(gameSocketHandler, "/ws/game")
                .setAllowedOrigins("*"); // 모든 곳에서 접속 허용(CORS 오류 방지)
    }
}