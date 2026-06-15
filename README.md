# Yeni Backoffice Portfolio

.NET 기반 백오피스 실무 경험을 Spring Boot로 재구성한 결제·매출 운영 포트폴리오입니다.

이 프로젝트는 실제 PG 운영망에 직접 연결하지 않고 Mock PG Adapter를 사용합니다. 단순 결제 CRUD가 아니라 승인, 취소, 결과불명, 망취소 필요 상태, SALE/CANCEL 매출 원장, 외부전송, 알림톡 Queue, RecoveryTask, 정산 상태 전이까지 이어지는 운영 흐름을 구현했습니다.

## 목차

1. [프로젝트 개요](#1-프로젝트-개요)
2. [핵심 구현 요약](#2-핵심-구현-요약)
3. [프로젝트를 만든 이유](#3-프로젝트를-만든-이유)
4. [기술 스택](#4-기술-스택)
5. [전체 아키텍처](#5-전체-아키텍처)
6. [핵심 도메인 흐름](#6-핵심-도메인-흐름)
7. [주요 화면](#7-주요-화면)
8. [테스트와 검증](#8-테스트와-검증)
9. [로컬 실행 방법](#9-로컬-실행-방법)
10. [Demo 안내](#10-demo-안내)
11. [운영 환경 확장 계획](#11-운영-환경-확장-계획)
12. [포트폴리오 검토 포인트](#12-포트폴리오-검토-포인트)
13. [제출용 PDF](#13-제출용-pdf)

## 1. 프로젝트 개요

기존 백오피스 실무에서 다뤘던 관리자 화면, API, PG 연동, 외부 시스템 연동, 알림톡 Agent, POS/KIOSK 운영 데이터 흐름을 Java/Spring Boot 구조로 다시 설계해 본 프로젝트입니다.

결제 승인·취소 이후 운영자가 확인하고 처리해야 하는 데이터를 하나의 흐름으로 연결했습니다.

```text
Mock PG 승인/취소
  -> PaymentTransaction / PaymentCancel
  -> SALE / CANCEL 매출 원장
  -> 외부전송 / 알림톡 Queue
  -> 결과불명 및 실패 RecoveryTask
  -> 정산 초안 생성 / 확정 / 지급 처리
```

### 구현 범위

- **구현 완료**: Mock PG 승인·취소, 중복 요청 방어, 부분취소, 결과불명, RecoveryTask, SALE/CANCEL 원장, 외부전송·알림톡 대기열, 정산 초안·확정·지급
- **포트폴리오 시연 범위**: 운영 화면, 시나리오 실행, 데이터 연결 조회, DB 명세, Swagger
- **실제 운영 확장 필요**: 실제 PG·알림톡 외부망, 역할 기반 권한·CSRF·CORS·IP allowlist·감사 로그 정책, 영업일 정산, 정산 후 취소 자동 차감, 대량 데이터 검증

## 2. 핵심 구현 요약

- Idempotency Key와 DB unique constraint 기반 중복 승인·취소 방어
- `PaymentTransaction` 비관적 락을 통한 동시 부분취소 초과 방어
- 승인·취소 결과불명 상태와 `RecoveryTask` 분리
- 확정된 거래만 SALE/CANCEL 매출 원장으로 기록
- CANCEL 거래를 음수 원장으로 남기고 원 SALE은 수정하지 않는 구조
- 외부전송과 알림톡 Queue를 결제 트랜잭션 이후 후속 처리로 분리
- 정산 `DRAFT -> CONFIRMED -> PAID` 상태 전이 및 확정 후 재계산 차단
- 같은 날짜의 DRAFT 정산 재실행 시 신규 미정산 원장을 누적 재계산
- `ErrorResponse`, `requestId`, `fieldErrors` 기반 API 오류 응답 표준화
- Gradle 멀티모듈 `api` / `core` 구조와 자동화 테스트

## 3. 프로젝트를 만든 이유

기존 실무에서는 C#/.NET 기반으로 관리자 화면, API, PG 연동, 외부 시스템 연동, 알림톡 Agent, POS/KIOSK 운영 데이터 흐름을 다뤘습니다.

이 프로젝트는 그 경험을 Java/Spring Boot 환경에서 다시 설계해 본 포트폴리오입니다. Java 문법 학습 결과만 보여주기보다, 실무에서 중요했던 중복 요청 방어, 상태값 관리, 결과불명 처리, 후속 처리 분리, 매출 원장과 정산 기준을 Spring MVC, JPA, Service 계층 구조로 재구성하는 데 초점을 맞췄습니다.

실제 운영 경험이 없는 정산·수수료·PG 운영망 연동까지 완성했다고 표현하지 않습니다. 해당 영역은 운영 흐름을 이해하고 Mock 기반으로 재현한 범위이며, 실제 운영 적용을 위해 필요한 보강 항목을 별도로 정리했습니다.

## 4. 기술 스택

| 구분 | 기술 |
|---|---|
| Backend | Java 17, Spring Boot 3.5, Spring MVC |
| Persistence | Spring Data JPA, H2, MySQL Driver |
| Frontend | Thymeleaf, Vanilla JavaScript, Tabulator |
| API 문서 | springdoc-openapi, Swagger UI |
| Build | Gradle 멀티모듈 (`api`, `core`) |
| Test | JUnit 5, Spring Boot Test, MockMvc, AssertJ |
| Demo | Fly.io, H2 in-memory |

## 5. 전체 아키텍처

```text
Browser / Operator
       |
       v
api module
  - View Controller / REST Controller
  - Thymeleaf / JavaScript
  - ErrorResponse / requestId
       |
       v
core module
  - PaymentApproveService / PaymentCancelService
  - PaymentQueryService / PaymentRecoveryService
  - SalesLedgerService / SettlementOperationService
  - Entity / Repository / Mock PG Adapter
       |
       v
H2 Demo DB / MySQL profile
```

주요 서비스 책임:

| 서비스 | 책임 |
|---|---|
| `PaymentApproveService` | 승인 요청 검증, 중복 방어, PG 승인, 결제·원장·후속 처리 생성 |
| `PaymentCancelService` | 취소 가능 금액 검증, 비관적 락, 부분·전체취소 처리 |
| `PaymentQueryService` | 결제 조회 및 승인 결과불명 재조회 |
| `PaymentRecoveryOperationService` | RecoveryTask 운영 조회·재처리 기반과 상태 전이 |
| `SalesLedgerService` | SALE/CANCEL 원장 조회, 요약, 연결 데이터 조회 |
| `SettlementOperationService` | 정산 실행 동시성 방어, 명세 조회·확정·지급 |
| `SettlementBatchProcessor` | 미정산 원장 집계와 수수료·VAT 계산 |

## 6. 핵심 도메인 흐름

### 결제 승인

```text
승인 요청
  -> Idempotency Key / orderNo 중복 확인
  -> Mock PG 승인 호출
  -> 성공: PaymentTransaction + SALE 원장 + 외부전송 + 알림톡 Queue
  -> 결과불명: APPROVE_UNKNOWN + RecoveryTask
  -> PG 승인 후 내부 처리 실패: 복구 대상 기록
```

### 결제 취소

```text
취소 요청
  -> PaymentTransaction 비관적 락
  -> 중복 요청 / 취소 가능 금액 / 현재 상태 검증
  -> 성공: PaymentCancel + CANCEL 음수 원장 + 후속 처리
  -> 결과불명: CANCEL_UNKNOWN + RecoveryTask
```

#### 부분취소 동시성 방어

부분취소는 `PaymentTransaction` PK로 조회하면서 `PESSIMISTIC_WRITE` lock을 적용합니다. 이 lock은
`payment_transaction` 테이블 전체가 아니라 해당 payment row 하나에 적용되며, 같은 결제에 동시
부분취소가 들어오면 앞선 트랜잭션 커밋 후 갱신된 취소 가능 금액을 다시 검증합니다.

통합 테스트에서는 승인금액 10,000원 결제에 8,000원 부분취소 두 건을 동시에 요청해 한 건만
성공하고, 최종 취소금액과 CANCEL 원장이 승인금액을 초과하지 않는 것을 확인했습니다. 서로 다른
payment row의 동시 부분취소는 각각 정상 반영되는 것도 함께 검증했습니다.

> 현재 테스트는 H2 환경에서 최종 데이터 정합성을 검증합니다. 실제 운영 DB에서는 외부 PG 호출이
> 길어지는 동안 DB lock을 오래 유지하지 않도록 취소 요청 선점, PG 호출, 결과 확정 트랜잭션을
> 분리하는 구조로 확장할 수 있습니다.

### 매출 원장

- 승인 성공 거래는 `SALE` 양수 원장으로 기록합니다.
- 취소 성공 거래는 `CANCEL` 음수 원장으로 기록합니다.
- CANCEL은 원 SALE ID를 참조하며 원매출을 수정하지 않습니다.
- 결과불명 상태에서는 확정 전까지 매출 원장을 생성하지 않습니다.

### RecoveryTask

결과불명, 망취소 필요 상태, 후속 처리 실패를 결제 상태와 분리해 추적합니다. 운영 화면에서 목록·상세를 확인하고 지원되는 유형은 재처리할 수 있습니다.

재처리는 `READY` 또는 `FAILED` 상태인 작업만 조건부 업데이트로 `PROCESSING` claim한 뒤 실행합니다. 동일 작업에 동시 재처리 요청이 들어와도 하나의 요청만 claim에 성공하며, 이미 `PROCESSING` 또는 `SUCCESS`인 작업은 재처리하지 않습니다. 재처리 실패와 미지원 유형은 실패 사유를 남겨 `FAILED`로 전이해 `PROCESSING` 상태에 고착되지 않도록 구성했습니다.

### 외부전송 / 알림톡 Queue

결제 처리와 외부 시스템 전송을 분리해, 결제가 성공한 뒤 후속 처리 상태를 별도로 확인할 수 있도록 구성했습니다. RecoveryTask는 운영 조회와 일부 유형의 재처리 기반을 제공하며, 모든 유형을 자동 복구하는 Worker는 확장 범위입니다. 외부전송과 알림톡은 Queue 생성, 상태 관리, 수동 실행 Mock Worker까지 구현했습니다.

외부전송과 알림톡 Queue는 Mock Worker가 `READY/FAILED` 상태의 대상을 `SENDING`으로 조건부 claim한 뒤 처리합니다. 성공 시 `SUCCESS`, 실패 시 `FAILED`와 실패 사유 및 재시도 횟수를 기록하며, 동일 Queue에 Worker가 동시에 접근해도 하나의 Worker만 claim할 수 있습니다. 실제 외부전송 시스템과 알림톡 API는 호출하지 않습니다.

### 정산

```text
미정산 SALE/CANCEL 원장
  -> DRAFT 정산 초안 생성 또는 누적 재계산
  -> CONFIRMED 정산 확정
  -> PAID 지급 처리
```

- `payment_transaction`이 아니라 SALE/CANCEL 매출 원장을 기준으로 집계합니다.
- 동일 정산일·MID 동시 실행은 인메모리 락과 DB unique constraint로 방어합니다.
- 같은 날짜의 DRAFT 명세가 있으면 신규 미정산 원장을 같은 명세에 누적합니다.
- CONFIRMED 또는 PAID 명세는 재계산하지 않습니다.
- 확정 후 취소의 다음정산차감 자동 반영은 운영 환경 확장 계획에 포함합니다.

## 7. 주요 화면

| 화면 | 설명 | URL |
|---|---|---|
| 메인 대시보드 | 프로젝트 개요, 구현 범위, 개발 흐름 | `/dashboard` |
| PG 운영 콘솔 | Mock PG 승인·취소·결과불명·복구 시나리오 실행 | `/admin/payment-operations` |
| 매출 원장 | SALE/CANCEL 조회와 원거래·후속 처리 연결 확인 | `/admin/payment-operations/sales-ledger` |
| 정산 관리 | 정산 초안 생성·누적 재계산·확정·지급 처리 | `/admin/payment-operations/settlements` |
| DB 명세 | 전체 테이블과 컬럼 역할 확인 | `/admin/database-spec` |
| Swagger UI | REST API 명세 확인 | `/swagger-ui/index.html` |

> 화면 캡처는 제출용 PDF에 포함했으며, README에도 추후 선별해 추가할 예정입니다.

## 8. 테스트와 검증

결제 승인·취소, 결과불명, 매출 원장, 정산 중복 방어 흐름을 단위 테스트와 통합 테스트로 검증합니다.

### 테스트로 검증한 항목

주요 검증 시나리오:

- 승인 성공 후 `PaymentTransaction`, SALE 원장, 외부전송, 알림톡 Queue 생성
- 동일 Idempotency Key 중복 승인·취소 시 기존 결과 반환
- 부분취소 성공 후 `PaymentCancel`과 CANCEL 음수 원장 생성
- 취소 가능 금액 초과 요청 차단
- 같은 payment row 동시 부분취소 시 승인금액 초과 방어
- 서로 다른 payment row 동시 부분취소의 독립 처리
- 승인 결과불명 시 SALE 미생성 및 RecoveryTask 생성
- 취소 결과불명 시 CANCEL 원장 미생성
- 같은 날짜·MID 정산 중복 생성 방어
- 표준 ErrorResponse, requestId, fieldErrors 반환
- 재조회 성공 후 SALE 원장과 후속 처리 생성
- 취소 결과불명 시 CANCEL 원장 미생성
- 같은 날짜·MID 정산 동시 실행 방어
- DRAFT 정산 재실행 시 신규 매출 누적 및 동일 명세 유지
- CONFIRMED·PAID 상태 전이와 재계산 차단
- 표준 `ErrorResponse`, `requestId`, `fieldErrors` 반환
- RecoveryTask 동시 재처리 시 하나의 요청만 claim하는지 검증
- PROCESSING/SUCCESS 재처리 차단과 FAILED 재처리 허용 검증
- 외부전송·알림톡 Mock Worker 성공·실패 처리 검증
- 동일 Queue 동시 처리 시 하나의 Worker만 claim하는지 검증
- retry limit에 도달한 Queue가 재처리되지 않는지 검증

### 보강 예정

- 100만 건 기준 매출 원장·정산 조회 성능 테스트

## 9. 로컬 실행 방법

Windows:

```powershell
.\gradlew.bat clean build
.\gradlew.bat :api:bootRun
```

Mac/Linux:

```bash
./gradlew clean build
./gradlew :api:bootRun
```

실행 후 `http://localhost:8080/`에 접속합니다.

## 10. Demo 안내

- Demo: https://yeni-demo.fly.dev/
- PDF: [권예은 백오피스 포트폴리오 PDF](https://github.com/user-attachments/files/28837173/_._.pptx.pdf)
- Repository: https://github.com/Yeni924/yeni-backoffice
- 제출 가이드: [docs/submission-guide.md](docs/submission-guide.md)

라이브 데모는 무료 호스팅 환경과 H2 in-memory DB를 사용합니다. 초기 접속 또는 배포 직후 응답이 지연될 수 있으며, Machine 재시작 시 시연 데이터가 초기화됩니다.

주요 기능 흐름은 README, 제출용 PDF, GitHub 코드와 로컬 실행을 기준으로 함께 확인할 수 있습니다. 로컬에서는 PG 승인·취소, 매출 원장, RecoveryTask, 정산 시나리오를 직접 테스트할 수 있습니다.

## 11. 운영 환경 확장 계획

현재 프로젝트는 포트폴리오 시연을 위해 Mock PG와 데모 환경을 사용합니다. 실제 운영 환경으로 확장한다면 다음 항목을 추가로 보강할 수 있습니다.

- H2 대신 PostgreSQL/MySQL 기반 운영 DB와 migration 적용
- 공개 데모에서는 인증을 생략했으며, 실제 운영 환경에서는 역할 기반 권한, CSRF, CORS, IP allowlist, 감사 로그 정책을 추가로 보강
- 실제 PG callback signature 검증과 외부망 연동
- 외부전송·알림톡 Worker 자동 스케줄링과 dead-letter 처리
- RecoveryTask 자동 재처리 Worker
- 정산 후 취소의 다음정산차감 자동 반영
- 영업일·공휴일 기준 D+N 정산
- 셀러별·입점몰별 정산
- 쿠폰·포인트·프로모션 금액 분리
- 대량 매출 원장 기준 조회·배치 성능 테스트

## 12. 포트폴리오 검토 포인트

1. .NET 백오피스 실무 경험을 Java/Spring Boot 구조로 재구성한 방식
2. 결제 승인·취소 이후 매출 원장과 정산으로 이어지는 데이터 흐름
3. 결과불명과 망취소 필요 상황을 실패와 분리한 상태 모델링
4. Idempotency Key, unique constraint, pessimistic lock을 활용한 중복·동시성 방어
5. 결제 트랜잭션과 외부전송·알림톡 Queue를 분리한 운영 구조
6. Controller, Service, Repository 계층 분리와 멀티모듈 구성

## 13. 제출용 PDF

채용 플랫폼에서 파일 제출만 가능한 경우를 대비해 PDF 포트폴리오를 함께 준비했습니다. PDF에는 프로젝트 개요, 핵심 운영 흐름, 주요 화면, 구현 포인트와 아키텍처 리뷰 요약을 정리했습니다.

> PDF 포트폴리오는 제출용 파일로 별도 관리하며, 최신 공개본은 Demo 안내의 링크에서 확인할 수 있습니다. 저장소 내부 `docs/portfolio.pdf` 형태로 함께 관리하는 방식은 추후 정리할 예정입니다.

## License

Portfolio Project
