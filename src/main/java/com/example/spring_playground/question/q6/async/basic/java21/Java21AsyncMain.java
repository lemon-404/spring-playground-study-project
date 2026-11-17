package com.example.spring_playground.question.q6.async.basic.java21;

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

/**
 * Java 21의 가상 스레드에서 주문 제작을 실행하는 예제.
 * TODO: 스레드 기초를 학습한 뒤 플랫폼 스레드·OS 스레드·가상 스레드의 관계와
 * 생성 비용, 자원 한계를 다시 확인한다. 현재는 가상 스레드로 작업을 실행할 수 있다는
 * 수준으로 이해하고, 상세 동작은 이후 학습한다.
 */
public class Java21AsyncMain {

    private static final Logger log = LoggerFactory.getLogger(Java21AsyncMain.class);

    /*
     * 주문 직원: 주문 접수 → 주문마다 가상 스레드에 제작 맡김 → 다음 주문 접수
     * 각 주문의 가상 스레드: 제작 → "n번 나왔습니다." 알림
     * 주문 직원: 모든 제작과 알림 완료 확인 → 영업 종료
     *
     * Java 8 예제는 플랫폼 스레드 두 개를 재사용하므로 세 번째 주문은 대기한다.
     * 이 예제는 작업마다 새 가상 스레드를 사용하므로 직원 두 명의 제한이 없다.
     * 단, 가상 스레드 수만큼 OS 스레드가 만들어진다는 뜻은 아니다.
     * Thread.sleep() 중 가상 스레드는 대기하지만 실행을 맡던 OS 스레드는 놓아줄 수 있다.
     * 커피 한 잔의 제작 시간 자체가 줄어드는 것은 아니다.
     *
     * 결과 처리는 Java 8과 동일한 thenAccept()를 사용한다.
     * 가상 스레드는 실행 방식이고, CompletableFuture는 완료와 결과를 연결하는 도구다.
     * 마지막 join()은 main을 대기시킨다. 가상 스레드라고 대기가 사라지지 않는다.
     */
    public static void main(String[] args) {
        CafeSystem cafe = new CafeSystem();
        OrderDb orderDb = new OrderDb();
        List<CompletableFuture<Void>> results = new ArrayList<>();

        // Java 8 예제와 달리 고정된 직원 수 없이 작업마다 가상 스레드를 만든다.
        // try 블록 종료 시 close()가 호출되어 실행기를 종료하고 남은 작업도 기다린다.
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (Order order : orderDb.findAll()) {
                cafe.receiveOrder(order);

                CompletableFuture<String> preparation = CompletableFuture.supplyAsync(() -> {
                    // 가상 스레드는 기본 이름이 비어 있으므로 주문 번호로 이름을 붙인다.
                    Thread.currentThread().setName("cafe-order-" + order.orderNumber());
                    log.info("[제작 직원] 주문 {}번, virtual={}, thread={}",
                            order.orderNumber(), Thread.currentThread().isVirtual(), Thread.currentThread());
                    try {
                        return cafe.prepareOrder(order);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new CompletionException(e);
                    }
                }, executor);

                // 별도 알림 스레드를 만들지는 않는다. 이미 완료됐다면 등록 시 실행될 수도 있다.
                CompletableFuture<Void> result = preparation.thenAccept(completedOrder -> {
                    log.info("[완료 알림] 주문 {}번 나왔습니다. 결과={}, thread={}",
                            order.orderNumber(), completedOrder, Thread.currentThread().getName());
                });
                results.add(result);
                log.info("[main] 주문 {}번 제작 맡김, 다음 주문 접수 가능", order.orderNumber());
            }

            log.info("[main] 모든 주문 접수 완료, 전체 제작 및 알림 완료 대기");
            CompletableFuture.allOf(results.toArray(new CompletableFuture<?>[0])).join();
        }

        log.info("[main] 모든 주문 결과 처리 및 실행기 종료 완료, 영업 종료");
    }
}
