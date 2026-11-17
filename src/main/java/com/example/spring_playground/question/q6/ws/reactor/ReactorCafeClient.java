package com.example.spring_playground.question.q6.ws.reactor;

import java.net.URI;
import java.time.Duration;
import java.util.List;

import com.example.spring_playground.question.q6.async.basic.cafe.OrderDb;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.reactive.socket.CloseStatus;
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

/**
 * 기존 커피숍 서버에 Reactor의 send·receive로 연결하는 클라이언트.
 *
 * 학습 현황: 비동기 프로그래밍과 WebSocket 기본 흐름 학습 완료.
 * Reactor 기초 예제는 살펴봤지만, 이를 조합한 이 클라이언트의 흐름 학습은 아직 하지 않았다.
 *
 * TODO: Reactor 학습을 이어가고 이 코드의 구독 → 연결 → 송수신 → 결과 전달 → 종료를 이해한다.
 * TODO: Reactor 학습 후 q6 패키지를 정리한다.
 * TODO: q6에서 배운 내용과 직접 확인한 실험 결과를 정리한다.
 * TODO: 학습 정리 후 아래 주제로 블로그를 작성한다.
 *  - 비동기 프로그래밍
 *  - WebSocket
 *  - Reactor 객체들
 *  - Spring WebSocket 사용법
 *  - 실시간 차트 조회 수정 및 구현
 */
public class ReactorCafeClient {

    private static final Logger log = LoggerFactory.getLogger(ReactorCafeClient.class);

    /*
     * 연결 → send로 주문 번호 1·2·3 전송 / receive로 제작 완료 응답 처리
     * → 세 응답 수신 → 연결 종료 → 전체 결과 반환
     *
     * send와 receive를 Mono.when으로 함께 구독한다. 주문별 poll/get 대기는 없다.
     * 서버는 기존 수신 콜백에서 주문을 하나씩 제작한다. 클라이언트만 바꾼 예제다.
     * 클라이언트의 수신 처리에는 sleep 같은 블로킹 작업을 넣지 않는다.
     */
    public static void main(String[] args) {
        ReactorCafeClient client = new ReactorCafeClient();
        Mono<List<String>> exchange = client.connectAndExchange("ws://localhost:8080/q6/ws");
        log.info("[main] 흐름 구성 완료. 아직 연결하지 않음");

        // 콘솔 예제의 시작과 전체 완료 대기. Netty의 송수신 스레드를 막는 것은 아니다.
        List<String> responses = exchange.block(Duration.ofSeconds(30));
        log.info("[main] 전체 결과 확인: {}", responses);
    }

    /** 구독할 때 연결하고, 모든 응답과 연결 종료를 확인한 뒤 결과 목록을 전달한다. */
    public Mono<List<String>> connectAndExchange(String url) {
        return Mono.defer(() -> {
            var orders = new OrderDb().findAll();
            // 세 응답을 모은 결과를 WebSocket 세션 밖의 반환 흐름에 전달한다.
            Sinks.One<List<String>> completedOrders = Sinks.one();
            var client = new ReactorNettyWebSocketClient();

            log.info("[연결 요청] {}, thread={}", url, Thread.currentThread().getName());
            Mono<Void> connection = client.execute(URI.create(url), session -> {
                log.info("[연결 완료] session={}, thread={}",
                        session.getId(), Thread.currentThread().getName());

                Flux<String> orderNumbers = Flux.fromIterable(orders)
                        .map(order -> Integer.toString(order.orderNumber()))
                        .doOnNext(number -> log.info("[송신 흐름] 주문 {}번, thread={}",
                                number, Thread.currentThread().getName()));

                // send()에는 메시지 흐름을 전달한다. send 완료는 제작 완료와 다르다.
                Mono<Void> send = session.send(orderNumbers.map(session::textMessage))
                        .doOnSuccess(ignored -> log.info("[송신 완료] 세 주문 전송 완료. 응답은 별도 수신"));


                log.info("대기 안함, thread={}", Thread.currentThread().getName());

                Mono<Void> receive = session.receive()
                        .map(message -> message.getPayloadAsText())
                        .doOnNext(text -> log.info("[응답 수신] {}, thread={}",
                                text, Thread.currentThread().getName()))
                        // 학습용 종료 조건: 공통 주문 수만큼 응답을 받으면 종료한다.
                        .take(orders.size())
                        .collectList()
                        .flatMap(responses -> {
                            if (responses.size() != orders.size()) {
                                return Mono.error(new IllegalStateException("모든 응답 수신 전에 연결 종료"));
                            }
                            Sinks.EmitResult emitted = completedOrders.tryEmitValue(List.copyOf(responses));
                            if (emitted.isFailure()) {
                                return Mono.error(new IllegalStateException("결과 전달 실패: " + emitted));
                            }
                            log.info("[종료 요청] 모든 제작 응답 수신. 정상 종료 요청");
                            return session.close(CloseStatus.NORMAL);
                        });

                // 송신만 끝나고 세션이 종료되지 않도록 두 흐름을 함께 구성한다.
                return Mono.when(send, receive);
            });

            return connection
                    .doOnSuccess(ignored -> log.info("[세션 처리 완료] 송수신과 종료 처리 완료"))
                    .then(completedOrders.asMono());
        }).timeout(Duration.ofSeconds(20))
                .doFinally(signal -> log.info("[전체 흐름 종료] {}", signal));
    }
}
