package com.example.spring_playground.question.q6.ws.server;

import java.io.IOException;

import com.example.spring_playground.question.q6.async.basic.cafe.CafeSystem;
import com.example.spring_playground.question.q6.async.basic.cafe.Order;
import com.example.spring_playground.question.q6.async.basic.cafe.OrderDb;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

/** 주문 번호를 받아 커피를 제작하고 완료 응답을 보내는 학습용 서버. */
public class LearningWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(LearningWebSocketHandler.class);
    private final CafeSystem cafe = new CafeSystem();
    private final OrderDb orderDb = new OrderDb();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        // HTTP 연결 요청이 WebSocket으로 전환된 후 호출된다.
        log.info("[연결] session={}, thread={}", session.getId(), Thread.currentThread().getName());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws IOException {
        // 1. 클라이언트가 보낸 텍스트 메시지의 내용을 읽는다.
        String received = message.getPayload();
        log.info("[수신] session={}, text={}, thread={}",
                session.getId(), received, Thread.currentThread().getName());

        // 2. 주문 번호로 공통 주문 DB에서 메뉴를 조회하고 제작한다.
        String reply;
        try {
            int number = Integer.parseInt(received);
            Order order = orderDb.findAll().stream()
                    .filter(candidate -> candidate.orderNumber() == number)
                    .findFirst().orElseThrow(() -> new IllegalArgumentException("없는 주문 번호"));
            cafe.receiveOrder(order);
            // 기본 흐름 실험: 수신 콜백에서 순차 제작한다. Reactor 적용 시 실행 위치를 비교한다.
            reply = cafe.prepareOrder(order);
        } catch (IllegalArgumentException e) {
            reply = "주문 번호를 확인하세요.";
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            session.close(CloseStatus.SERVER_ERROR);
            return;
        }

        // 3. 메시지를 보낸 클라이언트에게 답변을 전송한다.
        // 이 메서드는 다음 메시지를 기다리는 반복문이 아니다.
        // 메시지가 도착할 때마다 Spring이 호출한다.
        session.sendMessage(new TextMessage(reply));
        log.info("[송신 호출 완료] session={}, text={}", session.getId(), reply);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws IOException {
        log.warn("[통신 오류] session={}", session.getId(), exception);
        if (session.isOpen()) {
            session.close(CloseStatus.SERVER_ERROR);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        log.info("[종료] session={}, code={}, reason={}",
                session.getId(), status.getCode(), status.getReason());
    }
}
