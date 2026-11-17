package com.example.spring_playground.question.q6.async.reactor.flux;

import com.example.spring_playground.question.q6.async.basic.cafe.CafeSystem;
import com.example.spring_playground.question.q6.async.basic.cafe.Order;
import com.example.spring_playground.question.q6.async.basic.cafe.OrderDb;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;

/** Flux로 여러 주문을 전달하고, 주문마다 처리 함수가 실행되는 것을 확인한다. */
public class FluxBaseMain {

    private static final Logger log = LoggerFactory.getLogger(FluxBaseMain.class);

    /*
     * A. 주문 목록으로 Flux 구성 → 아직 주문 전달이나 제작은 하지 않음
     * B. 구독 → 1번 전달·제작 → 2번 전달·제작 → 3번 전달·제작 → 전체 완료
     *
     * Mono 예제는 제작 결과 String 하나를 전달했다.
     * 이 Flux 예제는 Order 세 개를 전달하고, 수신 함수에서 각각 제작한다.
     * subscribe()의 첫 번째 함수는 주문마다, 세 번째 함수는 전체 정상 완료 시 한 번 실행된다.
     * 별도 스케줄러가 없으므로 전달과 제작은 모두 main에서 순서대로 실행된다.
     * 여러 값을 전달하는 것과 여러 작업을 동시에 실행하는 것은 다르다.
     */
    public static void main(String[] args) {
        CafeSystem cafe = new CafeSystem();
        OrderDb orderDb = new OrderDb();

        log.info("[A-1] Flux 구성 시작, thread={}", Thread.currentThread().getName());
        Flux<Order> orders = Flux.fromIterable(orderDb.findAll());
        log.info("[A-2] Flux 구성 완료. 아직 구독하지 않았으므로 주문 전달과 제작은 없음");
        // A만 확인하려면 아래 B 부분을 주석 처리하고 실행한다.

        log.info("[B-1] subscribe() 호출 직전, thread={}", Thread.currentThread().getName());
        orders.subscribe(
                order -> {
                    log.info("[B-2] 주문 {}번 전달받음, thread={}",
                            order.orderNumber(), Thread.currentThread().getName());
                    cafe.receiveOrder(order);
                    try {
                        String result = cafe.prepareOrder(order);
                        log.info("[B-3] 주문 {}번 나왔습니다. 결과={}, thread={}",
                                order.orderNumber(), result, Thread.currentThread().getName());
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new IllegalStateException("커피 제작 중 대기가 중단됐습니다.", e);
                    }
                },
                error -> log.error("[B-오류] 주문 처리 실패", error),
                () -> log.info("[B-4] 전체 주문 전달 및 처리 정상 완료, thread={}",
                        Thread.currentThread().getName())
        );
        log.info("[B-5] subscribe() 호출 반환. 이 예제는 전체 처리 후 이 줄 실행, thread={}",
                Thread.currentThread().getName());
    }
}
