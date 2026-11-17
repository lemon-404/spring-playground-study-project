# 커피숍 WebSocket + Reactor

## 실행

1. 기존 서버를 프로젝트 루트에서 실행한다.

   ```powershell
   .\gradlew.bat --console=plain bootRun --args="--spring.profiles.active=q6-websocket"
   ```

2. IDE에서 [ReactorCafeClient.main()](./ReactorCafeClient.java)을 실행한다.

서버 주소는 기존과 같은 ws://localhost:8080/q6/ws다. 한글 콘솔이 깨지면 [서버 안내](../server/README.md)의 UTF-8 설정을 확인한다.

## 확인할 흐름

흐름 구성 → 구독 → 연결 → 주문 1·2·3 송신 / 제작 완료 응답 수신 → 세 응답 수신 후 연결 종료 → 전체 결과 확인.

| 코드 | 역할 |
| --- | --- |
| Mono.defer() | 구독할 때 연결과 이번 실행의 Sinks.One 생성 |
| client.execute() | 연결하고 세션에서 수행할 송수신 흐름 실행 |
| Flux.fromIterable()·map() | 공통 주문을 전송할 주문 번호로 변환 |
| session.send()·textMessage() | 문자열 흐름을 WebSocket 메시지로 전송 |
| session.receive()·getPayloadAsText() | 도착하는 메시지를 텍스트로 읽기 |
| take()·collectList() | 주문 수만큼 응답을 받고 목록으로 모으기 |
| Mono.when() | 송신과 수신 흐름을 함께 구독하고 완료 조합 |
| Sinks.One·asMono() | 세션 안에서 모은 결과를 세션 밖의 반환 흐름에 전달 |
| session.close() | 모든 응답 수신 후 정상 종료 요청 |

기존 [CafeOrderClient](../client/CafeOrderClient.java)는 주문 하나를 전송한 뒤 큐에서 응답을 기다려 다음 주문을 보낸다. 이번 클라이언트는 송신과 수신을 별도 흐름으로 구성하며 주문마다 poll()이나 get()을 호출하지 않는다. 송신 완료가 커피 제작 완료를 뜻하는 것은 아니다.

main의 마지막 block()은 한 번 구독하고 콘솔 예제의 전체 종료를 기다린다. 블로킹 대기는 main에서만 수행하며, 수신 콜백에서 호출하지 않는다. 별도로 subscribe()한 뒤 다시 block()하면 중복 연결이 생길 수 있으므로 이번 코드는 block() 한 번만 사용한다.

## 실험 조건과 범위

- 기존 Servlet WebSocket 서버와 공통 주문·CafeSystem을 그대로 사용한다. 라떼 2초, 아메리카노 1초다.
- 서버는 수신 콜백에서 순차 제작한다. Reactor 클라이언트로 바꿨다고 서버 제작이 병렬화되는 것은 아니다.
- 모든 송수신의 제한시간은 전체 20초, main의 대기는 최대 30초다.
- 각 주문에 응답 한 개가 오는 학습용 계약이다. 주문 수보다 적게 응답하고 연결이 종료되면 실패한다. 실제 서비스의 메시지 분류·요청 ID 매칭·무한 구독·로그인·재접속은 추가하지 않는다.
- 새 의존성은 spring-webflux와 reactor-netty-http다. 기존 MVC 서버를 WebFlux 서버로 바꾸는 설정은 추가하지 않는다.
- 통합 테스트: `.\gradlew.bat test --tests '*LearningWebSocketServerTest'`.

참고: [Spring 공식 Reactive WebSocket 문서](https://docs.spring.io/spring-framework/reference/web/webflux-websocket.html).

## 관찰 결과

Gradle 실행은 배포 파일 다운로드가 차단되어 완료하지 못했다. 로컬 캐시와 기존 프로젝트 빌드의 라이브러리로 JDK 21 컴파일 후 JUnit 통합 테스트를 직접 실행했다. 기존 Ping·순차 주문 테스트와 새 Reactor 주문 테스트 총 3개가 통과했다.

이번 실행에서 Reactor 클라이언트는 세 주문을 먼저 송신했고, Netty의 reactor-http-nio 스레드에서 각 제작 응답을 받았다. 세 응답 문자열을 단언문으로 확인했고, 서버 로그에서 정상 종료 코드 1000을 확인했다. 실제 키움 서버나 장시간 연결은 검증하지 않았다.
