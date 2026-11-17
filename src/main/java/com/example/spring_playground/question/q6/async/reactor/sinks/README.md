# Sinks로 알림 발생과 처리 분리하기

서버 없이 각 main()을 IDE에서 실행한다. 기존 CafeSystem과 주문 데이터를 사용한다. SinksOneMain은 동일 스레드와 별도 제작 스레드를 비교하고, SinksManyMain은 main에서 순차 실행한다.

| 예제 | 제작 쪽에서 하는 일 | 구독한 쪽에서 하는 일 |
| --- | --- | --- |
| [SinksOneMain](./SinksOneMain.java) | 주문 1번 제작 후 tryEmitValue() | asMono()로 결과 하나를 받고 알림 출력 |
| [SinksManyMain](./SinksManyMain.java) | 주문별 tryEmitNext(), 마지막 tryEmitComplete() | asFlux()로 알림을 여러 번 받고 전체 완료 확인 |

Sinks는 값을 넣는 입구이고 asMono()·asFlux()는 그 신호를 전달받는 흐름이다. 이번 구독은 제작 함수를 실행하지 않고 수신 처리를 등록한다. 제작은 이후 main의 별도 코드에서 수행한다.

One의 tryEmitValue()는 값과 정상 완료를 함께 전달한다. Many는 각 값 전달 후에도 열려 있으며, 마지막에 tryEmitComplete()로 종료한다. Trady처럼 값 없이 완료만 알리려면 Sinks.One<Void>와 tryEmitEmpty()를 사용할 수 있으나 이번 예제에서는 알림 문장을 직접 확인하기 위해 String을 사용한다.

unicast()는 구독자 한 명을 허용하며, onBackpressureBuffer()는 아직 전달할 수 없는 값을 버퍼에 보관하는 방식이다. Many는 여러 구독자가 아닌 여러 값이라는 뜻이다. 버퍼와 고급 동시성 옵션은 이번 범위에서 다루지 않는다.

tryEmit 계열은 성공·실패를 EmitResult로 반환한다. 구독한 수신 함수가 같은 main 스레드에서 즉시 실행될 수 있으므로, 수신 로그가 tryEmit 호출 반환 로그보다 먼저 나오는 것도 확인한다. Sinks 자체가 비동기 실행이나 별도 스레드를 만들어주는 것은 아니다.

참고: [Reactor 공식 Sinks 문서](https://projectreactor.io/docs/core/release/reference/coreFeatures/sinks.html).

실행 확인: 로컬 JDK 21과 캐시의 Reactor Core 3.8.7로 두 예제를 직접 컴파일·실행했다. One은 알림 한 번 후 정상 완료했고, Many는 주문 1·2·3의 알림을 받은 후 명시적인 완료 신호에서 종료했다. 모든 수신 처리는 main에서 실행됐으며, 수신·완료 로그는 emit 호출 반환 로그보다 먼저 출력됐다.

## One의 구독 스레드와 알림 전달 스레드 비교

SinksOneMain.main()은 다음 두 내부 메서드를 순서대로 호출한다. Sinks.One은 한 번만 완료할 수 있으므로 각 실험에서 새 객체를 만든다.

| 메서드 | 구독 등록 | 제작·tryEmitValue 호출 | 실제 수신 함수 실행 |
| --- | --- | --- | --- |
| sameThread() | main | main | main |
| differentThread() | main | cafe-worker | cafe-worker |

differentThread()는 직접 만든 ExecutorService에 제작을 맡긴다. main이 get()에서 기다리는 동안 수신 함수도 cafe-worker에서 실행된다. get()이 알림 처리를 main으로 옮기는 것은 아니다. 두 실험 모두 별도 Reactor 스케줄러는 지정하지 않는다.

추가 실행 확인: JDK 21과 Reactor Core 3.8.7로 SinksOneMain을 직접 컴파일·실행하여 표의 스레드를 확인했다. 양쪽 모두 수신·완료 로그 후 tryEmitValue()가 OK로 반환됐다. 이는 Sinks의 신호 수락 결과이며 외부 작업 성공을 보장하는 의미는 아니다. 구독 시점과 스케줄러 구성이 바뀌면 실행 스레드도 달라질 수 있다.
