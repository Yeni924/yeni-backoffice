# Portfolio Submission Guide

이 문서는 포트폴리오 제출 시 면접관이 프로젝트를 어떤 순서로 보면 좋은지 정리한 안내입니다. 구현 의도와 운영 확장 포인트는 [Implementation Notes](implementation-notes.md)를 함께 참고합니다.

## 권장 확인 순서

1. README 상단에서 프로젝트 목적, 구현 범위, Mock 기반 한계를 먼저 확인합니다.
2. 대시보드에서 경력 프로젝트와 Spring Boot 포트폴리오의 연결 지점을 확인합니다.
3. 주문 관리(`/admin/commerce/orders`)에서 Mock 주문을 생성하거나 주문 상품을 직접 입력합니다.
4. 주문 목록에서 결제 승인을 실행해 기존 PG 승인 서비스로 연결되는 흐름을 확인합니다.
5. PG 운영(`/admin/payment-operations`)에서 승인, 취소, 결과불명, 망취소, 외부전송, 알림톡, RecoveryTask 흐름을 확인합니다.
6. 매출 원장(`/admin/payment-operations/sales-ledger`)에서 SALE 결제매출과 CANCEL 취소매출이 별도 거래로 기록되는지 확인합니다.
7. 정산 관리(`/admin/payment-operations/settlements`)에서 오늘 정산 배치를 실행하고 매출 원장 기준 정산 초안이 생성되는지 확인합니다.
8. 필요하면 DB 명세(`/admin/database-spec`)와 Swagger(`/swagger-ui/index.html`)에서 테이블 구조와 API 요청/응답을 확인합니다.

핵심 시연 흐름은 `주문 관리 -> PG 운영 -> 매출 원장 -> 후속처리/복구 -> 정산 관리`입니다.

## 면접 설명 요약

이 프로젝트는 실제 PG 운영망에 직접 연결한 서비스가 아니라, 기존 백오피스 실무에서 접한 결제 운영 흐름을 Spring Boot로 재구성한 포트폴리오입니다.

단순 승인/취소 CRUD가 아니라 중복 요청, 부분취소, 승인 결과불명, 취소 결과불명, 망취소 필요 상태, SALE/CANCEL 매출 원장, 외부전송, 알림톡 Queue, RecoveryTask, 정산 상태 전이를 하나의 데이터 흐름으로 연결했습니다.

주문 관리는 결제 흐름의 출발점으로 추가했습니다. 주문 상품 상세를 기준으로 서버가 결제 대상 금액을 계산하고, 주문 결제 승인 이후 기존 PG 운영, 매출 원장, 후속처리, 정산 화면으로 이어지도록 구성했습니다.

## .NET에서 Java로 전환한 맥락

기존 C#/.NET 백오피스 실무에서 다뤘던 관리자 화면, API, PG 연동, 외부 시스템 연동, 운영 데이터 흐름을 Java/Spring Boot 구조로 재구성했습니다.

검증 포인트는 Java 문법 자체보다 다음 항목에 맞췄습니다.

- 업무 흐름을 Entity, Service, Repository 구조로 모델링하는 방식
- 결제 승인/취소 오케스트레이션과 후속 처리 분리
- 결과불명과 복구 작업 모델링
- 매출 원장과 정산 기준 분리
- 중복 요청과 동시성 방어
- ErrorCode, requestId, fieldErrors 기반 표준 오류 응답

## 구현 완료

- 주문 상품 snapshot 기반 주문 생성
- Mock PG 승인/취소
- 중복 승인/취소 방어
- 부분취소 금액 검증
- APPROVE_UNKNOWN / CANCEL_UNKNOWN 결과불명 처리
- RecoveryTask 생성과 운영 조회/재처리 API
- SALE/CANCEL 매출 원장
- 외부전송 대기함과 알림톡 Queue
- 매출 원장 Tabulator 조회와 summary DB 집계
- 정산 초안, 확정, 지급 완료 상태 전이
- ErrorCode/requestId/fieldErrors 기반 예외 응답

## 운영 확장 필요

- 실제 PG, 알림톡, 외부망 연동
- 역할 기반 권한, CSRF, CORS, IP allowlist, 감사 로그 강화
- RecoveryTask 자동 재처리 Worker
- 정산 후 취소 다음정산차감 자동화
- 입점몰 sellerId/orderItemId 기반 셀러별 정산
- 영업일/공휴일 기준 D+N 정산
- 대량 데이터 성능 검증

## 로컬 확인

```powershell
.\gradlew.bat clean build
.\gradlew.bat :api:bootRun
```

공개 데모는 무료 호스팅과 H2 in-memory DB를 사용할 수 있어 초기 접속이 지연되거나 데이터가 초기화될 수 있습니다. 제출 시에는 README, PDF 캡처, GitHub 코드, 로컬 실행 순서로 함께 설명하는 편이 안전합니다.
