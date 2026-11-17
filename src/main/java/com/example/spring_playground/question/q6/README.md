# WebSocket 기본 사용 흐름과 비동기 처리

학습 코드는 다음 패키지로 구분한다.

- `ws`: 기존 WebSocket 서버·클라이언트 실험. [JDK 클라이언트](./ws/client/README.md), [Reactor 클라이언트](./ws/reactor/README.md), [서버 안내](./ws/server/README.md).
- `async`: [비동기 프로그래밍 기초 학습](./async/README.md). 커피숍 예제로 Java 5·8·11·21의 기본 흐름을 학습했다.

WebSocket의 기본 생명주기를 확인한 뒤 비동기 프로그래밍 기초를 먼저 학습하고, 이후 WebSocket에 Reactor를 적용한다.

## 1. 문제 상황 및 학습 이유

Trady 프로젝트의 `KiwoomRealtimeClient` 코드를 이해하기 위한 학습이다. 
이 코드는 WebSocket으로 연결한 뒤 로그인, 주식체결 구독, 실시간 데이터 수신, PING 응답을 처리한다. 
이 과정에 Reactor의 `Mono`, `Flux`, `Sinks` 등이 함께 사용되어 WebSocket의 역할과 비동기 처리 흐름을 나누어 익힐 필요가 있다.

먼저 WebSocket의 기본 개념과 연결·송신·수신·종료 흐름을 공부한다.
그다음 비동기 처리를 적용하고, 제공된 코드에서 각 메서드가 어떤 역할을 하며 어떻게 연결되는지 이해한다. 
프레임워크 기본 메서드를 직접 사용하고 실행 흐름을 따라갈 수 있는 수준을 목표로 한다.

## 2. 학습 질문

### 1단계: WebSocket 기본 사용

- WebSocket은 어떤 문제를 해결하며, 일반적인 HTTP 요청·응답과 통신 흐름이 어떻게 다른가?
- 클라이언트와 서버는 어떻게 연결하고, 연결을 유지하면서 서로 메시지를 주고받는가?
- 연결, 메시지 송신, 메시지 수신, 연결 종료에 사용하는 기본 메서드는 무엇인가?
- 로그인 → 구독 요청 → 실시간 데이터 수신 순서는 어떻게 구성하는가?
- 예제 코드의 JSON `PING` 메시지에 대한 응답은 어디에서 처리하는가?

### 2단계: 비동기 적용과 제공된 코드 읽기

- 비동기 처리에서 작업을 구성하는 시점과 실제 실행되는 시점은 어떻게 다른가?
- `Mono`와 `Flux`는 각각 무엇을 나타내며, 구독은 어떤 역할을 하는가?
- `execute()`, `send()`, `receive()`, `textMessage()`, `getPayloadAsText()`는 어떻게 사용하는가?
- `defer()`, `fromRunnable()`, `map()`, `concatMap()`, `then()`, `when()`, `Flux.concat()`은 실행 흐름을 어떻게 구성하는가?
- `Sinks.Many`와 `Sinks.One`은 각각 송신 메시지 전달과 로그인·구독 완료 신호에 어떻게 사용되는가?
- `timeout()`, `doOnSuccess()`, `doFinally()`는 응답 대기, 정상 완료, 종료 처리에 어떻게 사용되는가?
- 제공된 코드에서 연결 성공, 로그인 성공, 구독 성공, 연결 종료는 각각 어느 시점을 의미하는가?

## 3. 학습 범위와 실험 조건

- 학습 범위: WebSocket의 목적과 기본 생명주기, 메시지 송수신, 기본 메서드 사용법, Reactor를 이용한 비동기 흐름 구성.
- 학습 깊이: 연결과 메시지 처리의 주요 흐름까지 다룬다. 네트워크 프레임의 세부 구조, Reactor·Netty 내부 구현, 고급 성능 최적화는 현재 범위에서 제외한다.
- 단계 구분: 1단계는 통신 동작에 집중하고, 2단계에서 비동기 연산자와 완료·오류·종료 신호를 설명한다. 1단계 예제에서도 사용하는 라이브러리에 필요한 최소한의 비동기 코드는 등장할 수 있다.
- 외부 연동: 실제 키움 서버, 접근토큰 발급, 종목 DB 조회, 이벤트 후속 비즈니스 로직은 모의 구현으로 대체한다. WebSocket 통신 자체는 실제 클라이언트와 로컬 모의 서버 사이에서 확인한다.
- 메시지 가정: 제공된 코드의 `LOGIN`, `REG`, `REAL`, JSON `PING` 흐름을 학습용으로 단순화한다. 키움의 전체 공식 계약을 재현한다고 가정하지 않으며, JSON `PING`과 WebSocket 프로토콜의 Ping/Pong 프레임은 구분한다.
- 실행 환경 및 관련 버전: 예제 구현 시 프로젝트 의존성을 확인하여 기록한다.
- 주요 설정값: 서버 주소, 입력 메시지, 응답 제한시간 등은 실험 구현 시 기록한다. 지연이 필요한 경우에만 1초 이상으로 설정하고 쉽게 변경할 수 있게 한다.
- 관찰 방법: 메시지 내용과 처리 순서는 단언문으로 검증하고, 실행 시점·스레드·종료 신호 등 흐름 이해에 필요한 정보는 로그로 확인한다.

## 5. 실험 및 관찰 결과

커피숍 WebSocket 서버와 JDK·Reactor 클라이언트 및 통합 테스트를 구현했다. WebSocket과 비동기 예제는 동일한 주문 데이터와 CafeSystem을 사용한다. 실험 2의 세부 학습과 관찰은 아래 항목에 기록한다.

### 실험 1. WebSocket 연결과 기본 송수신

- 확인할 내용: 로컬 서버 연결 → 클라이언트 메시지 송신 → 서버 응답 수신 → 연결 종료 흐름과 각 단계의 기본 메서드.
- 관련 코드: [CafeOrderClient.java](./ws/client/CafeOrderClient.java), [서버 설정](./ws/server/LearningWebSocketConfig.java), [수신 핸들러](./ws/server/LearningWebSocketHandler.java). 실행 방법과 메서드 설명은 [서버 사용 안내](./ws/server/README.md)를 참고한다.
- 직접 실행: `.\gradlew.bat bootRun --args="--spring.profiles.active=q6-websocket"`으로 서버를 켠 뒤 IDE에서 `CafeOrderClient.main()`을 실행한다. 연결 → 주문 1·2·3 전송 및 제작 완료 수신 → 종료 로그를 확인한다.
- 실행 방법: 프로젝트 루트에서 `.\gradlew.bat test --tests '*LearningWebSocketServerTest'` 실행.
- 실행 조건: `q6-websocket` 프로필, 임의 서버 포트, 로컬 JDK WebSocket 클라이언트. 공통 CafeSystem의 라떼 2초·아메리카노 1초 지연을 사용하고 외부 API는 호출하지 않는다.
- 실행 상태: 실행 완료(2026-11-16), 테스트 1개 성공.
- 관찰 결과: 같은 연결에서 주문 번호 1·2·3을 순서대로 보내 각 주문의 전체 제작 완료 문자열을 수신했고, 클라이언트가 연결 종료를 요청한 후 정상 종료 코드 `1000`을 수신했다. 인사 예제를 커피숍으로 변경한 뒤 통합 테스트를 다시 실행해 확인했다.

### 실험 1-2. 연결과 Ping/Pong

- 관련 코드: [ConnectAndPingClient.java](./ws/client/ConnectAndPingClient.java). [실행 안내](./ws/client/README.md)를 따라 서버와 클라이언트 `main()`을 각각 실행한다.
- 확인할 내용: JDK 기본 WebSocket 클라이언트로 연결하고 Ping 프레임을 한 번 전송한 뒤 Pong을 받고 종료한다. 로그인·구독은 추가하지 않는다.
- 실행 방법: `.\gradlew.bat test --tests '*LearningWebSocketServerTest'`.
- 실행 상태: 실행 완료(2026-11-16). 커피 주문 테스트와 Ping 테스트 총 2개 성공.
- 관찰 결과: 연결 요청 → 연결 완료 → Ping 전송 요청 → Pong 수신 → 종료 요청 → 정상 종료(`1000`) 로그를 확인했다. Pong 내용은 보낸 `cafe-ping`과 일치했다.
- 구분: 프로토콜 Ping/Pong이며, Trady의 JSON `PING` 메시지 처리는 아직 미확인이다.

### 실험 2. 비동기 흐름과 완료·오류 처리

- 확인할 내용: 앞서 익힌 통신 흐름에 `Mono`, `Flux`, `Sinks`를 연결하고, 송신·수신 작업의 구성과 실행을 구분한다.
- 커피 주문의 송신·제작 완료 응답, 제한시간 초과, 연결 종료 시 처리 흐름을 확인한 뒤 Trady의 로그인·구독·체결 수신 코드와 연결해 읽는다.
- 관련 코드와 실행 방법: 아래 실험 2-1부터 2-8까지의 개별 항목과 링크를 참고한다.
- 실행 상태: 기초 Reactor 예제 및 WebSocket 정상 송수신 실행 완료. WebSocket 제한시간 초과와 연결 장애에 대한 별도 실패 실험은 미실행.
- 관찰 결과: 각 하위 실험에 기록한다. Trady의 로그인·구독·체결 데이터 연동은 아직 검증하지 않았다.

### 실험 2-1. WebSocket 적용 전 Mono 구성과 구독

- 확인할 내용: 커피 제작을 Mono.fromCallable()로 구성한 시점과 subscribe()로 실행하는 시점을 구분한다.
- 관련 코드: [MonoBaseMain.java](./async/reactor/mono/MonoBaseMain.java), [실행 안내](./async/reactor/mono/README.md).
- 실행 방법: IDE에서 MonoBaseMain.main() 실행. 별도 서버는 사용하지 않는다.
- 실행 조건: 주문 1번, 라떼 2초·아메리카노 1초, 별도 스케줄러 없음.
- 실행 상태: 로컬 JDK 21과 캐시의 Reactor Core 3.8.7로 직접 컴파일·실행 완료. Gradle 실행은 다운로드 차단으로 미확인.
- 관찰 결과: 구성 시 제작은 실행되지 않았다. 구독 후 제작·결과 처리·subscribe() 반환 로그가 순서대로 출력됐고 모두 main에서 실행됐다.

### 실험 2-2. Flux로 여러 주문 전달

- 확인할 내용: Flux에서 여러 주문이 전달될 때 주문별 처리와 전체 정상 완료를 구분한다.
- 관련 코드: [FluxBaseMain.java](./async/reactor/flux/FluxBaseMain.java), [실행 안내](./async/reactor/flux/README.md).
- 실행 방법: IDE에서 FluxBaseMain.main() 실행. 별도 서버는 사용하지 않는다.
- 실행 조건: 공통 주문 1·2·3번, 라떼 2초·아메리카노 1초, 별도 스케줄러 없음.
- 실행 상태: 로컬 JDK 21과 캐시의 Reactor Core 3.8.7로 직접 컴파일·실행 완료.
- 관찰 결과: 구성 시 제작은 실행되지 않았다. 구독 후 세 주문이 main에서 순서대로 전달·제작됐고, 전체 정상 완료 함수는 한 번 실행됐다. 이후 subscribe()가 반환됐다.

### 실험 2-3. Mono와 Flux의 완료·오류·취소

- 관련 코드: [MonoLifecycleMain.java](./async/reactor/mono/MonoLifecycleMain.java), [FluxLifecycleMain.java](./async/reactor/flux/FluxLifecycleMain.java). 기존 입문 코드는 MonoBaseMain·FluxBaseMain으로 이름을 변경했다.
- 실행 방법: 각 main()을 IDE에서 실행한다. 별도 서버나 스케줄러는 사용하지 않는다.
- 실행 조건: 공통 주문과 제작 지연을 사용한다. Mono는 제작 전 취소, Flux는 첫 제작 결과를 받은 직후 취소한다. 실패는 학습용으로 의도적으로 발생시킨다.
- 실행 상태: 로컬 JDK 21과 캐시의 Reactor Core 3.8.7로 직접 컴파일·실행 완료.
- 관찰 결과: 정상은 onNext 후 onComplete, 실패는 onError, 취소는 doFinally의 cancel로 관찰됐다. Flux 오류 시 3번 제작이 없었고, 취소 시 2·3번 제작이 없었다. 오류·취소 상황에서는 정상 완료 함수가 실행되지 않았다.
- 한계: 취소 전에 이미 제작한 커피는 되돌리지 않는다. 이미 실행 중인 외부 작업의 즉시 중단 여부를 확인한 실험은 아니다.

### 실험 2-4. 기본 연산자로 처리 연결

- 확인할 내용: map의 값 변환, then의 정상 완료 후 연결, concatMap의 순차 흐름 연결, Mono.when의 전체 완료 구분.
- 관련 코드: [MapMain](./async/reactor/operators/MapMain.java), [ThenMain](./async/reactor/operators/ThenMain.java), [ConcatMapMain](./async/reactor/operators/ConcatMapMain.java), [WhenMain](./async/reactor/operators/WhenMain.java).
- 실행 방법: 각 main()을 IDE에서 실행한다. [실행 안내](./async/reactor/operators/README.md)를 참고한다.
- 실행 조건: 공통 주문과 제작 지연을 사용하며 서버·별도 스케줄러는 사용하지 않는다.
- 실행 상태: JDK 21과 로컬 캐시의 Reactor Core 3.8.7로 네 예제 직접 컴파일·실행 완료.
- 관찰 결과: map은 안내문 변환, then은 제작 후 알림, concatMap은 1·2·3번 순차 제작, when은 두 주문 완료 후 전체 완료를 확인했다. then·when의 최종 Mono<Void>는 onNext를 호출하지 않았다. 모두 main에서 실행됐다.
- 한계: Mono.when의 병렬 실행이나 고급 스케줄링을 검증한 것은 아니다.

### 실험 2-5. Sinks로 완료 알림 전달

- 확인할 내용: 제작 코드가 직접 알림을 출력하는 대신 Sinks에 값을 넣고 구독한 코드가 알림을 처리한다.
- 관련 코드: [SinksOneMain](./async/reactor/sinks/SinksOneMain.java), [SinksManyMain](./async/reactor/sinks/SinksManyMain.java), [실행 안내](./async/reactor/sinks/README.md).
- 실행 방법: 각 main()을 IDE에서 실행한다. 서버와 별도 스레드는 사용하지 않는다.
- 실행 조건: 공통 주문과 제작 지연을 사용한다. One은 주문 1번, Many는 주문 1·2·3번을 사용한다.
- 실행 상태: 로컬 JDK 21과 캐시의 Reactor Core 3.8.7로 직접 컴파일·실행 완료.
- 관찰 결과: One의 tryEmitValue()는 알림 한 번과 정상 완료를 전달했다. Many는 tryEmitNext()마다 알림을 전달했고, tryEmitComplete()에서 정상 완료했다. 수신 처리는 main에서 실행됐다.
- 한계: 다중 생산자, 여러 구독자, 버퍼 포화나 스케줄링은 검증하지 않았다.

### 실험 2-6. Sinks.One의 구독·전달 스레드 비교

- 관련 코드: [SinksOneMain](./async/reactor/sinks/SinksOneMain.java)의 sameThread()와 differentThread(). main은 두 실험을 호출한다.
- 실행 조건: 동일한 주문 1번과 제작 지연을 사용한다. 두 경우 모두 main에서 먼저 구독하고, B에서만 직접 만든 cafe-worker에 제작과 알림 전달을 맡긴다. Reactor 스케줄러는 없다.
- 실행 상태: JDK 21과 Reactor Core 3.8.7로 직접 컴파일·실행 완료.
- 관찰 결과: A에서는 제작·알림 수신이 main, B에서는 제작·tryEmitValue·알림 수신이 cafe-worker에서 실행됐다. 구독을 등록한 main으로 자동 복귀하지 않았다.
- 한계: 미리 구독한 수신자와 스케줄러 없는 현재 구성에서의 결과이며, 모든 Sinks 구성을 일반화한 것은 아니다.

### 실험 2-7. subscribeOn과 boundedElastic

- 관련 코드: [SubscribeOnMain](./async/reactor/scheduler/SubscribeOnMain.java), [실행 안내](./async/reactor/scheduler/README.md).
- 실행 방법: main()에서 withoutScheduler()와 withBoundedElastic()을 순서대로 실행한다.
- 실행 조건: 공통 주문 1번, 라떼 2초와 아메리카노 1초. B에서만 subscribeOn(Schedulers.boundedElastic())을 적용하며 마지막에 CompletableFuture.get()으로 전체 완료를 기다린다.
- 실행 상태: JDK 21과 로컬 캐시의 Reactor Core 3.8.7로 직접 컴파일·실행 완료.
- 관찰 결과: A의 제작·알림은 main에서 실행됐고 제작 완료 후 subscribe()가 반환됐다. B의 제작·알림은 boundedElastic-1에서 실행됐으며 main은 제작 완료 전에 다음 코드를 실행했다.
- 한계: 블로킹 작업의 실행 위치를 옮긴 실험이다. 작업 자체의 non-blocking 전환, 스케줄러 포화나 성능 우위를 검증하지 않았다.

### 실험 2-8. WebSocket 송수신을 Reactor로 구성

- 관련 코드: [ReactorCafeClient](./ws/reactor/ReactorCafeClient.java), [실행 안내](./ws/reactor/README.md).
- 실행 방법: 기존 q6-websocket 서버를 실행한 뒤 ReactorCafeClient.main()을 실행한다.
- 실행 조건: 기존 서버와 공통 세 주문, 전체 흐름 제한시간 20초. 송신·수신을 Mono.when으로 구성하고 세 응답 수신 후 닫는다.
- 실행 상태: Gradle은 다운로드 차단으로 실행하지 못했다. JDK 21과 로컬 라이브러리로 직접 컴파일하고 JUnit 통합 테스트 3개를 실행해 모두 통과했다.
- 관찰 결과: 세 주문 송신 후 Netty 스레드에서 제작 응답을 각각 받았다. 세 완료 문자열을 단언문으로 확인하고 서버의 정상 종료 코드 1000을 로그로 확인했다.
- 한계: 기존 서버는 블로킹 순차 제작을 유지한다. 무한 구독·로그인·실시간 푸시·재접속은 구현하지 않았다.

## 6. 결과 해석

실험 1에서는 하나의 WebSocket 연결로 여러 텍스트 메시지를 주고받을 수 있음을 확인했다. 서버는 메시지 수신 콜백에서 현재 세션으로 응답했다. 이는 로컬 커피숍 서버에서 확인한 결과이며, 키움 로그인·구독이나 Reactor의 실행 흐름을 검증한 것은 아니다. 이후 실험 결과를 Trady 코드에 연결해 기록한다.

## 7. 결론 및 적용 기준

- 학습 질문에 대한 답: 현재 미확인. 단계별 학습과 실험 후 작성한다.
- 학습 완료 기준: 제공된 코드의 연결 → 로그인 → 구독 → 실시간 수신 및 PING 응답 → 종료 흐름을 설명하고, 주요 메서드를 사용해 작은 예제를 작성할 수 있다.
- 이후 적용 기준: Trady 코드를 읽을 때 각 작업의 실행 시점, 메시지 전달 경로, 완료·오류 신호가 어디로 전달되는지 파악한다. 실제 코드 변경 여부는 학습 결과를 바탕으로 별도로 판단한다.
