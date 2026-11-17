# Mono 구성과 구독

- 입문 예제: [MonoBaseMain](./MonoBaseMain.java)
- 완료·오류·취소: [MonoLifecycleMain](./MonoLifecycleMain.java)

[MonoBaseMain](./MonoBaseMain.java)의 main()을 IDE에서 실행한다. 별도 서버 없이 공통 커피숍 주문 1번(라떼와 아메리카노)을 사용한다.

- A 로그: Mono.fromCallable()로 제작 함수를 등록한다. 구성만으로 커피를 제작하는지 확인한다.
- B 로그: subscribe()를 호출해 제작 함수와 결과 처리 함수가 실행되는 순서를 확인한다.
- B 부분을 주석 처리하면 구독 없이 구성만 한 경우를 따로 확인할 수 있다.
- 별도 스케줄러를 사용하지 않는다. 제작·결과 처리·실행 종료 로그의 스레드가 모두 main인지 확인한다.

fromCallable()은 구독 시 실행할 작업을 구성하고, subscribe()는 구독과 결과 처리를 등록한다. Mono 전체가 언제나 지연 실행되거나, subscribe()가 언제나 작업 완료까지 기다린다는 의미는 아니다. 이번 예제는 fromCallable()과 스케줄러 없는 구성에서의 흐름만 확인한다.

실행 확인: Gradle 실행은 배포 파일 다운로드가 차단되어 진행하지 못했다. 로컬 캐시의 Reactor Core 3.8.7과 JDK 21로 직접 컴파일·실행했다. A 구간에는 제작 로그가 없었으며 B에서 제작 함수 → 결과 처리 → subscribe() 반환 순서가 출력됐다. 모두 main 스레드에서 실행됐다.

## 완료·오류·취소 실험

MonoLifecycleMain.main()을 실행하면 정상·오류·취소 상황을 순서대로 확인한다.

| 상황 | 확인할 흐름 |
| --- | --- |
| 정상 | 제작 결과 onNext → onComplete → doFinally(onComplete) |
| 오류 | 의도적인 재료 부족 → onError → doFinally(onError) |
| 취소 | 구독 직후 제작 전에 cancel → doFinally(cancel) |

doFinally()는 종료 이유를 관찰하는 용도다. 취소는 구독자가 위쪽으로 보내는 중단 요청이므로 정상 완료나 오류 콜백이 호출되는 것과 다르다. 취소 시점을 직접 지정하려고 취소 예제에서만 BaseSubscriber를 사용한다. BaseSubscriber의 모든 메서드를 학습할 필요는 없다.

별도 스레드 없이 제작 전에 취소한 사례이며, 이미 실행 중인 외부 작업을 즉시 중단하거나 되돌릴 수 있음을 검증한 실험은 아니다. JDK 21과 Reactor Core 3.8.7로 직접 실행해 표의 신호와 취소 시 제작이 시작되지 않는 것을 확인했다.

참고: [Reactor 공식 구독·취소 가이드](https://projectreactor.io/docs/core/release/reference/coreFeatures/simple-ways-to-create-a-flux-or-mono-and-subscribe-to-it.html).
