package com.example.spring_playground.question.q6.async.reactor.scheduler;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import com.example.spring_playground.question.q6.async.basic.cafe.CafeSystem;
import com.example.spring_playground.question.q6.async.basic.cafe.Order;
import com.example.spring_playground.question.q6.async.basic.cafe.OrderDb;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/** subscribeOn()으로 커피 제작을 boundedElastic 스케줄러에 맡긴다. */
public class SubscribeOnMain {

    private static final Logger log = LoggerFactory.getLogger(SubscribeOnMain.class);

    /*
     * A. 스케줄러 없음: main이 제작 → main이 결과 처리 → subscribe() 반환
     * B. boundedElastic: main이 구독 → 제작을 스케줄러에 맡김 → main은 다음 코드 진행
     *                    → 작업 스레드가 제작·결과 처리 → main이 전체 완료 확인
     *
     * subscribeOn()은 실행 위치를 지정할 뿐, 그 줄에서 구독을 시작하지 않는다.
     * boundedElastic()은 블로킹 작업을 맡기는 용도이며 스레드와 대기 작업 수에 한계가 있다.
     * CafeSystem의 sleep이 사라지거나 non-blocking으로 바뀌는 것은 아니다.
     * main 대신 boundedElastic 작업 스레드가 제작 중 기다린다.
     */
    public static void main(String[] args) throws Exception {
//        withoutScheduler();
        withBoundedElastic();
    }

    private static void withoutScheduler() {
        log.info("========== A. 스케줄러 없음 ==========");
        CafeSystem cafe = new CafeSystem();
        Order order = new OrderDb().findAll().getFirst();

        Mono<String> preparation = Mono.fromCallable(() -> {
            log.info("[A 제작] 실행 시작, thread={}", Thread.currentThread().getName());
            cafe.receiveOrder(order);
            return cafe.prepareOrder(order);
        });

        log.info("[A main] subscribe() 호출 직전");
        preparation.subscribe(
                result -> log.info("[A 알림] {}, thread={}", result, Thread.currentThread().getName()),
                error -> log.error("[A 오류] 제작 실패", error),
                () -> log.info("[A 완료] thread={}", Thread.currentThread().getName())
        );
        log.info("[A main] subscribe() 반환. 제작과 알림이 끝난 뒤 이 줄 실행");
    }

    private static void withBoundedElastic() throws Exception {
        log.info("========== B. boundedElastic 적용 ==========");
        CafeSystem cafe = new CafeSystem();
        Order order = new OrderDb().findAll().getFirst();
        // 콘솔 프로그램이 결과 처리 전에 끝나지 않도록 완료를 main에 전달한다.
        CompletableFuture<Void> finished = new CompletableFuture<>();

        Mono<String> preparation = Mono.fromCallable(() -> {
                    log.info("[B 제작] 실행 시작, thread={}", Thread.currentThread().getName());
                    cafe.receiveOrder(order);
                    return cafe.prepareOrder(order);
                })
                .subscribeOn(Schedulers.boundedElastic());

        log.info("[B main] subscribe() 호출 직전");
        preparation.subscribe(
                result -> log.info("[B 알림] {}, thread={}", result, Thread.currentThread().getName()),
                error -> {
                    log.error("[B 오류] 제작 실패", error);
                    finished.completeExceptionally(error);
                },
                () -> {
                    log.info("[B 완료] thread={}", Thread.currentThread().getName());
                    finished.complete(null);
                }
        );
        log.info("[B main] subscribe() 반환. 제작 완료를 기다리지 않고 이 줄 실행");
        log.info("[B main] 다른 일: 다음 주문을 접수할 수 있음");

        // get()은 이번 관찰을 마무리하기 위한 대기다. 작업을 main으로 옮기지 않는다.
        log.info("[B main] 마지막에만 get()으로 제작과 알림 완료 대기");
        finished.get(10, TimeUnit.SECONDS);
        log.info("[B main] 전체 완료 확인");
    }
}
