package com.example.spring_playground.question.q6.async.reactor.operators;

import com.example.spring_playground.question.q6.async.basic.cafe.CafeSystem;
import com.example.spring_playground.question.q6.async.basic.cafe.Order;
import com.example.spring_playground.question.q6.async.basic.cafe.OrderDb;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

/** Mono.when(): 여러 흐름의 값을 모으는 대신 전체 정상 완료를 묶는다. */
public class WhenMain {

    private static final Logger log = LoggerFactory.getLogger(WhenMain.class);

    /*
     * 1번 제작 Mono와 2번 제작 Mono 구성 → when으로 묶기 → 구독 → 둘 다 완료 알림
     * then은 앞 작업 완료 후 다음 흐름을 연결하고, when은 여러 흐름의 완료를 묶는다.
     * when이 자동으로 스레드를 만들거나 병렬 실행을 보장하지는 않는다.
     * 이번 fromCallable 작업들은 main에서 블로킹하므로 제작이 겹치지 않는다.
     * 이 예제의 제작 순서를 when 자체의 순서 보장으로 해석하지 않는다.
     */
    public static void main(String[] args) {
        CafeSystem cafe = new CafeSystem();
        var orders = new OrderDb().findAll();
        Mono<String> first = prepare(cafe, orders.get(0));
        Mono<String> second = prepare(cafe, orders.get(1));

        // 두 제작 결과 String은 전달하지 않는다. 모두 끝났다는 신호만 전달한다.
        Mono<Void> allOrders = Mono.when(first, second);
        log.info("[main] 구성 완료. 구독 시작");
        allOrders.subscribe(
                ignored -> log.info("[onNext] Mono<Void>에는 값이 없어 이 함수는 실행되지 않음"),
                error -> log.error("[onError] 제작 실패", error),
                () -> log.info("[onComplete] 1번과 2번 모두 제작 완료. 함께 픽업 가능합니다.")
        );
    }

    private static Mono<String> prepare(CafeSystem cafe, Order order) {
        return Mono.fromCallable(() -> {
            log.info("[개별 작업] 주문 {}번 실행, thread={}",
                    order.orderNumber(), Thread.currentThread().getName());
            String result = cafe.prepareOrder(order);
            log.info("[개별 작업] 반환값: {}", result);
            return result;
        });
    }
}
