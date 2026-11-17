# 비동기 프로그래밍 기초

커피숍 공통 시나리오로 Java 5·8·11·21의 비동기 실행과 결과 처리 흐름을 학습했다. [버전별 학습 주제](./basic/README.md)와 [커피숍 시나리오 및 실행 기록](./basic/cafe/README.md)을 참고한다. 가상 스레드의 내부 동작은 Java21AsyncMain 상단의 TODO로 남겨두었다.

| 패키지 | 내용 |
| --- | --- |
| `basic.cafe` | 공통 주문, 메모리 주문 DB, 커피 제작 시스템 |
| `basic.java5` | ExecutorService와 Future |
| `basic.java8` | CompletableFuture와 완료 후 처리 |
| `basic.java11` | HttpClient.sendAsync()와 로컬 모의 HTTP 서버 |
| `basic.java21` | 가상 스레드 실행 |
| `reactor.mono` | [Mono 입문 및 완료·오류·취소](./reactor/mono/README.md) |
| `reactor.flux` | [Flux 입문 및 완료·오류·취소](./reactor/flux/README.md) |
| `reactor.operators` | [map·then·concatMap·Mono.when](./reactor/operators/README.md) |
| `reactor.sinks` | [Sinks.One·Many로 완료 알림 전달](./reactor/sinks/README.md) |
| `reactor.scheduler` | [subscribeOn과 boundedElastic](./reactor/scheduler/README.md) |

학습 질문:

- 동기와 비동기는 호출 이후의 실행 순서가 어떻게 다른가?
- 작업을 시작하는 시점과 결과를 받는 시점은 어떻게 다른가?
- 콜백은 언제, 어떤 스레드에서 실행되는가?
- `CompletableFuture`의 `complete()`와 `get()`은 어떤 관계인가?
- 결과를 기다리는 방식과 완료 후 처리할 함수를 등록하는 방식은 어떻게 다른가?
- 이 기본 흐름이 Reactor의 `Mono`, `Flux`, `Sinks`와 어떻게 연결되는가?

각 버전 패키지의 JavaNAsyncMain.main()을 IDE에서 실행한다. 프로젝트 JDK는 21 이상을 사용한다. Reactor와의 연결은 이후 학습 주제이며, q6 전체 내용은 [학습 기록](../README.md)을 참고한다.
