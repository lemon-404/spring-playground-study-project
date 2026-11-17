package com.example.spring_playground.question.q6.async.basic.cafe;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** 버전별 비동기 실행 코드에서 공통으로 호출하는 학습용 커피숍. */
public class CafeSystem {

    private static final Logger log = LoggerFactory.getLogger(CafeSystem.class);

    // 실제 제조 대신 제작 시간을 재현한다. 라떼는 별도로 2초를 사용한다.
    private final long preparationMillis;

    public CafeSystem() {
        this(1_000);
    }

    public CafeSystem(long preparationMillis) {
        if (preparationMillis < 1_000) {
            throw new IllegalArgumentException("학습용 제작 지연은 1,000ms 이상으로 설정합니다.");
        }
        this.preparationMillis = preparationMillis;
    }

    /** 주문 접수 직원 역할의 main 스레드에서 호출한다. */
    public void receiveOrder(Order order) {
        log.info("[주문 {}번] 접수: {}, thread={}",
                order.orderNumber(), order.items(), Thread.currentThread().getName());
    }

    /**
     * 비동기 실행을 맡은 작업 스레드에서 호출한다.
     * 이 메서드 자체는 스레드를 만들지 않고, 호출한 스레드에서 메뉴를 순서대로 제작한다.
     */
    public String prepareOrder(Order order) throws InterruptedException {
        log.info("[주문 {}번] 제작 시작, thread={}",
                order.orderNumber(), Thread.currentThread().getName());

        for (String item : order.items()) {
            prepareCoffee(order.orderNumber(), item);
        }

        String result = "주문 " + order.orderNumber() + "번 전체 제작 완료";
        log.info("[주문 {}번] 전체 제작 완료, thread={}",
                order.orderNumber(), Thread.currentThread().getName());
        return result;
    }

    private void prepareCoffee(int orderNumber, String item) throws InterruptedException {
        log.info("[주문 {}번] {} 제작 시작, thread={}",
                orderNumber, item, Thread.currentThread().getName());
        long delayMillis = "라떼".equals(item) ? 2_000 : preparationMillis;
        Thread.sleep(delayMillis);
        log.info("[주문 {}번] {} 제작 완료, thread={}",
                orderNumber, item, Thread.currentThread().getName());
    }
}
