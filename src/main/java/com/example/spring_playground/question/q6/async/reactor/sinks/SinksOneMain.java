package com.example.spring_playground.question.q6.async.reactor.sinks;

import com.example.spring_playground.question.q6.async.basic.cafe.CafeSystem;
import com.example.spring_playground.question.q6.async.basic.cafe.Order;
import com.example.spring_playground.question.q6.async.basic.cafe.OrderDb;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Sinks;

/** 제작 완료 결과 하나를 Sinks.One에 넣고, 구독한 쪽에서 알림을 처리한다. */
public class SinksOneMain {

    private static final Logger log = LoggerFactory.getLogger(SinksOneMain.class);

    /*
     * 알림 수신 등록 → 주문 제작 → Sinks.One에 완료 결과 넣기 → 수신자가 알림 출력
     *
     * 이전 fromCallable 예제는 구독하면 커피 제작 함수를 실행했다.
     * 이번 asMono() 구독은 알림을 받을 준비만 한다. 커피 제작은 아래 코드에서 별도로 실행한다.
     * tryEmitValue()가 값 하나와 정상 완료를 전달한다.
     * sameThread(): main에서 구독·제작·알림 전달·수신을 모두 수행한다.
     * differentThread(): main에서 구독하고, 제작 스레드가 제작·알림 전달·수신을 수행한다.
     * 스케줄러가 없고 수신자가 먼저 등록된 이번 예제에서는 emit을 호출하는 스레드가
     * 수신 함수도 실행한다. 구독한 main으로 자동 복귀하는 것은 아니다.
     */
    public static void main(String[] args) throws Exception {
//        sameThread();
        differentThread();
    }

    private static void sameThread() throws InterruptedException {
        log.info("========== A. 같은 스레드 ==========");
        CafeSystem cafe = new CafeSystem();
        Order order = new OrderDb().findAll().getFirst();
        Sinks.One<String> ready = Sinks.one();

        // 알림을 받는 쪽: 무엇을 할지 미리 등록한다.
        ready.asMono().subscribe(
                result -> log.info("[알림 수신] {}, thread={}", result, Thread.currentThread().getName()),
                error -> log.error("[알림 오류] 실패", error),
                () -> log.info("[알림 완료] 결과 하나의 전달 종료, thread={}",
                        Thread.currentThread().getName())
        );
        log.info("[main] 수신 등록 완료. 아직 제작이나 알림은 실행하지 않음");

        // 제작하는 쪽: 직접 알림을 출력하지 않고 완료 데이터를 Sinks에 넣는다.
        cafe.receiveOrder(order);
        cafe.prepareOrder(order);
        log.info("[제작 직원] Sinks.One에 완료 알림 넣기");
        Sinks.EmitResult emitted = ready.tryEmitValue("주문 " + order.orderNumber() + "번 나왔습니다.");
        if (emitted.isFailure()) {
            throw new IllegalStateException("완료 알림 전달 실패: " + emitted);
        }

        log.info("[main] tryEmitValue() 반환: {}", emitted);
    }

    private static void differentThread() throws Exception {
        log.info("========== B. 구독 스레드와 제작 스레드 분리 ==========");
        CafeSystem cafe = new CafeSystem();
        Order order = new OrderDb().findAll().getFirst();
        // One은 한 번 완료되면 재사용할 수 없으므로 이번 실험용으로 새로 만든다.
        Sinks.One<String> ready = Sinks.one();

        log.info("[구독 등록] thread={}", Thread.currentThread().getName());
        ready.asMono().subscribe(
                result -> log.info("[알림 수신] {}, thread={}", result, Thread.currentThread().getName()),
                error -> log.error("[알림 오류] 실패", error),
                () -> log.info("[알림 완료] 결과 하나의 전달 종료, thread={}",
                        Thread.currentThread().getName())
        );

        // 이름을 붙여 로그에서 main과 구분한다. Sinks가 만든 스레드가 아니다.
        try (ExecutorService executor = Executors.newSingleThreadExecutor(
                task -> new Thread(task, "cafe-worker"))) {
            var production = executor.submit(() -> {
                cafe.receiveOrder(order);
                cafe.prepareOrder(order);
                log.info("[제작 직원] tryEmitValue() 호출 직전, thread={}",
                        Thread.currentThread().getName());
                Sinks.EmitResult emitted = ready.tryEmitValue("주문 " + order.orderNumber() + "번 나왔습니다.");
                if (emitted.isFailure()) {
                    throw new IllegalStateException("완료 알림 전달 실패: " + emitted);
                }
                log.info("[제작 직원] tryEmitValue() 반환: {}, thread={}",
                        emitted, Thread.currentThread().getName());
                return emitted;
            });

            // get()은 main을 기다리게 할 뿐, 수신 함수를 main에서 실행시키지는 않는다.
            log.info("[main] 제작을 맡겼음. get()으로 전체 작업 종료 대기");
            production.get();
        }
        log.info("[main] B 실험 종료");
    }
}
