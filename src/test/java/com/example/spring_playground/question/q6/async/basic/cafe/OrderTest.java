package com.example.spring_playground.question.q6.async.basic.cafe;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OrderTest {

    @Test
    void 원본_목록을_변경해도_주문의_메뉴는_바뀌지_않는다() {
        var items = new ArrayList<>(List.of("라떼"));
        var order = new Order(1, items);

        items.add("아메리카노");
        System.out.println(items == order.items());
        assertEquals(List.of("라떼", "아메리카노"), items);
        assertEquals(List.of("라떼"), order.items());
    }

    @Test
    void 주문의_메뉴_목록에_직접_추가하면_예외가_발생한다() {
        var items = new ArrayList<>(List.of("라떼"));
        var order = new Order(1, items);
        System.out.println(items == order.items());
        assertThrows(UnsupportedOperationException.class,
                () -> order.items().add("아메리카노"));

        assertEquals(List.of("라떼"), order.items());
    }
}
