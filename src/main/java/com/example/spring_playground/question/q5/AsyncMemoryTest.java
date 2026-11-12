package com.example.spring_playground.question.q5;

import com.example.spring_playground.question.q5.async.mock.MockApiServer;
import dev.failsafe.RateLimiter;

import java.lang.management.ManagementFactory;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static com.example.spring_playground.question.q5.async.mock.ExecutorSupport.stopExecutor;

/** Basic Case 3/4와 같은 호출 흐름에서 진행 중 요청 수와 힙만 관찰한다. */
public class AsyncMemoryTest {
    private static final int REQUEST_COUNT = 10;
    private static final long QPS = 2;
    private static final long RESPONSE_DELAY_MS = 1200;
    private static final int RESPONSE_BYTES = 1024 * 1024; // 요청당 1MiB를 메모리로 읽는다.

    public static void main(String[] args) throws Exception {
        try {
            run(false);
            run(true);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw e;
        }
    }

    private static void run(boolean async) throws Exception {
        ExecutorService httpExecutor = Executors.newFixedThreadPool(4);
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        AtomicInteger active = new AtomicInteger();
        AtomicInteger peakActive = new AtomicInteger();
        AtomicLong peakHeap = new AtomicLong();
        try (MockApiServer server = new MockApiServer(RESPONSE_DELAY_MS, RESPONSE_BYTES)) {
            HttpClient client = HttpClient.newBuilder().executor(httpExecutor)
                    .version(HttpClient.Version.HTTP_1_1).connectTimeout(Duration.ofSeconds(5)).build();
            HttpRequest request = HttpRequest.newBuilder(server.uri())
                    .timeout(Duration.ofSeconds(10)).GET().build();
            RateLimiter<Void> limiter = RateLimiter.<Void>smoothBuilder(QPS, Duration.ofSeconds(1)).build();
            System.out.println("\n========== " + (async ? "ASYNC" : "SYNC") + " ==========");
            long baselineHeap = heapUsed();
            peakHeap.set(baselineHeap);
            long start = System.nanoTime();

            for (int i = 1; i <= REQUEST_COUNT; i++) {
                int number = i;
                // Failsafe는 대기시간을 반환하지 않으므로 실제 경과시간을 측정한다.
                long waitStart = System.nanoTime();
                limiter.acquirePermit();
                double waited = (System.nanoTime() - waitStart) / 1_000_000_000.0;
                peakActive.accumulateAndGet(active.incrementAndGet(), Math::max);
                log(start, number, "START", waited, active, peakHeap);
                if (async) {
                    // main은 응답을 기다리지 않고 다음 acquirePermit()로 진행한다.
                    futures.add(client.sendAsync(request, HttpResponse.BodyHandlers.ofByteArray())
                            .whenComplete((response, error) -> {
                                active.decrementAndGet();
                                log(start, number, error == null ? "END" : "ERROR", 0, active, peakHeap);
                            })
                            .thenAccept(AsyncMemoryTest::checkResponse));
                } else {
                    HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
                    active.decrementAndGet();
                    log(start, number, "END", 0, active, peakHeap);
                    checkResponse(response);
                }
            }
            CompletableFuture.allOf(futures.toArray(CompletableFuture<?>[]::new)).get();
            if (active.get() != 0) throw new AssertionError("미완료 요청이 있습니다.");
            System.out.printf("Total: %.3fs | Peak in-flight: %d | Heap baseline/end/event peak: %.2f/%.2f/%.2f MiB%n",
                    (System.nanoTime() - start) / 1e9, peakActive.get(),
                    baselineHeap / 1048576.0, heapUsed() / 1048576.0, peakHeap.get() / 1048576.0);
        } finally {
            futures.forEach(f -> f.cancel(true));
            stopExecutor(httpExecutor);
        }
    }

    private static void checkResponse(HttpResponse<byte[]> response) {
        if (response.statusCode() != 200 || response.body().length != RESPONSE_BYTES) {
            throw new AssertionError("응답 상태/크기가 다릅니다.");
        }
    }

    private static long heapUsed() {
        return ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed();
    }

    private static void log(long start, int number, String event, double wait,
                            AtomicInteger active, AtomicLong peakHeap) {
        long heap = heapUsed();
        peakHeap.accumulateAndGet(heap, Math::max);
        System.out.printf("[%6.3fs] %02d %-5s | wait=%.3fs | in-flight=%d | heap=%.2f MiB | %s%n",
                (System.nanoTime() - start) / 1e9, number, event, wait, active.get(),
                heap / 1048576.0, Thread.currentThread().getName());
    }
}
