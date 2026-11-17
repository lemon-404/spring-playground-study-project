package com.example.spring_playground.question.q6.async.reactor.flux;

import com.example.spring_playground.question.q6.async.basic.cafe.CafeSystem;
import com.example.spring_playground.question.q6.async.basic.cafe.OrderDb;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.BaseSubscriber;
import reactor.core.publisher.Flux;

/** Flux의 전체 완료, 중간 오류, 첫 결과 수신 후 취소를 구분한다. */
public class FluxLifecycleMain {

    private static final Logger log = LoggerFactory.getLogger(FluxLifecycleMain.class);

    /*
     * 정상: 1번 결과 → 2번 결과 → 3번 결과 → onComplete
     * 오류: 1번 결과 → 2번에서 실패 → onError (3번 제작과 onComplete 없음)
     * 취소: 1번 결과 → cancel (2·3번 제작과 onComplete·onError 없음)
     *
     * 모두 main에서 순차 실행한다. 완료된 1번 제작은 취소해도 되돌아가지 않는다.
     * map()은 여기서 주문 객체를 제작 완료 문자열로 바꾸는 데에만 사용한다.
     */
    public static void main(String[] args) {
        CafeSystem cafe = new CafeSystem();
        OrderDb orderDb = new OrderDb();

        log.info("========== Flux 정상 완료 ==========");
        observe("정상", prepare(cafe, orderDb, false));

        log.info("========== Flux 2번 주문 오류 ==========");
        observe("오류", prepare(cafe, orderDb, true));

        log.info("========== Flux 1번 결과 수신 후 취소 ==========");
        prepare(cafe, orderDb, false)
                .doFinally(signal -> log.info("[취소] doFinally: {}", signal))
                .subscribe(new BaseSubscriber<String>() {
                    // 기본 BaseSubscriber는 구독 시 값을 요청한다.
                    @Override
                    protected void hookOnNext(String result) {
                        log.info("[취소] onNext: {}", result);
                        log.info("[취소] 1번은 제작 완료. 이후 주문을 받지 않도록 cancel() 요청");
                        cancel();
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
        log.info("========== Flux 실험 종료 ==========");
    }

    private static Flux<String> prepare(CafeSystem cafe, OrderDb orderDb, boolean failSecondOrder) {
        return Flux.fromIterable(orderDb.findAll())
                .map(order -> {
                    cafe.receiveOrder(order);
                    if (failSecondOrder && order.orderNumber() == 2) {
                        throw new IllegalStateException("학습용 오류: 주문 2번의 재료 부족");
                    }
                    try {
                        return cafe.prepareOrder(order);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new IllegalStateException("커피 제작 중 대기가 중단됐습니다.", e);
                    }
                });
    }

    private static void observe(String name, Flux<String> preparations) {
        preparations
                .doFinally(signal -> log.info("[{}] doFinally: {}", name, signal))
                .subscribe(
                        result -> log.info("[{}] onNext: {}", name, result),
                        error -> log.info("[{}] onError: {}", name, error.getMessage()),
                        () -> log.info("[{}] onComplete", name)
                );
    }
}
