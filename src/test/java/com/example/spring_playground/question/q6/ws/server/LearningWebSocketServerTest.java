package com.example.spring_playground.question.q6.ws.server;

import java.util.List;
import java.time.Duration;

import com.example.spring_playground.question.q6.ws.client.CafeOrderClient;
import com.example.spring_playground.question.q6.ws.reactor.ReactorCafeClient;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ActiveProfiles("q6-websocket")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class LearningWebSocketServerTest {

    @LocalServerPort
    private int port;

    @Test
    void Reactor로_세_주문을_전송하고_제작_응답을_받는다() {
        List<String> replies = new ReactorCafeClient()
                .connectAndExchange("ws://localhost:" + port + "/q6/ws")
                .block(Duration.ofSeconds(30));

        assertEquals(List.of("주문 1번 전체 제작 완료", "주문 2번 전체 제작 완료", "주문 3번 전체 제작 완료"), replies);
    }

    @Test
    void 학습용_클라이언트가_연결하고_Ping의_응답을_받는다() throws Exception {
        String pong = com.example.spring_playground.question.q6.ws.client.ConnectAndPingClient
                .connectAndPing("ws://localhost:" + port + "/q6/ws");
        assertEquals("cafe-ping", pong);
    }

    @Test
    void 같은_연결에서_세_주문을_제작하고_정상_종료한다() throws Exception {
        List<String> replies = CafeOrderClient.connectAndExchange("ws://localhost:" + port + "/q6/ws");
        assertEquals(List.of("주문 1번 전체 제작 완료", "주문 2번 전체 제작 완료", "주문 3번 전체 제작 완료"), replies);
    }
}
