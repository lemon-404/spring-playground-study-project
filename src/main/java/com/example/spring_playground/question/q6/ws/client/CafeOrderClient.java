package com.example.spring_playground.question.q6.ws.client;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.example.spring_playground.question.q6.async.basic.cafe.Order;
import com.example.spring_playground.question.q6.async.basic.cafe.OrderDb;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 실험 1: 연결 하나에서 주문 번호를 보내고 제작 완료 응답을 받은 뒤 종료한다.
 *
 * WebSocket의 기본 흐름을 이해하기 위해 main은 주문을 하나씩 순차 처리한다.
 * 연결·송신에는 JDK의 비동기 API를 사용하고 수신은 onText() 콜백으로 전달받지만,
 * main은 get()과 received.poll()로 기다린 뒤 다음 주문을 보낸다.
 * 따라서 비동기 통신 API를 사용하면서 호출 흐름은 블로킹 방식으로 구성한 예제다.
 *
 * WebSocket의 양방향 통신은 양쪽이 독립적으로 메시지를 보낼 수 있다는 뜻이며,
 * 반드시 비동기 API로 구현해야 한다는 뜻은 아니다.
 * 별도 송신·수신 스레드에서 블로킹 API를 사용하는 구현도 가능하다.
 */
public class CafeOrderClient {

    private static final Logger log = LoggerFactory.getLogger(CafeOrderClient.class);

    public static void main(String[] args) throws Exception {
        connectAndExchange("ws://localhost:8080/q6/ws");
    }

    public static List<String> connectAndExchange(String url) throws Exception {
        var received = new LinkedBlockingQueue<String>();
        var closed = new CompletableFuture<Integer>();

        log.info("[실험 1] 연결 요청: {}", url);
        WebSocket socket = HttpClient.newHttpClient().newWebSocketBuilder()
                .buildAsync(URI.create(url), new WebSocket.Listener() {
                    private final StringBuilder fragments = new StringBuilder();

                    @Override
                    public void onOpen(WebSocket webSocket) {
                        log.info("[실험 1] 연결 완료, thread={}", Thread.currentThread().getName());
                        webSocket.request(1);
                    }

                    @Override
                    public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
                        // 텍스트 하나가 여러 조각으로 도착할 수 있어 마지막 조각까지 모은다.
                        fragments.append(data);
                        if (last) {
                            String text = fragments.toString();
                            log.info("[실험 1] 텍스트 수신: {}, thread={}",
                                    text, Thread.currentThread().getName());
                            received.add(text);
                            fragments.setLength(0);
                        }
                        webSocket.request(1);
                        return null;
                    }

                    @Override
                    public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
                        log.info("[실험 1] [WS] 연결 종료: code={}, reason={}", statusCode, reason);
                        closed.complete(statusCode);
                        return null;
                    }

                    @Override
                    public void onError(WebSocket webSocket, Throwable error) {
                        log.info("[실험 1] 통신 오류", error);
                        closed.completeExceptionally(error);
                    }
                }).get(5, TimeUnit.SECONDS);

        try {
            var replies = new ArrayList<String>();
            for (Order order : new OrderDb().findAll()) {
                // 학습용 계약: 주문 번호만 전송하고 서버가 공통 DB에서 메뉴를 조회한다.
                String text = Integer.toString(order.orderNumber());
                log.info("[주문 직원] 주문 {}번 전송: {}", order.orderNumber(), order.items());
                socket.sendText(text, true).get(5, TimeUnit.SECONDS);

                // 서버가 보내는 제작 완료 응답 대기
                String reply = received.poll(10, TimeUnit.SECONDS);
                if (reply == null) {
                    throw new TimeoutException("커피 제작 응답이 10초 안에 도착하지 않았습니다.");
                }
                log.info("[주문 직원] 주문 {}번 나왔습니다. 응답={}", order.orderNumber(), reply);
                replies.add(reply);
            }

            log.info("[실험 1] 정상 종료 요청");
            socket.sendClose(WebSocket.NORMAL_CLOSURE, "실험 1 종료").get(5, TimeUnit.SECONDS);
            int status = closed.get(5, TimeUnit.SECONDS);
            log.info("Stats:{}",status);
            if (status != WebSocket.NORMAL_CLOSURE) {
                throw new IllegalStateException("정상 종료가 아닙니다: " + status);
            }
            return replies;
        } finally {
            socket.abort();
        }
    }
}
