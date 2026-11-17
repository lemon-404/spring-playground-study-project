package com.example.spring_playground.question.q6.ws.client;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** 서버를 먼저 실행한 후 이 클래스의 main()을 실행한다. */
public class ConnectAndPingClient {

    private static final Logger log = LoggerFactory.getLogger(ConnectAndPingClient.class);

    public static void main(String[] args) throws Exception {
        connectAndPing("ws://localhost:8080/q6/ws");
    }

    public static String connectAndPing(String url) throws Exception {
        // 콜백에서 받은 결과를 main 스레드에 전달한다. 별도 작업 풀은 만들지 않는다.
        var pongReceived = new CompletableFuture<String>();
        var connectionClosed = new CompletableFuture<Integer>();

        log.info("[클라이언트] 1. 연결 요청: {}", url);
        WebSocket socket = HttpClient.newHttpClient().newWebSocketBuilder()
                .buildAsync(URI.create(url), new WebSocket.Listener() {
                    @Override
                    public void onOpen(WebSocket webSocket) {
                        log.info("[클라이언트] 2. 연결 완료, thread={}", Thread.currentThread().getName());
                        // 수신 콜백을 받을 수 있도록 한 건을 요청한다.
                        webSocket.request(1);
                    }

                    @Override
                    public CompletionStage<?> onPong(WebSocket webSocket, ByteBuffer message) {
                        String payload = StandardCharsets.UTF_8.decode(message).toString();
                        log.info("[클라이언트] 4. Pong 수신: {}, thread={}",
                                payload, Thread.currentThread().getName());
                        webSocket.request(1);
                        pongReceived.complete(payload);
                        return null;
                    }

                    @Override
                    public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
                        log.info("[클라이언트] 6. 연결 종료: code={}, reason={}", statusCode, reason);
                        connectionClosed.complete(statusCode);
                        return null;
                    }

                    @Override
                    public void onError(WebSocket webSocket, Throwable error) {
                        log.info("[클라이언트] 통신 오류", error);
                        pongReceived.completeExceptionally(error);
                        connectionClosed.completeExceptionally(error);
                    }
                }).get(5, TimeUnit.SECONDS);

        try {
            // 문자열 "PING" 전송이 아니라 WebSocket 프로토콜의 Ping 프레임이다.
            log.info("[클라이언트] 3. 커피숍 연결 확인 Ping 전송: cafe-ping");
            socket.sendPing(StandardCharsets.UTF_8.encode("cafe-ping")).get(5, TimeUnit.SECONDS);

            // 임의의 sleep 대신 Pong이 도착할 때까지 최대 5초 기다린다.
            String pong = pongReceived.get(5, TimeUnit.SECONDS);
            log.info("[클라이언트] 5. 정상 종료 요청");
            socket.sendClose(WebSocket.NORMAL_CLOSURE, "학습 종료").get(5, TimeUnit.SECONDS);
            connectionClosed.get(5, TimeUnit.SECONDS);
            return pong;
        } finally {
            // 정상 종료했으면 효과가 없고, 실패한 경우에는 남은 연결을 정리한다.
            socket.abort();
        }
    }
}
