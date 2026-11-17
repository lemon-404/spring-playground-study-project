package com.example.spring_playground.question.q6.async.basic.java11;

import com.example.spring_playground.question.q6.async.basic.cafe.CafeSystem;
import com.example.spring_playground.question.q6.async.basic.cafe.Order;
import com.example.spring_playground.question.q6.async.basic.cafe.OrderDb;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Java 11의 표준 HttpClient.sendAsync()로 주문 제작을 요청하는 예제. */
public class Java11AsyncMain {

    private static final Logger log = LoggerFactory.getLogger(Java11AsyncMain.class);

    /*
     * 주문 직원: 주문 접수 → HTTP로 제작 요청 → 응답 후 알림 처리 등록 → 다음 주문 접수
     * 모의 서버의 제작 직원: 커피 제작 → 제작 완료 결과를 HTTP 응답으로 전송
     * 클라이언트: 각 응답 도착 → "n번 나왔습니다." 알림
     * 주문 직원: 모든 응답과 알림 완료 확인 → 영업 종료
     *
     * Java 8 예제: supplyAsync()로 같은 프로세스 안의 제작 작업을 직접 맡긴다.
     * Java 11 예제: sendAsync()로 HTTP 요청을 보내고 응답을 CompletableFuture로 받는다.
     * thenAccept()와 allOf(...).join()을 사용하는 결과 처리 방식은 동일하다.
     * HTTP 오류 상태도 응답이므로 상태 코드를 직접 확인한다.
     */
    public static void main(String[] args) throws Exception {
        CafeSystem cafe = new CafeSystem();
        OrderDb orderDb = new OrderDb();
        HttpClient client = HttpClient.newHttpClient();
        List<CompletableFuture<Void>> results = new ArrayList<>();

        // 모의 서버 준비 부분. main 한 번만 실행하면 서버도 함께 시작하고 종료한다.
        try (CafeHttpServer server = new CafeHttpServer(cafe, orderDb)) {
            server.start();

            for (Order order : orderDb.findAll()) {
                cafe.receiveOrder(order);
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(server.baseUrl() + "/orders/" + order.orderNumber()))
                        .timeout(Duration.ofSeconds(15))
                        .POST(HttpRequest.BodyPublishers.noBody())
                        .build();

                log.info("[main] 주문 {}번 HTTP 요청, 응답을 기다리지 않고 다음 주문 접수 가능",
                        order.orderNumber());
                CompletableFuture<HttpResponse<String>> response = client.sendAsync(
                        request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

                CompletableFuture<Void> result = response.thenAccept(reply -> {
                    if (reply.statusCode() != 200) {
                        throw new IllegalStateException("제작 요청 실패: HTTP " + reply.statusCode());
                    }
                    log.info("[HTTP 완료 알림] 주문 {}번 나왔습니다. 응답={}, thread={}",
                            order.orderNumber(), reply.body(), Thread.currentThread().getName());
                });
                results.add(result);
            }

            log.info("[main] 모든 주문 요청 완료, 전체 응답 및 알림 완료 대기");
            CompletableFuture.allOf(results.toArray(new CompletableFuture<?>[0])).join();
            log.info("[main] 모든 주문 결과 처리 완료, 영업 종료");
        }
    }
}
