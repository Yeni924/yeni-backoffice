# Yeni Backoffice Portfolio

C#/.NET 실무 경험을 Java/Spring Boot 백오피스로 확장하는 포트폴리오 프로젝트입니다.

## 프로젝트 정보

- **Framework**: Spring Boot 3.x
- **Language**: Java 17
- **Build**: Gradle
- **Template**: Thymeleaf
- **Database**: H2 (개발) → MySQL/Docker (향후)
- **ORM**: Spring Data JPA

## 로컬 개발 환경 설정

### 사전 요구사항

- Java 17 이상
- Gradle (프로젝트에 포함된 Gradle wrapper 사용)

### 실행

```bash
# Linux/macOS
./gradlew bootRun

# Windows PowerShell
.\gradlew.bat bootRun
```

애플리케이션은 `http://localhost:8080`에서 시작됩니다.

## Local H2 Database

현재 기본 실행 profile은 `test`이며, H2 File DB를 사용합니다.

### H2 Console

브라우저에서 아래 주소로 접속합니다.

```
http://localhost:8080/h2-console
```

**접속 정보:**

```
Driver Class: org.h2.Driver
JDBC URL: jdbc:h2:file:./data/yeni-backoffice;MODE=MySQL;DATABASE_TO_LOWER=TRUE;CASE_INSENSITIVE_IDENTIFIERS=TRUE
User Name: sa
Password: (비워두기)
```

**주의:**

- `jdbc:h2:~/test`는 기본 샘플 DB이므로 사용하지 않습니다.
- H2 실제 DB 파일은 `data/` 폴더에 생성되며, Git에 올리지 않습니다.

## Database Sharing Strategy

회사 PC와 집 PC에서 모두 개발할 수 있도록 실제 DB 파일은 Git에 올리지 않습니다.

### Git에 올리는 것

- Entity 코드
- Repository, Service, Controller 코드
- `application-*.properties` 설정 파일
- DB migration 또는 sample data script
- README 실행 방법

### Git에 올리지 않는 것

- H2 실제 DB 파일
- `data/` 폴더
- `*.mv.db`, `*.trace.db`
- `.env`
- 운영 DB 비밀번호

**DB 재생성 방식:**

각 환경에서는 Entity 코드와 migration/sample script를 통해 DB 구조와 샘플 데이터를 자동 재생성합니다.

## 개발 프로필 설정

### test (개발 기본값)

H2 File DB를 사용하는 로컬 개발 환경입니다.

```bash
./gradlew bootRun
```

### local (Docker MySQL)

나중에 Docker MySQL/MariaDB 연결용 설정입니다.

```bash
./gradlew bootRun --args='--spring.profiles.active=local'
```

### prod (클라우드 배포)

환경변수를 사용하는 프로덕션 환경입니다.

```bash
export DB_URL="jdbc:mysql://..."
export DB_USERNAME="user"
export DB_PASSWORD="password"
export PORT="8080"
./gradlew bootRun --args='--spring.profiles.active=prod'
```

## 주요 기능 (계획 중)

- **상품관리**: 상품 등록, 수정, 판매상태 변경, 재고 관리
- **주문관리**: 주문 목록, 상세, 상태 변경, 이력 관리
- **결제관리**: 결제 승인 Mock, 취소, 이력 관리
- **배송관리**: 송장 등록, 배송 상태 변경, 이력 조회
- **프로모션관리**: 기간별 프로모션, 할인 정책 관리
- **알림톡 Queue**: 상태 변경 알림 큐, 재처리 구조

## 프로젝트 구조

```
src/
├── main/
│   ├── java/
│   │   └── com/yeni/yeni_backoffice/
│   │       ├── YeniBackofficeApplication.java
│   │       └── dashboard/
│   │           └── DashboardController.java
│   └── resources/
│       ├── templates/
│       │   ├── fragments/
│       │   │   ├── sidebar.html
│       │   │   └── topbar.html
│       │   └── dashboard/
│       │       └── index.html
│       ├── static/
│       │   ├── css/
│       │   │   ├── common.css
│       │   │   ├── layout.css
│       │   │   ├── components.css
│       │   │   └── pages/
│       │   │       └── dashboard.css
│       │   └── js/
│       │       ├── common.js
│       │       └── pages/
│       │           └── dashboard.js
│       ├── application.properties
│       ├── application-test.properties
│       ├── application-local.properties
│       └── application-prod.properties
└── test/
```

## 기술 스택

### Backend

- Java 17
- Spring Boot 3.5.x
- Spring Data JPA
- Hibernate

### Frontend

- Thymeleaf
- Vanilla JavaScript
- CSS3 Grid/Flexbox

### Database

- H2 (개발)
- MySQL 8.0+ (로컬/클라우드)

## 라이선스

Portfolio Project

## 연락처

권예은 (Ye Eun Kwon)
- Role: Backoffice / API Developer
- Email: [contact info]

## Portfolio Mock: Payment Gateway Bridge Sales & Settlement Operation Module

이 모듈은 실제 KG이니시스 운영망을 호출하지 않는 포트폴리오용 Mock 구현입니다.  
목표는 단순 결제 샘플이 아니라, 여러 PG사를 공통 인터페이스로 중계하는 **PGB(Payment Gateway Bridge)** 서버 컨셉과 운영형 백오피스에서 자주 다루는 **결제 승인, 부분/전체 취소, UNKNOWN 재조회, 매출 생성, 외부 시스템 전송, 수수료 정책, 일별 정산 배치, 로그 추적** 흐름을 Spring Boot 구조 안에서 보여주는 것입니다.

### 구현 범위

- 공통 결제 도메인 + PG사별 Gateway/Adapter 구조
- `PaymentGateway`, `PaymentGatewayRegistry`, `PaymentGatewayRouter` 기반 PG 선택
- `MOCK`, `INICIS` Provider Mock 구현
- 공통 Payment Bridge API 기반 승인/취소/재조회 처리
- 승인 후 내부 처리 실패 시 netCancel 보상 흐름 진입점 표현
- 전체/부분 취소 API 및 취소 금액 검증
- UNKNOWN 거래 재조회 API
- 결제 결과 기반 매출 데이터 생성
- 외부 영업관리 시스템 전송 요청/이력 관리 Mock
- PG 수수료 정책 등록 및 유효기간 중복 검증
- 일별 정산 배치 생성, 정산 확정, 지급 상태 변경
- 결제/PG API/Audit/정산 로그 저장
- Swagger UI 기반 API 문서 확인
- 포트폴리오용 PG 운영 콘솔 화면 제공

### 구조 포인트

결제 승인/취소 서비스는 특정 PG 클라이언트에 직접 의존하지 않고 `PaymentGatewayRegistry`와 `PaymentGatewayRouter`를 통해 선택된 `PaymentGateway`를 호출합니다.  
현재는 `MockPaymentGateway`, `InicisPaymentGateway`가 Mock 응답으로 동작하지만, Toss, Stripe, Paymentwall 같은 신규 PG가 추가되더라도 공통 결제, 매출, 외부 전송, 정산 흐름은 유지하고 Gateway 구현만 확장할 수 있도록 구성했습니다.

결제 상태, 인증 상태, 취소 상태, 매출 상태, 외부 전송 상태, 정산 상태, 배치 상태는 enum으로 관리합니다.

### 주요 API

```text
POST /api/payment-bridge/payments/approve
POST /api/payment-bridge/payments/{paymentId}/cancel
POST /api/payment-bridge/payments/{paymentId}/retry-query
GET  /api/payment-bridge/payments
GET  /api/payment-bridge/payments/{paymentId}
GET  /api/payment-bridge/payments/{paymentId}/pg-logs
POST /api/payment-bridge/callback/{pgProvider}

GET  /api/admin/sales
POST /api/admin/sales/{salesId}/adjustments
GET  /api/admin/external-send-requests
POST /api/admin/external-send-requests/{requestId}/send
POST /api/admin/external-send-requests/retry

GET  /api/admin/pg-fee-policies
POST /api/admin/pg-fee-policies

POST /api/admin/settlements/batch/run
GET  /api/admin/settlements
GET  /api/admin/settlements/{statementId}
POST /api/admin/settlements/{statementId}/confirm
POST /api/admin/settlements/{statementId}/pay
```

### 화면 및 문서

```text
PG 운영 콘솔: http://localhost:8080/admin/payment-operations
Swagger UI:   http://localhost:8080/swagger-ui/index.html
```

### 보안 및 운영 정보

실제 MID, signKey, 운영 URL, 서버 주소, API 전문, DB 접속 정보는 포함하지 않습니다.  
로컬 실행에서는 아래 환경변수로 포트폴리오용 값을 주입할 수 있습니다.

```text
INICIS_MID
INICIS_SIGN_KEY
INICIS_RETURN_URL
INICIS_CLOSE_URL
```

기본값은 모두 로컬 Mock 실행용 샘플 값입니다.
