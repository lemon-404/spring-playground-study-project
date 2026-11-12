package com.example.spring_playground.question.q5;




import com.example.spring_playground.question.q5.async.mock.MockApiServer;
import dev.failsafe.RateLimiter;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static com.example.spring_playground.question.q5.async.mock.ExecutorSupport.stopExecutor;

public class RateLimiterBasicTest {

    private static final long QPS = 2;
    private static final int REQUEST_COUNT = 10;

    public static void main(String[] args) throws Exception {

        // 실행할 테스트 선택
        try {
            test1();
            test2();
            test3();
            test4();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw e;
        }

    }

    // Case 1: 기본 RateLimiter
    private static void test1() throws Exception {
        runSyncTest("Case 1 - 기본", 0);
    }

    // Case 2: 200ms 작업 지연
    private static void test2() throws Exception {
        runSyncTest("Case 2 - 200ms 지연", 200);
    }

    // Case 3: 1200ms 작업 지연
    private static void test3() throws Exception {
        runSyncTest("Case 3 - 1200ms 지연", 1200);
    }

    // Case 4: 하나의 호출 스레드에서 비동기 HTTP 요청 제출
    private static void test4() throws Exception {
        runAsyncTest("Case 4 - 단일 호출 스레드 비동기 HTTP", 1200);
    }



    // 동기식은 응답 완료 후 다음 요청으로 진행한다.
    private static void runSyncTest(String title, long delayMillis) throws Exception {
        runHttpTest(title, delayMillis, false);
    }

    // 비동기식은 응답을 기다리지 않고 다음 요청을 제출한다.
    private static void runAsyncTest(String title, long delayMillis) throws Exception {
        runHttpTest(title, delayMillis, true);
    }

    // 모든 Case가 동일한 HTTP 설정과 서버 지연을 사용한다.
    private static void runHttpTest(String title, long delayMillis, boolean async) throws Exception {
        // 서버의 지연 대기와 Executor 종료는 공통 Mock 코드에서 관리한다.
        // Java 17 HttpClient 내부 작업용. 요청마다 블로킹 작업 스레드를 만들지 않는다.
        ExecutorService httpExecutor = Executors.newFixedThreadPool(4);
        MockApiServer server = null;
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        try {
            server = new MockApiServer(delayMillis, 0);
            HttpClient client = HttpClient.newBuilder()
                    .executor(httpExecutor)
                    .version(HttpClient.Version.HTTP_1_1)
                    .connectTimeout(Duration.ofSeconds(5))
                    .build();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(server.uri())
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build();
            System.out.println("\n========== " + title + " ==========");
            RateLimiter<Void> limiter = RateLimiter.<Void>smoothBuilder(QPS, Duration.ofSeconds(1)).build();
            long start = System.nanoTime();
            for (int i = 1; i <= REQUEST_COUNT; i++) {
                int taskNumber = i;
                // Failsafe는 대기시간을 반환하지 않으므로 실제 경과시간을 측정한다.
                long waitStart = System.nanoTime();
                limiter.acquirePermit();
                double waited = (System.nanoTime() - waitStart) / 1_000_000_000.0;
                // START는 send/sendAsync 호출 직전 시각이며, 실제 서버 도착 시각은 아니다.
                log(start, taskNumber, "START", waited);
                if (async) {
                    futures.add(client.sendAsync(request, HttpResponse.BodyHandlers.discarding())
                            .thenAccept(response -> {
                                if (response.statusCode() != 200) {
                                    throw new IllegalStateException("HTTP " + response.statusCode());
                                }
                                log(start, taskNumber, "END", 0);
                            }));
                } else {
                    // main은 서버의 응답을 기다린다. sleep은 서버에서만 실행한다.
                    HttpResponse<Void> response = client.send(request, HttpResponse.BodyHandlers.discarding());
                    if (response.statusCode() != 200) {
                        throw new IllegalStateException("HTTP " + response.statusCode());
                    }
                    log(start, taskNumber, "END", 0);
                }
            }
            CompletableFuture.allOf(futures.toArray(CompletableFuture<?>[]::new)).get();
            printTotal(start);
        } finally {
            futures.forEach(future -> future.cancel(true));

            // Java 17의 HttpClient는 close()가 없으므로 소유한 Executor를 직접 종료한다.
            try {
                if (server != null) server.close();
            } finally {
                stopExecutor(httpExecutor);
            }
        }
    }

    // 로그 출력
    private static void log(
            long start,
            int taskNumber,
            String status,
            double waited
    ) {
        double elapsed =
                (System.nanoTime() - start) / 1_000_000_000.0;

        System.out.printf(
                "[%7.3fs] Task %02d | %-5s | wait=%.3fs | %s%n",
                elapsed,
                taskNumber,
                status,
                waited,
                Thread.currentThread().getName()
        );
    }

    // 총 실행 시간
    private static void printTotal(long start) {

        double elapsed =
                (System.nanoTime() - start) / 1_000_000_000.0;

        System.out.printf(
                "총 실행 시간: %.3f초%n",
                elapsed
        );
    }
}

