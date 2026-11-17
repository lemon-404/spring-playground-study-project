package com.example.spring_playground.question.q6.async.reactor.mono;

import com.example.spring_playground.question.q6.async.basic.cafe.CafeSystem;
import com.example.spring_playground.question.q6.async.basic.cafe.Order;
import com.example.spring_playground.question.q6.async.basic.cafe.OrderDb;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

/** Mono를 구성하는 것과 구독해서 실행하는 것의 차이만 확인한다. */
public class MonoBaseMain {

    private static final Logger log = LoggerFactory.getLogger(MonoBaseMain.class);

    /*
     * 주문 1번(라떼 + 아메리카노) 하나만 사용한다. 서버는 필요 없다.
     * A. Mono 구성 → 아직 제작하지 않음
     * B. subscribe() 호출 → 제작 → 결과 전달 → subscribe() 반환
     *
     * fromCallable() 안의 함수는 지금 호출하는 것이 아니라 구독될 때 호출된다.
     * 이번 예제는 별도 실행기나 스케줄러를 쓰지 않아 제작도 main에서 실행된다.
     * Mono를 사용하거나 구독했다고 자동으로 비동기 실행되는 것은 아니다.
     */
    public static void main(String[] args) {
        CafeSystem cafe = new CafeSystem();
        Order order = new OrderDb().findAll().getFirst();

        log.info("[A-1] 주문 {}번 Mono 구성 시작, thread={}",
                order.orderNumber(), Thread.currentThread().getName());

        Mono<String> preparation = Mono.fromCallable(() -> {
            log.info("[B-2] 등록한 제작 함수 실행, thread={}", Thread.currentThread().getName());
            return cafe.prepareOrder(order);
        });

        log.info("[A-2] Mono 구성 완료. 아직 구독하지 않았으므로 제작 로그가 없음");
        // A만 확인하려면 아래 B 부분을 주석 처리하고 실행한다.

        log.info("[B-1] subscribe() 호출 직전, thread={}", Thread.currentThread().getName());
        preparation.subscribe(
                completedOrder -> log.info("[B-3] 주문 {}번 나왔습니다. 결과={}, thread={}",
                        order.orderNumber(), completedOrder, Thread.currentThread().getName()),
                error -> log.error("[B-오류] 제작 실패", error)
        );
        log.info("[B-4] subscribe() 호출 반환. 이 예제는 제작과 결과 처리 후 이 줄 실행, thread={}",
                Thread.currentThread().getName());
    }
}
