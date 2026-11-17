# q6 학습용 WebSocket 수신 서버

현재는 **주문 번호를 받아 커피를 제작하고 완료 응답을 보내는 서버**다. 로그인, 종목 구독, 주기적인 데이터 전송은 아직 추가하지 않았다. 먼저 연결과 송수신을 확인한 뒤 기능을 단계적으로 추가하기 위한 출발점이다.

비동기 학습과 동일한 [OrderDb](../../async/basic/cafe/OrderDb.java)와 [CafeSystem](../../async/basic/cafe/CafeSystem.java)을 재사용한다. 텍스트 `1`, `2`, `3`을 주문 번호로 해석하고 서버가 메뉴를 조회한다. 유효하지 않은 번호에는 `주문 번호를 확인하세요.`라고 응답한다. 실제 외부 API의 계약을 재현한 것이 아닌 학습용 규칙이다.

## 실행

프로젝트 루트의 PowerShell에서 실행한다.

```powershell
.\gradlew.bat bootRun --args="--spring.profiles.active=q6-websocket"
```

IDE에서는 `SpringPlaygroundApplication`의 활성 프로필을 `q6-websocket`으로 설정하여 실행한다. 접속 주소는 `ws://localhost:8080/q6/ws`다. 포트를 변경했다면 주소도 변경한다. 서버 종료는 터미널에서 `Ctrl+C`로 한다.

## Windows에서 한글 로그가 깨질 때

프로젝트는 컴파일과 Gradle 실행 및 Spring Boot 콘솔 로그를 UTF-8로 설정한다. 실행 콘솔도 같은 인코딩으로 읽어야 한다.

- PowerShell 터미널에서는 `chcp 65001`을 실행한 뒤 서버를 다시 실행한다. 파이프로 출력을 읽는 경우 `[Console]::OutputEncoding = [System.Text.UTF8Encoding]::new()`도 설정한다.
- IDE에서 직접 실행한다면 콘솔 인코딩을 UTF-8로 맞추고, 서버와 클라이언트 실행 설정의 VM 옵션에 `-Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8 -Dstderr.encoding=UTF-8`을 추가한다. Gradle 설정은 IDE의 직접 Java 실행에는 적용되지 않는다.
- 이미 깨져서 출력된 로그는 복구되지 않으므로 프로세스를 재시작하고 새 로그를 확인한다.

이는 로그 출력 설정이며 WebSocket 텍스트 메시지의 문자 인코딩을 변경하는 설정은 아니다. 참고: [JDK 21 인코딩 문서](https://docs.oracle.com/en/java/javase/21/intl/supported-encodings.html), [Spring Boot 로깅 문서](https://docs.spring.io/spring-boot/reference/features/logging.html).

## 직접 메시지 보내기

프로젝트의 HTTP 주소 `http://localhost:8080/`를 브라우저에서 열고 개발자 도구 Console에서 아래 명령을 순서대로 실행한다. 페이지가 404여도 같은 서버 주소에서 Console을 사용할 수 있다. 서버는 기본 동일 출처 정책을 사용한다.

```javascript
const socket = new WebSocket('ws://localhost:8080/q6/ws');
socket.onopen = () => console.log('연결 성공');
socket.onmessage = event => console.log('서버 응답:', event.data);
socket.onclose = event => console.log('연결 종료:', event.code);
socket.onerror = event => console.log('통신 오류:', event);
```

`연결 성공`을 확인한 후 다음 명령을 실행한다.

```javascript
socket.send('1'); // 라떼 + 아메리카노
// 첫 주문의 응답을 확인한 뒤 실행한다.
socket.send('2'); // 아메리카노
// 두 번째 응답을 확인한 뒤 실행한다.
socket.send('3'); // 라떼 + 라떼
```

받은 응답과 서버 로그를 확인한 뒤 연결을 닫는다.

```javascript
socket.close(1000, '학습 종료');
```

브라우저 코드는 수동 확인용이다. Java 클라이언트의 비동기 사용법은 다음 학습 단계에서 다룬다.

Java 클라이언트는 [CafeOrderClient](../client/CafeOrderClient.java)를 실행한다. 현재 서버는 수신 콜백에서 제작 지연을 기다린다. 이는 기본 흐름을 단순하게 관찰하기 위한 구성으로, 이후 Reactor를 적용할 때 블로킹 제작 작업의 실행 위치도 함께 비교한다.

## 코드를 읽는 순서

1. [LearningWebSocketConfig.java](./LearningWebSocketConfig.java): `@EnableWebSocket`으로 기능을 활성화하고 `registerWebSocketHandlers()`에서 URL과 핸들러를 연결한다.
2. [LearningWebSocketHandler.java](./LearningWebSocketHandler.java): 연결 완료 → 텍스트 수신 → 응답 전송 → 연결 종료 콜백을 읽는다.
3. [LearningWebSocketServerTest.java](../../../../../../../../../test/java/com/example/spring_playground/question/q6/ws/server/LearningWebSocketServerTest.java): 실제 서버에 접속하여 같은 연결에서 세 주문의 제작 완료 응답을 받고 정상 종료하는지 검증한다.

| 메서드 | 이 예제에서의 역할 |
| --- | --- |
| `afterConnectionEstablished()` | WebSocket 연결 완료 시 호출 |
| `handleTextMessage()` | 텍스트 메시지를 받을 때마다 호출 |
| `message.getPayload()` | 받은 텍스트 읽기 |
| `session.sendMessage()` | 현재 연결의 클라이언트에게 메시지 보내기 |
| `session.close()` | 서버에서 연결 닫기 |
| `handleTransportError()` | 통신 오류 처리 |
| `afterConnectionClosed()` | 연결 종료 확인 |

`WebSocketSession`은 현재 연결을 나타낸다. 서버의 `sendMessage()` 호출 완료 로그만으로 상대 클라이언트가 메시지를 처리했다고 판단하지 않는다. 브라우저 수신 이벤트나 테스트 단언문으로 응답 수신을 확인한다.

이 예제는 STOMP, SockJS, 메시지 브로커 없이 Spring의 기본 WebSocket API를 사용한다. 직접 소켓이나 WebSocket 프레임을 구현하는 수준은 아니다. 수신 콜백 안에서 커피를 제작한 뒤 응답하며 별도 스레드 풀, `@Async`, Reactor 연산자는 추가하지 않았다. 그렇다고 네트워크 통신 전체가 동기식이거나, 모든 연결이 하나의 스레드에서 실행된다는 의미는 아니다.

구현 방식 참고: [Spring 공식 WebSocket API 문서](https://docs.spring.io/spring-framework/reference/6.2/web/websocket/server.html).

## 자동 검증

```powershell
.\gradlew.bat test --tests '*LearningWebSocketServerTest'
```

테스트는 임의 포트에서 서버를 실행하므로 수동 실행 서버와 포트가 겹치지 않는다. 외부 키움 서버와 토큰은 사용하지 않는다.
