package com.example.spring_playground.question.q6.async.basic.java11;

import com.example.spring_playground.question.q6.async.basic.cafe.CafeSystem;
import com.example.spring_playground.question.q6.async.basic.cafe.Order;
import com.example.spring_playground.question.q6.async.basic.cafe.OrderDb;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Java 11 학습용 모의 HTTP 서버. 실제 외부 API의 계약을 재현한 것은 아니다. */
public class CafeHttpServer implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(CafeHttpServer.class);

    private final HttpServer server;
    private final ExecutorService workers = Executors.newFixedThreadPool(2);

    public CafeHttpServer(CafeSystem cafe, OrderDb orderDb) throws IOException {
        // 로컬에서만 접속하고, 사용 가능한 포트를 자동으로 배정받는다.
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.setExecutor(workers);
        server.createContext("/orders/", exchange -> {
            try (exchange) {
                // 학습용 계약: POST /orders/{주문번호}, 메뉴는 공통 OrderDb에서 조회한다.
                if (!"POST".equals(exchange.getRequestMethod())) {
                    respond(exchange, 405, "POST 요청을 사용하세요.");
                    return;
                }
                try {
                    int number = Integer.parseInt(exchange.getRequestURI().getPath()
                            .substring("/orders/".length()));
                    Order order = orderDb.findAll().stream()
                            .filter(candidate -> candidate.orderNumber() == number)
                            .findFirst().orElseThrow(() -> new IllegalArgumentException("없는 주문 번호"));

                    log.info("[모의 서버] 주문 {}번 HTTP 요청 수신, thread={}",
                            number, Thread.currentThread().getName());
                    String result = cafe.prepareOrder(order);
                    respond(exchange, 200, result);
                } catch (IllegalArgumentException e) {
                    respond(exchange, 400, "주문 번호를 확인하세요.");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    respond(exchange, 503, "제작이 중단됐습니다.");
                }
            }
        });
    }

    public void start() {
        server.start();
        log.info("[모의 서버] 시작: {}", baseUrl());
    }

    public String baseUrl() {
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    private void respond(HttpExchange exchange, int status, String text) throws IOException {
        byte[] body = text.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8");
        exchange.sendResponseHeaders(status, body.length);
        exchange.getResponseBody().write(body);
    }

    @Override
    public void close() {
        server.stop(0);
        workers.shutdownNow();
        log.info("[모의 서버] 종료");
    }
}
