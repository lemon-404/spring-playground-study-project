package com.example.spring_playground.question.q6.async.reactor.operators;

import com.example.spring_playground.question.q6.async.basic.cafe.CafeSystem;
import com.example.spring_playground.question.q6.async.basic.cafe.Order;
import com.example.spring_playground.question.q6.async.basic.cafe.OrderDb;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

/** then(): 앞의 값은 무시하고, 정상 완료 후 다음 Mono를 실행한다. */
public class ThenMain {

    private static final Logger log = LoggerFactory.getLogger(ThenMain.class);

    // 주문 제작 정상 완료 → 픽업 알림 실행 → 전체 완료
    // 앞 작업이 실패하면 다음 알림은 실행되지 않는다.
    public static void main(String[] args) {
        CafeSystem cafe = new CafeSystem();
        Order order = new OrderDb().findAll().getFirst();

        Mono<String> preparation = Mono.fromCallable(() -> cafe.prepareOrder(order));
        Mono<Void> notification = Mono.fromRunnable(() ->
                log.info("[픽업 알림] 주문 {}번 나왔습니다.", order.orderNumber()));

        // 제작 결과 String을 알림에 넘기지 않는다. 제작의 정상 완료만 연결한다.
        Mono<Void> ready = preparation.then(notification);
        log.info("[main] 구성 완료. 구독 시작");
        ready.subscribe(
                ignored -> log.info("[onNext] Mono<Void>에는 값이 없어 이 함수는 실행되지 않음"),
                error -> log.error("[onError] 실패", error),
                () -> log.info("[onComplete] 제작과 픽업 알림 완료")
        );
    }
}
