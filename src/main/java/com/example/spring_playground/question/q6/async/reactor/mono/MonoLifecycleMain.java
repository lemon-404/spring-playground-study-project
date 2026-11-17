package com.example.spring_playground.question.q6.async.reactor.mono;

import com.example.spring_playground.question.q6.async.basic.cafe.CafeSystem;
import com.example.spring_playground.question.q6.async.basic.cafe.Order;
import com.example.spring_playground.question.q6.async.basic.cafe.OrderDb;
import org.reactivestreams.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.BaseSubscriber;
import reactor.core.publisher.Mono;

/** Mono의 값 수신, 정상 완료, 오류, 제작 시작 전 취소를 구분한다. */
public class MonoLifecycleMain {

    private static final Logger log = LoggerFactory.getLogger(MonoLifecycleMain.class);

    /*
     * 정상: 제작 → onNext(완료 문자열) → onComplete
     * 오류: 의도적인 제작 실패 → onError (onNext와 onComplete 없음)
     * 취소: 구독 직후 제작 시작 전에 cancel → 취소 관찰 (완료·오류 콜백 없음)
     *
     * 모두 main에서 실행한다. 취소를 위해 별도 스레드나 시간 지연 연산자를 넣지 않는다.
     * 동기 작업은 subscribe()가 반환될 때 이미 끝날 수 있으므로,
     * 취소 예제만 구독 시점에 직접 cancel()할 수 있는 BaseSubscriber를 사용한다.
     */
    public static void main(String[] args) {
        CafeSystem cafe = new CafeSystem();
        Order order = new OrderDb().findAll().getFirst();

        log.info("========== Mono 정상 완료 ==========");
        observe("정상", prepare(cafe, order));

        log.info("========== Mono 오류 ==========");
        Mono<String> failure = Mono.fromCallable(() -> {
            cafe.receiveOrder(order);
            throw new IllegalStateException("학습용 오류: 주문 1번의 재료 부족");
        });
        observe("오류", failure);

        log.info("========== Mono 제작 시작 전 취소 ==========");
        prepare(cafe, order)
                .doFinally(signal -> log.info("[취소] doFinally: {}", signal))
                .subscribe(new BaseSubscriber<String>() {
                    @Override
                    protected void hookOnSubscribe(Subscription subscription) {
                        log.info("[취소] 구독은 됐지만 제작 전에 cancel() 요청");
                        cancel();
                    }

                    @Override
                    protected void hookOnNext(String value) {
                        log.info("[취소] onNext: {}", value);
                    }

                    @Override
                    protected void hookOnComplete() {
                        log.info("[취소] onComplete");
                    }

                    @Override
                    protected void hookOnError(Throwable error) {
                        log.info("[취소] onError: {}", error.getMessage());
                    }
                });
        log.info("========== Mono 실험 종료 ==========");
    }

    private static Mono<String> prepare(CafeSystem cafe, Order order) {
        return Mono.fromCallable(() -> {
            cafe.receiveOrder(order);
            return cafe.prepareOrder(order);
        });
    }

    private static void observe(String name, Mono<String> preparation) {
        preparation
                // 정상 완료·오류·취소 중 어떤 이유로 끝났는지 관찰한다.
                .doFinally(signal -> log.info("[{}] doFinally: {}", name, signal))
                .subscribe(
                        result -> log.info("[{}] onNext: {}", name, result),
                        error -> log.info("[{}] onError: {}", name, error.getMessage()),
                        () -> log.info("[{}] onComplete", name)
                );
    }
}
