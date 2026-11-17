package com.example.spring_playground.question.q6.async.basic.java5;

import com.example.spring_playground.question.q6.async.basic.cafe.CafeSystem;
import com.example.spring_playground.question.q6.async.basic.cafe.Order;
import com.example.spring_playground.question.q6.async.basic.cafe.OrderDb;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Java 5에서 도입된 ExecutorService와 Future로 주문 제작을 맡기는 예제. */
public class Java5AsyncMain {

    private static final Logger log = LoggerFactory.getLogger(Java5AsyncMain.class);

    /*
     * Java 5 예제의 흐름 (주문 직원 main 한 명, 제작 직원 두 명)
     * 주문 직원: 주문들을 접수하고 제작을 맡긴다.
     * 주문 직원: 1번 기다리기 → "1번 나왔습니다."
     * 주문 직원: 2번 확인하기 → "2번 나왔습니다."
     * 주문 직원: 3번 기다리기 → "3번 나왔습니다."
     * 주문 직원: 전체 결과 확인 → 영업 종료
     *
     * 2번이 먼저 완성돼도 주문 직원은 제출 순서인 1번부터 확인한다.
     * get()은  미완료 작업의 완료까지 기다린다.
     *
     * 정리: 작업 실행은 비동기지만, 결과처리는 동기식 처리가 된다.
     */
    public static void main(String[] args) throws Exception {
        CafeSystem cafe = new CafeSystem();
        OrderDb orderDb = new OrderDb();
        List<Order> orders = orderDb.findAll();

        // 제작 직원 두 명. main은 주문 접수를 계속한다.
        ExecutorService executor = Executors.newFixedThreadPool(2);
        List<Future<String>> results = new ArrayList<>();

        try {
            for (Order order : orders) {
                cafe.receiveOrder(order);

                // submit은 제작 완료를 기다리지 않고 Future를 반환한다.
                Future<String> result = executor.submit(() -> cafe.prepareOrder(order));
                results.add(result);
                log.info("[main] 주문 {}번 제작 맡김, 다음 주문 접수 가능", order.orderNumber());
            }

            log.info("[main] 모든 주문 접수 완료, 제작 결과 확인 시작");
            for (int i = 0; i < results.size(); i++) {
                Future<String> result = results.get(i);
                Order order = orders.get(i);
                // 이 주문이 아직 끝나지 않았다면 main이 여기서 기다린다.
                // 완료된 결과는 바로 반환된다. 확인 순서는 제출 순서다.
                String completedOrder = "";

                //v1
                /*log.info("[main] get() 호출: 결과가 없으면 대기");
                completedOrder = result.get();
                log.info("[main] 결과 확인: {}", completedOrder);*/

                //v2
                /* get()은 작업이 완료됐다면 결과를 즉시 반환하고,
                 아직 진행 중이라면 호출한 스레드를 완료까지 대기시킨다.
                 따라서 모든 작업의 결과를 확인하려면 isDone() 여부와 관계없이 호출해야 한다.
                 작업이 실패하거나 취소됐다면 결과 대신 예외가 발생한다.*/
                if (result.isDone()) {
                    log.info("[main] 이미 완료된 작업");
                } else {
                    log.info("[main] get() 호출: 결과가 없으면 대기");
                }
                completedOrder = result.get();
                log.info("[주문 직원] 주문 {}번 나왔습니다. 결과={}, thread={}",
                        order.orderNumber(), completedOrder, Thread.currentThread().getName());

            }
            log.info("[main] 모든 주문 결과 확인 완료, 영업 종료");
        } finally {
            // 스레드 풀도 사용이 끝나면 종료해야 프로세스가 종료될 수 있다.
            executor.shutdown();
        }
    }
}
