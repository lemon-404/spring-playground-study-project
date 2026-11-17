# 학습용 WebSocket 클라이언트

Reactor 방식의 같은 주문 예제는 [ReactorCafeClient](../reactor/README.md)에서 비교한다.

## 실험 1: 기본 텍스트 송수신

아래 방법으로 서버를 실행한 뒤 IDE에서 [CafeOrderClient.main()](./CafeOrderClient.java)을 실행한다. 같은 연결에서 공통 OrderDb의 주문 번호 1·2·3을 순서대로 전송하고 각 제작 완료 응답을 받은 뒤 종료한다. 연결·송신·수신·종료를 `log.info`로 확인한다.

공통 데이터와 제작 과정은 [커피숍 시나리오](../../async/basic/cafe/README.md)를 사용한다. 라떼는 2초, 아메리카노는 1초가 걸리며 주문별 응답을 최대 10초 기다린다. 이 기본 예제는 주문 하나의 응답을 받은 후 다음 주문을 보낸다. 이후 Reactor 예제에서 같은 도메인으로 실행 흐름을 비교한다.

| 전송 텍스트 | 서버가 조회하는 메뉴 | 응답 |
| --- | --- | --- |
| `1` | 라떼, 아메리카노 | 주문 1번 전체 제작 완료 |
| `2` | 아메리카노 | 주문 2번 전체 제작 완료 |
| `3` | 라떼, 라떼 | 주문 3번 전체 제작 완료 |

`sendText(text, true)`의 `true`는 이번 전송이 해당 텍스트 메시지의 마지막 조각임을 뜻한다. `onText()`의 `last`도 수신한 조각이 마지막인지 알려준다. `request(1)`로 다음 수신 콜백을 요청하고, 수신 내용은 큐를 통해 실행 스레드에 전달한다. 메시지마다 응답을 기다리는 예제이며 로그인·구독은 포함하지 않는다.

## 실험 1-2: 연결과 Ping/Pong

1. 프로젝트 루트에서 서버를 실행한다.

   ```powershell
   .\gradlew.bat bootRun --args="--spring.profiles.active=q6-websocket"
   ```

2. IDE에서 [ConnectAndPingClient.main()](./ConnectAndPingClient.java)을 실행한다.
3. 클라이언트 로그의 1~6 순서를 확인한다: 연결 요청 → 연결 완료 → Ping 전송 요청 → Pong 수신 → 종료 요청 → 종료 완료. 서버에서도 연결·종료 로그를 확인할 수 있다.

JDK 기본 `HttpClient`와 `WebSocket.Listener`를 사용한다. 로그인, 종목 구독, 재접속, 주기적 Ping은 추가하지 않았다. `buildAsync()`는 연결을 요청하고, `onOpen()`은 연결 완료를 알려준다. `sendPing()`은 Ping을 보내고, `onPong()`은 Pong을 받는다. `request(1)`은 수신 콜백 한 건을 요청하는 메서드이며 서버에 메시지를 보내는 메서드가 아니다.

`get(5, TimeUnit.SECONDS)`는 이 예제의 호출 스레드가 결과를 최대 5초 기다리게 한다. 네트워크 이벤트는 콜백으로 전달되며, `CompletableFuture`는 콜백 결과를 기다리는 데에만 사용한다. 송신 요청 완료와 상대 응답 수신은 별개다.

이번 Ping은 WebSocket 제어 프레임이다. 서버의 Tomcat WebSocket 구현이 Pong을 자동 응답하므로 기존 `handleTextMessage()`에는 들어오지 않는다. Trady 코드의 JSON `{"trnm":"PING"}`은 별도의 애플리케이션 텍스트 메시지이며 이번 학습 코드에서는 다루지 않는다.

Ping의 내용은 `cafe-ping`이며 커피숍 서버와의 연결 확인에만 사용한다. 주문 제작 메시지와는 별개다.

참고: [JDK WebSocket.Listener 문서](https://docs.oracle.com/en/java/javase/17/docs/api/java.net.http/java/net/http/WebSocket.Listener.html).

자동 검증: `.\gradlew.bat test --tests '*LearningWebSocketServerTest'`. 테스트에서는 임의 포트의 기존 학습 서버에 이 클라이언트를 연결해 Pong 내용을 검증한다.
