package com.example.spring_playground.question.q6.async.reactor.operators;

import com.example.spring_playground.question.q6.async.basic.cafe.CafeSystem;
import com.example.spring_playground.question.q6.async.basic.cafe.OrderDb;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** concatMap(): 값마다 다음 흐름을 연결하고, 이전 흐름 완료 후 다음 흐름을 구독한다. */
public class ConcatMapMain {

    private static final Logger log = LoggerFactory.getLogger(ConcatMapMain.class);

    // 주문 목록 → 1번 제작 Mono 완료 → 2번 제작 Mono 완료 → 3번 제작 Mono 완료
    public static void main(String[] args) {
        CafeSystem cafe = new CafeSystem();
        OrderDb orderDb = new OrderDb();

        Flux<String> results = Flux.fromIterable(orderDb.findAll())
                .concatMap(order -> {
                    log.info("[concatMap] 주문 {}번을 제작 Mono로 연결", order.orderNumber());
                    cafe.receiveOrder(order);
                    return Mono.fromCallable(() -> {

                        return cafe.prepareOrder(order);
                    });
                });

        // map으로 Mono를 반환하면 값 자체가 Mono가 된다.
        // concatMap은 그 Mono를 구독해 제작 결과 String을 아래로 전달한다.
        log.info("[main] 구성 완료. 구독 시작");
        results.subscribe(
                result -> log.info("[onNext] {}", result),
                error -> log.error("[onError] 실패", error),
                () -> log.info("[onComplete] 모든 주문 제작 완료")
        );
    }
}
