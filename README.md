# AAP-prototype (결제 및 환불 관리 시스템)

## 프로젝트 소개

**AAP-prototype**은 포트원(PortOne) 결제 API와 통합된 결제 처리 및 환불 관리 학습용 프로토타입입니다.
Spring Boot 백엔드와 React 프론트엔드로 구성되며, 결제 사후 검증 및 비동기 멀티 채널 알림(Slack, 이메일, SMS, KakaoTalk)을 구현합니다.

**핵심 특징:**
- 결제 사후 검증 (금액 비교 및 PortOne API 검증)
- 비동기 멀티 채널 알림 시스템 (전략 패턴 적용)
- PostgreSQL 기반 결제 내역 관리
- RESTful API 설계

---

## 기술 스택

### 백엔드 (Spring Boot)
| 기술 | 버전 | 설명 |
|------|------|------|
| Spring Boot | 3.5.10-SNAPSHOT | 메인 프레임워크 |
| Spring Data JPA | - | ORM / 데이터베이스 접근 |
| Spring Web | - | RESTful API 구축 |
| Spring Validation | - | 입력 데이터 검증 |
| Lombok | - | 보일러플레이트 코드 제거 |
| PostgreSQL Driver | - | 데이터베이스 드라이버 |
| dotenv-java | 3.0.0 | 환경변수 관리 (.env 로드) |

**Java 버전:** JDK 17

### 프론트엔드 (React)
| 기술 | 버전 | 설명 |
|------|------|------|
| React | 19.2.3 | UI 프레임워크 |
| Axios | 1.13.2 | HTTP 클라이언트 |
| React Scripts | 5.0.1 | 빌드 도구 |

### 데이터베이스
| 기술 | 설정 |
|------|------|
| PostgreSQL | localhost:5432 |
| Hibernate DDL | update (자동 테이블 생성) |

---

## 주요 기능

### 1. 결제 처리
- 포트원 결제창 연동 (IMP.request_pay)
- 결제 사후 검증: 프론트엔드 금액 vs 포트원 실제 결제 금액 비교
- 검증 완료 후 PostgreSQL에 결제 정보 저장 (orderId, amount, status 등)
- 결제 상태 관리: READY → PAID

### 2. 환불 처리
- 결제 건에 대한 환불 신청
- 포트원 환불 API 연동
- DB 결제 상태 PAID → CANCELLED 업데이트

### 3. 비동기 멀티 채널 알림
- **Slack** / **이메일** / **SMS** / **KakaoTalk** 알림 (각각 독립 구현)
- `CombinedNotificationService`로 통합 관리 (전략 패턴)
- `@Async` 비동기 처리 — 알림이 결제 응답 시간에 영향을 주지 않음
- ThreadPool: Core 5 / Max 10 / Queue 500 (`AsyncConfig`)

### 4. 결제 내역 조회
- 전체 결제 내역 최신순 조회
- 영수증 URL 저장 및 제공

---

## 프로젝트 구조

```
AAP-prototype/
├── src/main/java/com/project/AAP_prototype/
│   ├── AapPrototypeApplication.java
│   ├── config/
│   │   └── AsyncConfig.java                   # 비동기 ThreadPool 설정
│   ├── controller/
│   │   └── PaymentController.java             # REST API 엔드포인트
│   ├── entity/
│   │   └── Payment.java                       # JPA Entity
│   ├── repository/
│   │   └── PaymentRepository.java             # Spring Data JPA
│   └── service/
│       ├── PaymentService.java                # 결제 핵심 비즈니스 로직
│       └── notification/
│           ├── NotificationService.java       # 알림 인터페이스
│           ├── CombinedNotificationService.java
│           ├── SlackNotificationService.java
│           ├── EmailNotificationService.java
│           ├── SmsNotificationService.java
│           └── KaKaoNotificationService.java
│
├── src/main/resources/
│   └── application.yaml                       # Spring Boot 설정
│
├── frontend/                                  # React 프로젝트
│   ├── src/
│   │   └── App.js                             # 결제/환불/목록 UI
│   └── package.json
│
├── .env                                       # 환경변수 (DB, API 키, Webhook)
├── build.gradle
└── settings.gradle
```

---

## 실행 방법

### 사전 요구사항
- JDK 17 이상
- Node.js 14 이상
- PostgreSQL 12 이상
- 포트원(PortOne) 계정 및 가맹점 식별코드

### 1. 환경변수 설정 (`.env`)

```bash
# PostgreSQL
DB_HOST=localhost
DB_PORT=5432
DB_USER=postgres
DB_PASSWORD=your_password
DB_NAME=postgres

# 포트원
PORTONE_API_KEY=your_portone_api_key
PORTONE_API_SECRET=your_portone_api_secret

# Slack (선택)
SLACK_WEBHOOK_URL=https://hooks.slack.com/services/YOUR/WEBHOOK/URL
```

### 2. 백엔드 실행

```bash
# Windows
gradlew.bat bootRun

# Mac/Linux
./gradlew bootRun
```

실행 포트: `http://localhost:8080`

### 3. 프론트엔드 실행

```bash
cd frontend
npm install
npm start
```

실행 포트: `http://localhost:3000`

---

## API 엔드포인트

| 메서드 | URL | 설명 |
|--------|-----|------|
| `POST` | `/api/payments/verify` | 결제 검증 및 DB 저장 |
| `GET` | `/api/payments/list` | 결제 내역 전체 조회 |
| `POST` | `/api/payments/refund` | 환불 요청 처리 |

### POST /api/payments/verify
```json
// Request
{
  "imp_uid": "imp_12345678",
  "merchant_uid": "order_1234567890",
  "amount": 1000,
  "buyerName": "홍길동",
  "paymentMethod": "card"
}

// Response 200
{
  "id": 1,
  "orderId": "order_1234567890",
  "amount": 1000,
  "status": "PAID",
  "createdAt": "2026-03-29T15:30:00"
}
```

### POST /api/payments/refund
```json
// Request
{
  "merchantUid": "order_1234567890",
  "reason": "사용자 단순 변심"
}

// Response 200
{
  "message": "환불 처리가 완료되었습니다."
}
```

---

## 결제 플로우

```
[React] IMP.request_pay() 호출
    ↓
[포트원] 결제창 표시 → 사용자 결제 완료 → imp_uid 반환
    ↓
[React] POST /api/payments/verify { imp_uid, merchant_uid, amount }
    ↓
[PaymentService]
  ① 포트원 API 토큰 발급
  ② imp_uid로 실제 결제 정보 조회
  ③ 금액 검증 (프론트 amount vs 포트원 amount)
  ④ DB 저장 (status: PAID)
  ⑤ 비동기 알림 발송 (Slack/Email/SMS/Kakao)
    ↓
[React] GET /api/payments/list → 결제 목록 업데이트
```

---

## 주의사항

### DEV_MODE
현재 포트원 사후 검증이 우회된 상태입니다 (`DEV_MODE`).
운영 환경 전환 시 `PaymentService`의 실제 검증 로직을 활성화해야 합니다.

```java
// PaymentService.java
log.warn("[DEV_MODE] PortOne 사후 검증 우회 중 - 운영 환경에서는 반드시 제거할 것");
```

### .env 보안
`.env` 파일은 `.gitignore`에 포함되어 있습니다. API 키를 소스코드에 하드코딩하지 마세요.

---

## 향후 개선 사항

- [ ] DEV_MODE 제거 및 실제 포트원 API 검증 활성화
- [ ] JWT 기반 API 인증
- [ ] 결제 재시도 로직 (지수 백오프)
- [ ] 결제/환불 통계 대시보드
- [ ] 인덱스 추가 (orderId, createdAt)
- [ ] 테스트 코드 작성 (단위/통합)

---

**프로젝트 버전:** 0.0.1-SNAPSHOT
**최종 수정일:** 2026-03-29
