package com.example.spring_playground.question.q6.async.basic.cafe;

import java.util.List;

/** 실제 DB 연결 없이 모든 실험에 동일한 주문을 제공하는 메모리 주문 DB. */
public class OrderDb {

    private final List<Order> orders = List.of(
            new Order(1, List.of("라떼", "아메리카노")),
            new Order(2, List.of("아메리카노")),
            new Order(3, List.of("라떼", "라떼"))
    );

    public List<Order> findAll() {
        return orders;
    }
}
