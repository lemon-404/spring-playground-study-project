package com.example.spring_playground.question.q6.async.reactor.sinks;

import com.example.spring_playground.question.q6.async.basic.cafe.CafeSystem;
import com.example.spring_playground.question.q6.async.basic.cafe.Order;
import com.example.spring_playground.question.q6.async.basic.cafe.OrderDb;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Sinks;

/** 주문마다 완료 알림을 Sinks.Many에 넣고, 구독한 쪽에서 처리한다. */
public class SinksManyMain {

    private static final Logger log = LoggerFactory.getLogger(SinksManyMain.class);

    /*
     * 알림 수신 등록 → 1번 제작·알림 → 2번 제작·알림 → 3번 제작·알림 → 전체 알림 완료
     *
     * tryEmitNext(): 알림 하나 전달. 이것만으로 전체 흐름이 끝나지 않는다.
     * tryEmitComplete(): 더 전달할 알림이 없음을 알린다.
     * unicast는 구독자 한 명을 허용한다. Many는 구독자 수가 아니라 전달 가능한 값의 수다.
     * 별도 스레드는 만들지 않는다. 구독 등록 자체가 커피 제작을 시작하지도 않는다.
     */
    public static void main(String[] args) throws InterruptedException {
        CafeSystem cafe = new CafeSystem();
        OrderDb orderDb = new OrderDb();
        Sinks.Many<String> notices = Sinks.many().unicast().onBackpressureBuffer();

        // 알림을 받는 쪽. 나중에는 로그 대신 WebSocket 송신 같은 처리를 연결할 수 있다.
        notices.asFlux().subscribe(
                message -> log.info("[알림 수신] {}, thread={}", message, Thread.currentThread().getName()),
                error -> log.error("[알림 오류] 실패", error),
                () -> log.info("[알림 완료] 모든 주문의 알림 전달 종료")
        );
        log.info("[main] 수신 등록 완료. 이제 주문 제작 시작");

        // 제작하는 쪽은 알림 내용을 넣기만 한다.
        for (Order order : orderDb.findAll()) {
            cafe.receiveOrder(order);
            cafe.prepareOrder(order);
            log.info("[제작 직원] 주문 {}번 완료 알림 넣기", order.orderNumber());
            Sinks.EmitResult emitted = notices.tryEmitNext("주문 " + order.orderNumber() + "번 나왔습니다.");
            if (emitted.isFailure()) {
                throw new IllegalStateException("완료 알림 전달 실패: " + emitted);
            }
        }

        log.info("[제작 직원] 더 전달할 알림이 없어 완료 신호 넣기");
        Sinks.EmitResult completed = notices.tryEmitComplete();
        if (completed.isFailure()) {
            throw new IllegalStateException("완료 신호 전달 실패: " + completed);
        }
        log.info("[main] tryEmitComplete() 반환: {}", completed);
    }
}
