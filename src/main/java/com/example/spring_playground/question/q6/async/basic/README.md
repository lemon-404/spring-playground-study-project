# Java 버전별 비동기 프로그래밍 학습 주제

| 구분 | 관련 코드 |
| --- | --- |
| 공통 커피숍 | [시나리오](./cafe/README.md), [CafeSystem](./cafe/CafeSystem.java), [Order](./cafe/Order.java), [OrderDb](./cafe/OrderDb.java) |
| Java 5 | [Java5AsyncMain](./java5/Java5AsyncMain.java) |
| Java 8 | [Java8AsyncMain](./java8/Java8AsyncMain.java) |
| Java 11 | [Java11AsyncMain](./java11/Java11AsyncMain.java), [모의 HTTP 서버](./java11/CafeHttpServer.java) |
| Java 21 | [Java21AsyncMain](./java21/Java21AsyncMain.java) |

## Java 5: 작업 실행과 결과 대기

- 동기 호출과 비동기 작업의 실행 흐름 차이.
- `ExecutorService`에 작업 실행을 맡기는 방식.
- `Future`가 나타내는 작업 결과와 완료 상태.
- `get()`으로 결과를 기다릴 때 호출 스레드가 대기한다는 의미.
- 작업을 실행하는 스레드와 결과를 사용하는 스레드의 구분.

## Java 8: 완료 후 처리 연결

- `CompletableFuture`가 나타내는 결과와 완료·실패 상태.
- `supplyAsync()`와 `runAsync()`의 역할.
- `complete()`로 결과를 직접 전달하는 방식.
- `get()`으로 결과를 기다리는 방식과 완료 후 처리를 등록하는 방식의 차이.
- `thenApply()`를 통한 결과 변환과 `thenAccept()`를 통한 결과 사용.
- 비동기 작업의 기본 예외 처리.

## Java 11: 비동기 통신 API

- 표준 `HttpClient`의 동기 요청과 비동기 요청 차이.
- `sendAsync()`가 반환하는 `CompletableFuture`의 의미.
- 요청 시작, 응답 도착, 응답 처리 시점의 구분.
- 기존 WebSocket 예제의 `buildAsync()`와 수신 콜백의 관계.
- 네트워크 송신 완료와 상대 응답 수신의 차이.

## Java 21: 가상 스레드

- 플랫폼 스레드와 가상 스레드의 기본 차이.
- 가상 스레드에서 작업을 실행하는 방식.
- 대기하는 작업이 많을 때 가상 스레드가 해결하려는 문제.
- 순차적으로 작성한 코드에서도 여러 작업을 동시에 진행할 수 있다는 의미.
- 가상 스레드의 역할과 `CompletableFuture`의 역할 차이.

## 학습 범위

각 방식에서 작업을 시작하고, 다른 일을 진행하고, 결과를 받는 전체 흐름을 이해한다. 스레드 내부 구현, 모든 메서드의 상세 사용법, 성능 최적화는 현재 학습 범위에 포함하지 않는다.
