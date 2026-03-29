# AAP-prototype

포트원(PortOne) 결제 API 연동 및 결제 사후 검증 흐름 학습용 풀스택 프로토타입.

**Stack:** Spring Boot 3 / React 19 / PostgreSQL / PortOne API

---

## 개발 흐름

### `feat/db-setup`
- 프로젝트 초기 설정 (Spring Initializr, Gradle)
- PostgreSQL 연결 및 결제 테이블(`payments`) 스키마 설계
- `db.js`로 DB 연결 확인

### `feat/payment-verification`
- 결제 사후 검증 로직 구현 (`PaymentService.verifyAndSave`)
  - 프론트엔드 금액 vs PortOne 실제 결제 금액 교차 검증
- 로깅 최적화 (접근 로그, 결제 요청 로그)
- 비동기 결제 생애주기 구현
  - `@Async` + `ThreadPoolTaskExecutor` (core:5, max:10, queue:500)
  - `NotificationService` 인터페이스 + `CombinedNotificationService` (전략 패턴)
  - Slack / Email / SMS / KakaoTalk 4채널 알림
  - 환불 API 연동 (`POST /api/payments/refund`)

### `feat/payments-main-lifecycle`
- PortOne `TC0ONETIME` 공용 테스트 MID 이슈 발견
  - REST API 사후 검증 404 반환 (플랫폼 이슈)
  - DEV_MODE로 임시 우회 처리
- React `App.js` PG 파라미터 `kakaopay.TC0ONETIME` 복원
- README 작성

---

## API

| 메서드 | URL | 설명 |
|--------|-----|------|
| `POST` | `/api/payments/verify` | 결제 검증 및 DB 저장 |
| `GET` | `/api/payments/list` | 결제 내역 조회 |
| `POST` | `/api/payments/refund` | 환불 처리 |

---

## 현재 상태

- **DEV_MODE 활성화 중** — PortOne 사후 검증 우회 (TC0ONETIME 이슈)
- 운영 전환 시 `PaymentService.java` 검증 로직 주석 해제 필요
- 카카오 개발자 계정 테스트 CID 발급 후 정상 검증 가능
