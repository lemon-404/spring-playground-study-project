package com.example.spring_playground.question.q5.async.mock;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/** 지정한 시간 뒤 응답하는 학습용 로컬 API. */
public final class MockApiServer implements AutoCloseable {
    private final HttpServer server;
    private final ExecutorService executor = Executors.newFixedThreadPool(10);

    public MockApiServer(long delayMillis, int responseBytes) throws IOException {
        this(delayMillis, responseBytes, 0);
    }

    // 서버 인스턴스 전체의 최초 N회 요청만 503으로 실패시킨다.
    public MockApiServer(long delayMillis, int responseBytes, int failFirstRequests) throws IOException {
        if (delayMillis < 0 || responseBytes < 0 || failFirstRequests < 0) throw new IllegalArgumentException();
        AtomicInteger requests = new AtomicInteger();
        byte[] body = new byte[responseBytes];
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.setExecutor(executor);
        server.createContext("/mock", exchange -> {
            int attempt = requests.incrementAndGet();
            try (exchange) {
                try {
                    // CPU 부하가 아닌 응답 대기시간을 모의한다.
                    if (delayMillis > 0) Thread.sleep(delayMillis);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
                // 서버 내부 예외는 클라이언트에 그대로 전달되지 않으므로 HTTP 오류로 모의한다.
                int status = attempt <= failFirstRequests ? 503 : 200;
                exchange.sendResponseHeaders(status, body.length == 0 ? -1 : body.length);
                if (body.length > 0) exchange.getResponseBody().write(body);
            }
        });
        server.start();
    }

    public URI uri() {
        return URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/mock");
    }

    @Override
    public void close() throws InterruptedException {
        server.stop(0);
        ExecutorSupport.stopExecutor(executor);
    }
}
