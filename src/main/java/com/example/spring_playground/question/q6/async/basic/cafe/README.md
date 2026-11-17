# 비동기 학습용 커피숍 모의 시스템

## 목적

같은 주문과 제작 작업을 서로 다른 Java 비동기 API로 실행하면서 주문 접수, 작업 시작, 결과 확인의 전체 흐름을 관찰한다. 실제 입력, DB 연결, 결제, 재고 관리, 커피 제조 로직은 구현하지 않는다.

## 공통 구성

| 구성 | 역할 |
| --- | --- |
| [Order](./Order.java) | 주문 번호와 주문 목록을 담는다. |
| [OrderDb](./OrderDb.java) | 모든 실험에 동일한 고정 주문을 제공하는 메모리 DB다. |
| [CafeSystem](./CafeSystem.java) | 주문 접수, 메뉴 제작, 제작 완료 로그와 결과 반환을 담당한다. |
| 버전별 main | 주문을 조회하고 반복하면서 CafeSystem을 호출한다. 비동기 실행 방식과 결과 확인만 달리한다. |

## 주문 데이터

주문은 `[주문 번호, [주문 목록]]` 형태다.

| 주문 번호 | 주문 목록 |
| --- | --- |
| 1 | 라떼, 아메리카노 |
| 2 | 아메리카노 |
| 3 | 라떼, 라떼 |

같은 메뉴가 두 번 있으면 두 잔을 순서대로 제작한다. 주문 데이터는 변경할 수 없는 목록으로 제공하며 입력은 받지 않는다.

## 실행 시나리오

1. main에서 OrderDb의 주문 목록을 조회한다.
2. 반복문에서 CafeSystem.receiveOrder()를 호출하여 주문을 접수한다.
3. 해당 버전의 비동기 API로 CafeSystem.prepareOrder() 실행을 맡긴다.
4. main은 이전 주문의 제작 완료를 기다리지 않고 다음 주문을 접수한다.
5. 맡긴 모든 주문의 완료를 확인한 뒤 종료한다.

주문 간에는 비동기로 제작하고, 한 주문 안에서는 목록 순서대로 메뉴를 제작한다. 실제 주문 간 실행 순서는 스레드 스케줄링에 따라 달라질 수 있다.

## 제작 작업과 로그

주문 하나의 제작 흐름은 제작 시작 → 각 메뉴의 제작 시작·완료 → 전체 제작 완료다. 메뉴별 작업은 실제 로직 없이 Thread.sleep()으로만 재현한다. 로그의 실행 시각, 주문 번호, 메뉴, 스레드 이름으로 접수와 제작을 구분한다.

- 기본 지연: 라떼 한 잔은 2,000ms(2초), 아메리카노 한 잔은 1,000ms(1초).
- 지연 변경: 라떼는 prepareCoffee()의 2,000ms 값을 변경한다. 그 외 메뉴는 CafeSystem 생성자의 preparationMillis 인자로 설정하며 1,000ms 이상을 사용한다.
- 제작 결과: 주문 번호를 포함한 전체 제작 완료 문자열.
- 인터럽트: 제작 중 대기가 중단되면 InterruptedException을 호출자에게 전달하며 정상 완료로 처리하지 않는다.

CafeSystem 자체에는 스레드 생성이나 비동기 API를 넣지 않는다. 버전별 main이 실행을 맡기며, CafeSystem은 호출한 스레드에서 동작한다. 제작 요청 직후 결과를 기다리면 다음 주문 접수가 지연되므로, 각 주문을 맡긴 뒤 전체 결과를 확인하는 흐름으로 사용한다.

## 현재 상태

공통 모의 시스템과 [Java 5 예제](../java5/Java5AsyncMain.java), [Java 8 예제](../java8/Java8AsyncMain.java), [Java 11 예제](../java11/Java11AsyncMain.java), [Java 21 예제](../java21/Java21AsyncMain.java)를 작성했다. 프로젝트의 Java 도구 체인은 가상 스레드 API 사용을 위해 21로 설정했다. 주문에 record를 사용하므로 Java 5·8 등의 학습은 해당 시기에 도입된 비동기 API의 사용 방식을 뜻하며 실제 구버전 JDK에서의 소스 호환성을 뜻하지 않는다.

## Java 21의 가상 스레드

- 실행: IDE에서 Java21AsyncMain.main()을 실행한다. JDK 21 이상이 필요하며 별도 서버는 사용하지 않는다.
- 구성: newVirtualThreadPerTaskExecutor()로 주문마다 가상 스레드를 만들고, 결과 알림은 Java 8과 동일하게 thenAccept()로 등록한다.
- 대기: main은 마지막 allOf(...).join()에서 제작과 알림 완료를 기다린다. try 블록 종료 시 실행기의 close()가 호출되어 종료도 기다린다.
- 관찰: JDK 21로 직접 컴파일·실행하여 세 주문이 각각 다른 가상 스레드에서 시작하고 virtual=true임을 확인했다. 이 실행에서는 2번 → 1번 → 3번 순으로 알림이 출력됐고 정상 종료했다. 순서는 보장하지 않는다.
- 비교 한계: Java 8 예제는 작업 스레드 두 개, 이번 예제는 주문마다 가상 스레드를 사용한다. 전체 시간 차이만으로 가상 스레드 자체의 성능 우위를 판단할 수 없다.
- API 참고: [Executors 공식 문서](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/concurrent/Executors.html), [ExecutorService 공식 문서](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/concurrent/ExecutorService.html).

## Java 11의 모의 HTTP 환경

[CafeHttpServer](../java11/CafeHttpServer.java)는 main과 같은 프로세스에서 시작하는 로컬 학습용 서버다. 서버 구현은 학습 보조 코드이며 CafeSystem의 주문 제작을 그대로 호출한다. main을 실행하면 서버도 자동으로 시작·종료하므로 별도 서버 실행은 필요 없다.

- 요청: POST /orders/{주문번호}. 본문은 없으며 서버가 공통 OrderDb에서 메뉴를 조회한다.
- 응답: 제작 완료 후 HTTP 200과 UTF-8 완료 문자열을 반환한다.
- 조건: 서버 제작 직원 두 명, 라떼 2초, 그 외 메뉴 기본 1초, 클라이언트 요청 제한시간 15초.
- 가정: 이 API는 학습을 위해 정한 모의 계약이다. 실제 외부 API의 계약이나 동작을 검증하지 않는다.
- 관찰: 로컬 JDK로 컴파일하고 main을 실행하여 서버의 세 주문 요청 수신, 응답별 완료 알림, 전체 완료 후 서버 종료를 확인했다. 해당 실행에서는 HTTP 요청 도착 순서가 제출 순서와 달랐으며 완료 알림은 2번 → 3번 → 1번이었다. 이 순서를 보장하지 않는다.
- 핵심 API: [HttpClient.sendAsync() 공식 문서](https://docs.oracle.com/en/java/javase/11/docs/api/java.net.http/java/net/http/HttpClient.html). 결과 처리는 Java 8과 동일한 CompletableFuture를 사용한다.
