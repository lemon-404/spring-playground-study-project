package com.example.spring_playground.question.q5;

import com.example.spring_playground.question.q5.async.mock.MockApiServer;
import dev.failsafe.Failsafe;
import dev.failsafe.RetryPolicy;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static com.example.spring_playground.question.q5.async.mock.ExecutorSupport.stopExecutor;

/** HTTP 503을 예외로 변환하여 비동기 재시도의 성공/소진을 확인한다. */
public class AsncRetryTest {
    private static final int MAX_RETRIES = 3; // 최초 1회 + 재시도 3회 = 최대 4회 호출
    private static final Duration RETRY_DELAY = Duration.ofSeconds(1);

    public static void main(String[] args) throws Exception {
        try {
            run("Case 1 - 두 번 실패 후 성공", 2, true);
            run("Case 2 - 재시도를 모두 소진해도 실패", Integer.MAX_VALUE, false);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw e;
        }
    }

    private static void run(String title, int failFirstRequests, boolean expectSuccess) throws Exception {
        ExecutorService httpExecutor = Executors.newFixedThreadPool(4);
        CompletableFuture<HttpResponse<Void>> future = null;
        // 응답은 빠르게 반환한다. 1초 대기는 API 지연이 아니라 재시도 간격이다.
        try (MockApiServer server = new MockApiServer(0, 0, failFirstRequests)) {
            HttpClient client = HttpClient.newBuilder().executor(httpExecutor)
                    .version(HttpClient.Version.HTTP_1_1).connectTimeout(Duration.ofSeconds(5)).build();
            HttpRequest request = HttpRequest.newBuilder(server.uri())
                    .timeout(Duration.ofSeconds(5)).GET().build();
            AtomicInteger attempts = new AtomicInteger();
            long start = System.nanoTime();
            System.out.println("\n========== " + title + " ==========");

            RetryPolicy<HttpResponse<Void>> retryPolicy = RetryPolicy.<HttpResponse<Void>>builder()
                    .handle(RetryableApiException.class) // 이 실험은 HTTP 503만 재시도한다.
                    .withDelay(RETRY_DELAY)
                    .withMaxRetries(MAX_RETRIES)
                    .onRetry(event -> log(start, "RETRY: " + event.getAttemptCount() + "번째 재시도"))
                    .onRetriesExceeded(event -> log(start, "재시도 소진"))
                    .build();

            // getAsync(send())가 아니라 sendAsync()가 반환한 Future 자체를 정책으로 감싼다.
            // 매 재시도마다 supplier를 다시 실행하므로 실제 HTTP 요청이 다시 전송된다.
            future = Failsafe.with(retryPolicy).getStageAsync(() -> {
                int attempt = attempts.incrementAndGet();
                log(start, "START: 시도 " + attempt);
                return client.sendAsync(request, HttpResponse.BodyHandlers.discarding())
                        .thenApply(response -> {
                            log(start, "END: 시도 " + attempt + ", HTTP " + response.statusCode());
                            // HttpClient는 HTTP 503 자체를 예외로 처리하지 않는다.
                            if (response.statusCode() == 503) throw new RetryableApiException();
                            if (response.statusCode() != 200) {
                                throw new IllegalStateException("예상하지 않은 HTTP " + response.statusCode());
                            }
                            return response;
                        });
            });

            try {
                future.get(); // 모든 재시도를 포함한 최종 결과를 기다린다.
                if (!expectSuccess) throw new AssertionError("실패해야 하는 조건에서 성공했습니다.");
                if (attempts.get() != failFirstRequests + 1) throw new AssertionError("호출 횟수 불일치");
                log(start, "최종 성공: 총 " + attempts.get() + "회 호출, 일시적인 요청 실패에서 복구");
            } catch (ExecutionException e) {
                Throwable cause = e;
                while (cause.getCause() != null) cause = cause.getCause();
                if (expectSuccess || !(cause instanceof RetryableApiException)) throw e;
                if (attempts.get() != MAX_RETRIES + 1) throw new AssertionError("호출 횟수 불일치");
                log(start, "최종 실패: 총 " + attempts.get() + "회 호출, 복구되지 않음");
            }
        } finally {
            if (future != null) future.cancel(true);
            stopExecutor(httpExecutor);
        }
    }

    private static void log(long start, String message) {
        System.out.printf("[%6.3fs] %s | %s%n", (System.nanoTime() - start) / 1e9,
                message, Thread.currentThread().getName());
    }

    private static final class RetryableApiException extends RuntimeException {
        RetryableApiException() {
            super("Mock API가 의도적으로 HTTP 503을 반환했습니다.");
        }
    }
}
