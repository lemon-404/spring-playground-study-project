package com.example.spring_playground.question.q6.async.basic.java8;

import com.example.spring_playground.question.q6.async.basic.cafe.CafeSystem;
import com.example.spring_playground.question.q6.async.basic.cafe.Order;
import com.example.spring_playground.question.q6.async.basic.cafe.OrderDb;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Java 8의 CompletableFuture로 제작 완료 후 실행할 처리를 등록하는 예제. */
public class Java8AsyncMain {

    private static final Logger log = LoggerFactory.getLogger(Java8AsyncMain.class);

    /*
     * Java 8 예제의 흐름 (주문 직원 main 한 명, 제작 직원 두 명)
     * 주문 직원: 주문들을 접수하고 "각 주문이 완성되면 결과를 알려주세요." 등록
     * 제작 직원: 2번 완성 → "2번 나왔습니다."
     * 제작 직원: 1번 완성 → "1번 나왔습니다."
     * 제작 직원: 3번 완성 → "3번 나왔습니다."
     * 주문 직원: 전체 완료 확인 → 영업 종료
     *
     * 위 순서는 현재 주문과 지연 조건에서의 예시이며 실행 순서를 보장하지 않는다.
     * 개별 완료 알림은 thenAccept(), 전체 완료 대기는 allOf(...).join()이 담당한다.
     * thenAccept()는 별도 스레드를 만들지 않으며, 등록 전에 이미 완료된 작업이면
     * 등록하는 스레드에서도 실행될 수 있다.
     *
     * 정리:
     * 1. 작업 실행이 비동기 처리되는것은 java 5와 동일하다.
     * 2. java 8 예제는 각 작업이 완료되면 등록된 결과 처리를 실행하므로,
     *  먼저 제출한 작업의 완료를 기다리지 않고 완료된 작업의 결과를 처리할 수 있다.
     * 다만 마지막 allOf(...).join에서는 모든 작업과 결과 처리가 끝날때까지
     * main 스레드에 블로킹 대기가 발생한다.
     */
    public static void main(String[] args) {
        CafeSystem cafe = new CafeSystem();
        OrderDb orderDb = new OrderDb();
        ExecutorService executor = Executors.newFixedThreadPool(2);
        List<CompletableFuture<Void>> results = new ArrayList<>();

        try {
            for (Order order : orderDb.findAll()) {
                cafe.receiveOrder(order);

                // supplyAsync: 지정한 스레드 풀에서 제작하고 결과를 반환한다.
                CompletableFuture<String> preparation = CompletableFuture.supplyAsync(() -> {
                    try {
                        return cafe.prepareOrder(order);
                    } catch (InterruptedException e) {
                        // Supplier는 체크 예외를 던질 수 없어 비동기 실패로 전달한다.
                        Thread.currentThread().interrupt();
                        throw new CompletionException(e);
                    }
                }, executor);

                // thenAccept: 성공 결과가 준비되면 실행할 처리를 등록한다.
                // main이 주문마다 get()으로 기다리며 결과를 꺼낼 필요가 없다.
                // 이 처리는 완료 스레드에서 실행되거나, 이미 완료됐다면 등록 시 실행될 수 있다.
                CompletableFuture<Void> result = preparation.thenAccept(completedOrder -> {
                    log.info("[완료 알림] 주문 {}번 나왔습니다. 결과={}, thread={}",
                            order.orderNumber(), completedOrder, Thread.currentThread().getName());
                });
                results.add(result);
                log.info("[main] 주문 {}번 제작 맡김, 완료 후 처리 등록, 다음 주문 접수 가능",
                        order.orderNumber());
            }

            // 결과 출력까지 끝난 Future들을 묶는다.
            CompletableFuture<Void> allOrders = CompletableFuture.allOf(
                    results.toArray(new CompletableFuture<?>[0]));

            // 콘솔 예제가 먼저 끝나지 않도록 마지막에만 main이 전체 완료를 기다린다.
            // join()도 미완료 상태라면 호출 스레드를 대기시킨다.
            log.info("[main] 모든 주문 접수 완료, 전체 제작 및 결과 처리 완료 대기");
            allOrders.join();
            log.info("[main] 모든 주문 결과 처리 완료, 영업 종료");
        } finally {
            executor.shutdown();
        }
    }
}
