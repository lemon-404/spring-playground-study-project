package com.example.spring_playground.question.q5;

import com.example.spring_playground.question.q5.async.mock.MockApiServer;
import dev.failsafe.RateLimiter;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.example.spring_playground.question.q5.async.mock.ExecutorSupport.stopExecutor;

/** 동일한 서버에 동기식으로 요청하면서 Guava SmoothBursty와 Failsafe Smooth를 비교한다. */
public class SyncRateLimiterComparisonTest {
    private static final int REQUEST_COUNT = 10;
    private static final long QPS = 5;
    private static final long IDLE_MILLIS = 2000;
    private static final long[] RESPONSE_DELAYS = {100, 300};

    public static void main(String[] args) throws Exception {
        List<Result> results = new ArrayList<>();
        try {
            for (long delay : RESPONSE_DELAYS) {
                ExecutorService httpExecutor = Executors.newFixedThreadPool(4);
                try (MockApiServer server = new MockApiServer(delay, 0)) {
                    HttpClient client = HttpClient.newBuilder().executor(httpExecutor)
                            .version(HttpClient.Version.HTTP_1_1).connectTimeout(Duration.ofSeconds(5)).build();
                    HttpRequest request = HttpRequest.newBuilder(server.uri())
                            .timeout(Duration.ofSeconds(5)).GET().build();
                    // 연결 초기화는 측정 전에 수행한다. 실제 실험은 모두 같은 요청을 사용한다.
                    checkResponse(client.send(request, HttpResponse.BodyHandlers.discarding()));
                    for (boolean idle : new boolean[]{false, true}) {
                        results.add(run(client, request, delay, idle, true));
                        results.add(run(client, request, delay, idle, false));
                    }
                } finally {
                    stopExecutor(httpExecutor);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw e;
        }
        System.out.println("\n=== SUMMARY (start interval excludes response completion of last request) ===");
        System.out.println("Limiter | server delay | idle | first interval | avg interval | total");
        for (Result r : results) {
            System.out.printf("%s | %dms | %s | %.3fms | %.3fms | %.3fs%n",
                    r.limiter, r.delay, r.idle, r.firstIntervalMillis, r.averageIntervalMillis, r.totalSeconds);
        }
    }

    private static Result run(HttpClient client, HttpRequest request, long delay,
                              boolean idle, boolean guava) throws Exception {
        // 같은 이름의 클래스이므로 Guava만 패키지 전체 이름으로 사용한다.
        com.google.common.util.concurrent.RateLimiter guavaLimiter =
                com.google.common.util.concurrent.RateLimiter.create(QPS);
        RateLimiter<Void> failsafeLimiter = RateLimiter.<Void>smoothBuilder(QPS, Duration.ofSeconds(1)).build();
        String name = guava ? "Guava SmoothBursty" : "Failsafe Smooth";
        if (idle) {
            // permit 1개 소비 후 쉬어 초기 상태와 구분한다. 이때 HTTP 요청은 보내지 않는다.
            if (guava) guavaLimiter.acquire();
            else failsafeLimiter.acquirePermit();
            Thread.sleep(IDLE_MILLIS);
        }
        System.out.printf("%n=== %s | API=%dms | idle=%s ===%n", name, delay, idle);
        long[] starts = new long[REQUEST_COUNT];
        long start = System.nanoTime();
        for (int i = 0; i < REQUEST_COUNT; i++) {
            long waitStart = System.nanoTime();
            if (guava) guavaLimiter.acquire();
            else failsafeLimiter.acquirePermit();
            double waited = (System.nanoTime() - waitStart) / 1e6;
            starts[i] = System.nanoTime();
            double interval = i == 0 ? 0 : (starts[i] - starts[i - 1]) / 1e6;
            System.out.printf("[%6.3fs] %02d START | acquire elapsed=%.3fms | start interval=%.3fms | %s%n",
                    (starts[i] - start) / 1e9, i + 1, waited, interval, Thread.currentThread().getName());
            // 두 방식 모두 응답을 받은 뒤 다음 permit을 획득한다.
            checkResponse(client.send(request, HttpResponse.BodyHandlers.discarding()));
            System.out.printf("[%6.3fs] %02d END%n", (System.nanoTime() - start) / 1e9, i + 1);
        }
        double total = (System.nanoTime() - start) / 1e9;
        return new Result(name, delay, idle, (starts[1] - starts[0]) / 1e6,
                (starts[REQUEST_COUNT - 1] - starts[0]) / 1e6 / (REQUEST_COUNT - 1), total);
    }

    private static void checkResponse(HttpResponse<Void> response) {
        if (response.statusCode() != 200) throw new AssertionError("HTTP " + response.statusCode());
    }

    private record Result(String limiter, long delay, boolean idle, double firstIntervalMillis,
                          double averageIntervalMillis, double totalSeconds) {}
}
