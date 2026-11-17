package com.example.spring_playground.question.q6.async.reactor.operators;

import com.example.spring_playground.question.q6.async.basic.cafe.CafeSystem;
import com.example.spring_playground.question.q6.async.basic.cafe.Order;
import com.example.spring_playground.question.q6.async.basic.cafe.OrderDb;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

/** map(): 전달받은 값을 다른 값으로 바꾼다. */
public class MapMain {

    private static final Logger log = LoggerFactory.getLogger(MapMain.class);

    // 주문 1번 제작 → 완료 문자열 → map으로 안내문 변환 → 구독자에게 안내문 전달
    public static void main(String[] args) {
        CafeSystem cafe = new CafeSystem();
        Order order = new OrderDb().findAll().getFirst();

        Mono<String> notice = Mono.fromCallable(() -> cafe.prepareOrder(order))
                .map(result -> {
                    log.info("[map] 변환 전: {}", result);
                    String message = "[픽업 안내] " + result + ". 카운터에서 받아주세요.";
                    log.info("[map] 변환 후: {}", message);
                    return message;
                });

        log.info("[main] 구성 완료. 구독 시작");
        notice.subscribe(
                message -> log.info("[onNext] {}", message),
                error -> log.error("[onError] 실패", error),
                () -> log.info("[onComplete] 안내문 전달 완료")
        );
    }
}
