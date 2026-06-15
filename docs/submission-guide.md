# Portfolio Submission Guide

## 이 프로젝트를 보는 순서

1. README 상단에서 프로젝트 목적과 핵심 구현 범위를 확인합니다.
2. 제출용 PDF에서 주요 화면과 전체 흐름을 빠르게 확인합니다.
3. PG 운영 콘솔에서 승인·취소·결과불명 시나리오를 실행합니다.
4. 매출 원장에서 SALE/CANCEL 거래와 연결 데이터를 확인합니다.
5. 정산 관리에서 DRAFT 생성·누적 재계산·확정·지급 흐름을 확인합니다.
6. 필요하면 Swagger와 DB 명세에서 API·데이터 구조를 확인합니다.

## .NET에서 Java로 전환한 맥락

기존 C#/.NET 백오피스 실무에서 다뤘던 관리자 화면, API, PG 연동, 외부 시스템 연동과 운영 데이터 흐름을 Java/Spring Boot 구조로 재구성했습니다.

Java 입문 문법을 나열하는 대신, 기존 실무 도메인을 Spring MVC, JPA, Service 계층, Repository 구조로 옮기며 다음 항목을 검증하는 데 초점을 맞췄습니다.

- 업무 흐름을 Java 객체와 상태값으로 모델링하는 방식
- 결제 승인·취소 오케스트레이션과 후속 처리 분리
- 결과불명과 복구 작업 모델링
- 매출 원장과 정산 기준 분리
- 중복 요청과 동시성 방어

## 핵심 구현 포인트

- Idempotency Key와 unique constraint 기반 중복 요청 방어
- 부분취소 시 비관적 락과 취소 가능 금액 검증
- 확정 거래만 SALE/CANCEL 원장으로 기록
- 결과불명 상태와 RecoveryTask 분리
- 외부전송·알림톡 Queue를 결제 처리와 분리
- DRAFT 정산 누적 재계산과 CONFIRMED·PAID 변경 차단
- ErrorCode, requestId, fieldErrors 기반 오류 응답

## Demo가 불안정할 때

라이브 데모는 무료 호스팅 환경과 H2 in-memory DB를 사용합니다. 초기 접속이 지연되거나 데이터가 초기화될 수 있습니다.

이 경우 다음 순서로 검토할 수 있습니다.

1. README의 핵심 도메인 흐름
2. 제출용 PDF의 화면 캡처와 설명
3. GitHub의 Service·Entity·Repository·테스트 코드
4. 로컬 실행

```powershell
.\gradlew.bat clean build
.\gradlew.bat :api:bootRun
```

## 면접 설명 요약

이 프로젝트는 실제 PG 운영망 연동을 완료한 서비스가 아니라, 기존 백오피스 실무에서 접한 결제 운영 흐름을 Spring Boot로 재구성한 포트폴리오입니다.

승인과 취소뿐 아니라 중복 요청, 부분취소, 결과불명, 복구 작업, SALE/CANCEL 매출 원장, 외부전송·알림톡 Queue, 정산 상태 전이를 하나의 데이터 흐름으로 연결했습니다. 공개 데모에서는 인증을 생략했으며, 실제 운영 적용 시 필요한 역할 기반 권한과 보안 정책, 실제 외부망, 영업일 정산, 자동 재처리 Worker와 대량 데이터 검증은 확장 계획으로 구분했습니다.

## 검토 시 구분할 범위

### 구현 완료

- Mock PG 승인·취소
- 중복 승인·취소 방어
- 부분취소 및 결과불명 처리
- RecoveryTask 운영 조회와 일부 유형의 재처리 기반
- SALE/CANCEL 매출 원장
- 외부전송·알림톡 Queue 생성 및 상태 관리
- 정산 초안·확정·지급 상태 전이
- 주요 자동화 테스트

### 운영 환경 확장 필요

- 실제 PG·알림톡·외부 시스템 연동
- 역할 기반 권한, CSRF, CORS, IP allowlist, 감사 로그 정책
- RecoveryTask 동시 claim 및 자동 재처리 Worker
- 정산 후 취소 자동 차감
- 영업일·셀러별 정산
- 자동 재처리 Worker
- 대량 데이터 성능 검증
