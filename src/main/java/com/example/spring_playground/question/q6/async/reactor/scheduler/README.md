# subscribeOn과 boundedElastic

[SubscribeOnMain](./SubscribeOnMain.java)의 main()을 IDE에서 실행한다. 서버 없이 공통 주문 1번(라떼 2초와 아메리카노 1초)을 사용한다. main은 비교용 내부 메서드 두 개만 호출한다.

| 메서드 | 제작·결과 처리 스레드 | main의 subscribe() 다음 코드 |
| --- | --- | --- |
| withoutScheduler() | main | 제작과 결과 처리 후 실행 |
| withBoundedElastic() | 기본 설정에서는 boundedElastic 작업 스레드 | 제작 완료를 기다리지 않고 실행 |

- subscribeOn(): 구독과 원본 작업의 실행 위치를 지정한다. 스케줄러를 지정한 것만으로 실행되지는 않으며 subscribe()가 필요하다.
- Schedulers.boundedElastic(): sleep이나 블로킹 DB 조회 같은 대기 작업을 맡길 수 있는 공유 스케줄러다. 무제한 실행 자원을 제공하지 않으며 이번 실험에서 한계값이나 내부 구현은 다루지 않는다.
- CompletableFuture와 get(): B 실험의 완료를 main에서 마지막에 기다리기 위해 사용한다. 별도 실행을 시작하는 부분도, 콜백을 main으로 되돌리는 부분도 아니다.

B의 제작 시작 로그와 main의 다음 코드 로그 사이 순서는 스케줄링에 따라 바뀔 수 있다. 이번 조건에서는 제작에 3초가 걸리므로 main의 subscribe() 반환 로그가 제작 완료 전에 나오는지 확인한다. 수신 처리가 main으로 자동 복귀하지 않는 것도 스레드 이름으로 확인한다.

블로킹 제작 작업을 다른 스레드로 옮긴 것이며, 제작 자체가 non-blocking으로 바뀌거나 제작 시간이 단축된 것은 아니다. publishOn()이나 여러 스케줄러의 조합은 추가하지 않는다.

참고: [Reactor 공식 블로킹 작업 감싸기 가이드](https://projectreactor.io/docs/core/release/reference/faq.html#faq.wrap-blocking).

실행 확인: 로컬 JDK 21과 캐시의 Reactor Core 3.8.7로 직접 컴파일·실행했다. A는 main에서 제작과 알림이 끝난 뒤 subscribe()가 반환됐다. B는 subscribe() 반환과 main의 다음 작업 로그가 제작 완료 전에 출력됐고, 제작·알림·완료 함수는 boundedElastic-1에서 실행됐다. main은 마지막 get() 이후 전체 완료를 확인했다.
