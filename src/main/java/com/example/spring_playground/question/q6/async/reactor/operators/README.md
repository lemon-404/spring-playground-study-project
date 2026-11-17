# 기본 연산자로 처리 연결하기

서버 없이 각 클래스의 main()을 IDE에서 실행한다. 별도 실행기나 스케줄러는 사용하지 않으며, 공통 CafeSystem으로 커피를 제작한다.

| 실행 클래스 | 학습할 동작 | 관찰할 흐름 |
| --- | --- | --- |
| [MapMain](./MapMain.java) | map()으로 값 변환 | 주문 1번 제작 결과 → 픽업 안내문 → onNext |
| [ThenMain](./ThenMain.java) | then()으로 정상 완료 후 다음 Mono 연결 | 주문 1번 제작 → 알림 → onComplete |
| [ConcatMapMain](./ConcatMapMain.java) | concatMap()으로 제작 Mono를 순차 연결 | 1번 제작 완료 → 2번 제작 완료 → 3번 제작 완료 |
| [WhenMain](./WhenMain.java) | Mono.when()으로 전체 완료 묶기 | 1·2번 제작이 모두 끝난 후 전체 onComplete |

map()의 함수는 변환한 값을 반환한다. concatMap()의 함수는 Mono 같은 다음 흐름을 반환하며, concatMap이 그 흐름을 구독하고 결과를 전달한다. 이 차이를 먼저 확인한다.

ThenMain과 WhenMain의 최종 타입은 Mono<Void>다. 정상 완료 신호만 전달하므로 onNext 콜백은 실행되지 않는다. Mono.when()은 개별 제작 결과들을 목록으로 모아 반환하는 연산자가 아니다.

모든 예제는 main에서 실행된다. Mono.when() 자체가 병렬 실행을 만드는 것은 아니다. 현재의 블로킹 제작 작업에서는 제작 시간이 겹치지 않으며, when의 목적은 여러 흐름의 전체 완료를 표현하는 것이다.

참고: [Reactor 공식 연산자 선택 가이드](https://projectreactor.io/docs/core/release/reference/apdx-operatorChoice.html).

실행 확인: JDK 21과 로컬 캐시의 Reactor Core 3.8.7로 네 클래스를 직접 컴파일·실행했다. map은 변환된 안내문을 전달했고, then은 제작 후 알림을 실행했다. concatMap은 1·2·3번 제작 결과를 순서대로 전달했다. when은 두 제작 완료 후 전체 완료를 알렸다. then과 when의 onNext는 실행되지 않았으며 모두 main 스레드에서 정상 종료했다.
