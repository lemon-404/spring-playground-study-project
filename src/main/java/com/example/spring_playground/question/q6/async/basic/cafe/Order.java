package com.example.spring_playground.question.q6.async.basic.cafe;

import java.util.List;

/** 주문 번호와 제작할 메뉴 목록. 같은 메뉴가 여러 번 있으면 각각 한 잔씩 제작한다. */
public record Order(int orderNumber, List<String> items) {

    public Order {
        items = List.copyOf(items);
    }
}
