# RateLimiter와 동기·비동기 병행 처리의 동작 및 자원 관리

## 1. 문제 상황 및 학습 이유

Trady 프로젝트에서는 서비스 배포 이전에 키움 REST API를 통해 대량의 과거 차트 데이터를 수집해야 한다.

외부 API의 호출 제한을 준수하기 위해 Guava RateLimiter를 적용하면서, 초당 허용 호출량을 설정하면 API 요청 속도를 제어할 수 있을 것으로 예상하였다. 이를 검증한 기존 실험에서는 RateLimiter를 통해 설정된 초당 평균 호출 속도를 제어할 수 있음을 확인하였다.

그러나 **동기식 순차 처리에서는 외부 API의 응답시간이 길어질 경우 다음 요청이 지연되어 실제 호출 속도가 RateLimiter의 설정값보다 낮아지는 한계**가 존재한다. 반대로 응답시간이 짧으면 RateLimiter가 호출 속도를 제한하지만, Guava의 기본 `SmoothBursty` 방식에서는 축적된 permit에 의해 순간적인 요청 집중이 발생할 수 있다.

이러한 동기식 처리의 한계를 개선하기 위해 비동기 병행 처리 도입을 고려하였다. 병행 처리는 외부 API의 응답 대기시간을 중첩하여 처리량을 높일 수 있지만, 동시에 진행되는 작업이 증가하면서 스레드 풀, 작업 대기열, 메모리 및 네트워크 연결 등의 자원 관리가 필요하다.

따라서 **동기식 순차 처리에서 외부 API 응답시간에 따른 실제 호출 속도의 변화를 확인하고, 비동기 병행 처리에서 발생하는 자원 사용량과 병목 현상을 실험하여 두 방식의 성능 및 자원 관리 측면에서의 트레이드오프를 이해한다.**

## 2. 학습 질문

1. 동기식 순차 처리에서 API 응답시간에 따라 RateLimiter의 설정 속도와 실제 요청 시작 속도는 어떻게 달라지는가?
2. API 응답시간이 RateLimiter의 기본 호출 간격보다 길 때,
비동기 요청 처리는 동기식 순차 처리와 비교해 실제 호출 속도와 전체 처리시간이 얼마나 달라지는가?
3. 동기식 순차 처리와 비동기 HTTP 처리에서 동시에 진행 중인 요청 수와
JVM 힙 메모리 사용량은 어떻게 달라지는가?

## 3. 학습 결과

### 3.1. 학습 질문 1

실험: [GuavaRateLimiterBasicTest.test1(), test2(), test3()](./GuavaRateLimiterBasicTest.java) → `runSyncTest()` → `runHttpTest()`

[실행 결과](./results/GuavaRateLimiterBasicTest.txt)에서 초당 2회 설정 시 서버 지연이 0ms·200ms인 요청은 약 500ms 간격으로 시작했다. 200ms 조건에서는 다음 호출까지 부족한 시간을 `acquire()`가 약 270~290ms 대기시켰다.

응답 지연이 1,200ms이면 `acquire()`의 추가 대기는 없었지만 요청 시작 간격은 약 1.2초로 늘었다. RateLimiter는 호출 속도를 제한할 뿐, 긴 응답시간으로 낮아진 처리량을 설정값까지 높여 주지는 않는다.

### 3.2. 학습 질문 2

실험: [GuavaRateLimiterBasicTest.test3(), test4()](./GuavaRateLimiterBasicTest.java) → `runSyncTest()`, `runAsyncTest()` → `runHttpTest()`

[실행 결과](./results/GuavaRateLimiterBasicTest.txt)에서 같은 1,200ms 서버 지연의 요청 10개를 처리하는 데 동기식은 12.165초, 비동기식은 5.715초가 걸렸다.

비동기식은 이전 응답이 오기 전에 다음 요청을 제출해 약 500ms 시작 간격을 유지했다.
각 요청의 응답시간이 짧아진 것이 아니라, 여러 요청의 응답 대기가 겹쳐 전체 처리시간이 줄었다.

### 3.3. 학습 질문 3

실험: [AsyncMemoryTest.main(), run(false), run(true)](./AsyncMemoryTest.java)

[실행 결과](./results/AsyncMemoryTest.txt)에서 진행 중 요청의 최대값은 동기식 1개, 비동기식 3개였다. 이벤트 시점에 관찰한 최대 힙 사용량은 각각 24.00MiB, 45.77MiB였다.

이번 실행에서는 비동기식의 동시 요청 수와 최대 힙 사용량이 더 컸다. 다만 힙은 Mock 서버를 포함한 JVM 전체 값이고, 두 조건의 시작 힙과 GC 영향도 다르므로 차이 전체를 비동기 요청 자체의 메모리 비용으로 단정할 수 없다. 완료 처리의 병목이나 메모리 부족까지 확인한 결과는 아니다.

### 3.4. Trady 적용 판단

Trady의 과거 차트 수집은 배포 전 한 번 수행하는 작업이므로, 구현의 단순함을 우선해 단일 스레드 기반 동기식 순차 처리를 사용한다.

처음에는 Guava RateLimiter로 호출 속도를 제어했다. 하지만 Guava RateLimiter의 기본 구현인 `SmoothBursty`는 토큰 버킷 방식으로 유휴 시간 동안 permit을 축적하며, 이후 여러 요청이 짧은 시간에 집중되는 burst를 허용한다. 이는 유휴 시간 이후에도 요청을 몰아 보내지 않고 초당 최대 5회 수준으로 일정하게 호출하려는 Trady의 정책과 맞지 않는다.

따라서 Leaky Bucket 방식으로 요청을 일정한 간격에 분산하는 Failsafe의 Smooth RateLimiter로 변경했다.

추가로 일시적인 API 장애에 대응하기 위해 Failsafe의 재시도 기능을 테스트했다. 의도적으로 HTTP 503을 반환하도록 구성한 결과, 일시적인 실패는 재시도 후 성공했으며, 실패가 지속되면 설정한 재시도 횟수를 소진한 뒤 해당 요청을 최종 실패로 처리했다.

### 3.5. 동기식 처리에서 Guava와 Failsafe 비교

실험: [SyncRateLimiterComparisonTest.main(), run()](./SyncRateLimiterComparisonTest.java). 초당 5회, 조건별 10개 요청, 서버 지연 100ms/300ms를 생성 직후와 2초 유휴 후로 나눠 비교했다. `main()`으로 실행하며 [실행 결과](./results/SyncRateLimiterComparisonTest.txt)는 1회 관찰값이다. 연결 초기화와 유휴 시간은 측정에서 제외했다.

100ms 응답의 생성 직후 평균 시작 간격은 Guava 199.357ms, Failsafe 200.111ms로 비슷했다. 유휴 후에는 Guava 112.886ms, Failsafe 199.860ms였다. Guava는 저장된 permit 때문에 요청 10개가 끝날 때까지 기본 간격보다 빠르게 호출될 수 있었다. 이는 지속 가능한 장기 호출 속도를 뜻하지 않는다.

300ms 응답에서는 두 구현 모두 평균 약 307~316ms 간격이었다. 동기식 순차 처리의 응답 대기가 기본 permit 간격 200ms보다 길어, 이 조건에서는 알고리즘에 따른 처리량 차이가 작았다. 시간은 환경과 스케줄링 영향을 받으며 START는 실제 서버 수신 시각이 아니라 HTTP 호출 직전 시각이다.
